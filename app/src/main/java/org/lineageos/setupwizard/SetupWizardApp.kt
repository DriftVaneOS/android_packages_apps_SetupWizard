/*
 * SPDX-FileCopyrightText: 2013 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard

import android.app.Application
import android.app.StatusBarManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemProperties
import android.provider.Settings
import android.util.Log
import org.lineageos.setupwizard.util.SetupWizardUtils

val LOGV: Boolean = Log.isLoggable(SetupWizardApp.TAG, Log.VERBOSE)

const val ACTION_RESTORE_FROM_BACKUP = "com.stevesoltys.seedvault.RESTORE_BACKUP"
const val ACTION_EMERGENCY_DIAL = "com.android.phone.EmergencyDialer.DIAL"
const val ACTION_LOAD = "com.android.wizard.LOAD"

const val EXTRA_WIZARD_BUNDLE = "wizardBundle"
const val EXTRA_SCRIPT_URI = "scriptUri"
const val EXTRA_ACTION_ID = "actionId"
const val EXTRA_RESULT_CODE = "com.android.setupwizard.ResultCode"
const val EXTRA_PREFS_SHOW_BUTTON_BAR = "extra_prefs_show_button_bar"
const val EXTRA_PREFS_SHOW_SKIP = "extra_prefs_show_skip"
const val EXTRA_PREFS_SHOW_SKIP_TV = "extra_show_skip_network"
const val EXTRA_PREFS_SET_BACK_TEXT = "extra_prefs_set_back_text"
const val EXTRA_ENABLE_NEXT_ON_CONNECT = "wifi_enable_next_on_connect"

const val KEY_SEND_METRICS = "send_metrics"
const val DISABLE_NAV_KEYS = "disable_nav_keys"
const val ENABLE_RECOVERY_UPDATE = "enable_recovery_update"
const val UPDATE_RECOVERY_PROP = "persist.vendor.recovery_update"
const val IGNORE_SIM_LOCALE_PROP = "ro.setupwizard.ignore_sim_locale"

const val NAVIGATION_OPTION_KEY = "navigation_option"

const val RADIO_READY_TIMEOUT = 10 * 1000

class SetupWizardApp : Application() {

    var ignoreSimLocale = SystemProperties.getBoolean(IGNORE_SIM_LOCALE_PROP, false)

    private val handler = Handler(Looper.getMainLooper())
    private val radioTimeoutRunnable = Runnable { isRadioReady = true }

    override fun onCreate() {
        super.onCreate()
        // DriftVaneOS: setup is always dark (ink background, white text)
        runCatching {
            getSystemService(android.app.UiModeManager::class.java)
                ?.setApplicationNightMode(android.app.UiModeManager.MODE_NIGHT_YES)
        }
        // bitDroid: switch to bitKeys as soon as setup starts, so it's there for the Wi-Fi
        // password. Only once; the AOSP keyboard stays enabled as a backup.
        runCatching { pickBitKeys() }.onFailure { Log.w(TAG, "couldn't pick bitKeys", it) }
        if (LOGV) {
            Log.v(TAG, "onCreate()")
        }
        statusBarManager = SetupWizardUtils.disableStatusBar(this)
        handler.postDelayed(radioTimeoutRunnable, RADIO_READY_TIMEOUT.toLong())
        if (SetupWizardUtils.hasGMS(this)) {
            SetupWizardUtils.disableHome(this)
            if (SetupWizardUtils.isOwner()) {
                Settings.Global.putInt(contentResolver, Settings.Global.ASSISTED_GPS_ENABLED, 1)
            }
        }
    }

    private fun pickBitKeys() {
        val cr = contentResolver
        if (Settings.Secure.getInt(cr, "bitdroid_keyboard_picked", 0) == 1) return
        val imm = getSystemService(android.view.inputmethod.InputMethodManager::class.java) ?: return
        val bitKeys =
            imm.inputMethodList.firstOrNull {
                it.packageName == "com.bitdroid.keys" || it.packageName.contains("bitkeys", ignoreCase = true)
            }
                ?: return
        val id = bitKeys.id
        val enabled =
            (Settings.Secure.getString(cr, Settings.Secure.ENABLED_INPUT_METHODS) ?: "")
                .split(':')
                .filter { it.isNotBlank() }
        if (enabled.none { it.substringBefore(';') == id }) {
            Settings.Secure.putString(
                cr,
                Settings.Secure.ENABLED_INPUT_METHODS,
                (enabled + id).joinToString(":"),
            )
        }
        Settings.Secure.putString(cr, Settings.Secure.DEFAULT_INPUT_METHOD, id)
        Settings.Secure.putInt(cr, "bitdroid_keyboard_picked", 1)
        Log.i(TAG, "bitKeys is the keyboard: $id")
    }

    companion object {
        const val TAG = "SetupWizardApp"

        @Volatile var isRadioReady: Boolean = false

        var statusBarManager: StatusBarManager? = null
            private set

        val settingsBundle = Bundle()
    }
}
