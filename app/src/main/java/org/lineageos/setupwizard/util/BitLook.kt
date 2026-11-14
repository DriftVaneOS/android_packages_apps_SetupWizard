/*
 * SPDX-FileCopyrightText: bitDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.util

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowInsetsController
import android.widget.Button
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupdesign.GlifLayout
import org.lineageos.setupwizard.R

/**
 * bitDroid look for every setup screen, applied in code so the system setup theme
 * (setupwizard.theme) can't put the stock look back: the moving-glow gradient behind every
 * page, big white headings, soft gray descriptions, and pill buttons (Next is solid white,
 * Skip is outlined).
 */
object BitLook {
    private const val TAG = "BitLook"
    private const val INK = 0xFF0B0A10.toInt()
    private const val MUTED = 0xFFB4ADC2.toInt()

    fun apply(activity: Activity) {
        runCatching { applyWindow(activity) }.onFailure { Log.w(TAG, "window", it) }
        val root = activity.findViewById<View>(R.id.setup_wizard_layout) as? GlifLayout ?: return
        root.background = null
        // looked up by name so a setup library update can't break the build
        call(root, "setBackgroundBaseColor", ColorStateList::class.java, ColorStateList.valueOf(Color.TRANSPARENT))
        call(root, "setBackgroundPatterned", Boolean::class.javaPrimitiveType!!, false)
        val ctx = activity
        val lilac = BloomShapes.color(ctx, android.R.color.system_accent1_100, 0xFFEADDFF.toInt())
        (call(root, "getHeaderTextView") as? android.widget.TextView)?.apply {
            setTextColor(lilac)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        (call(root, "getDescriptionTextView") as? android.widget.TextView)?.setTextColor(
            BloomShapes.color(ctx, android.R.color.system_neutral2_200, MUTED))
        // Bloom: the page icon sits on a scalloped cookie
        runCatching {
            val id = ctx.resources.getIdentifier("sud_layout_icon", "id", ctx.packageName)
            (if (id != 0) root.findViewById<View>(id) else null) as? android.widget.ImageView
        }.getOrNull()?.takeIf { it.drawable != null }?.let { icon ->
            val d = icon.resources.displayMetrics.density
            val pad = (14 * d).toInt()
            icon.background = CookieDrawable(BloomShapes.color(ctx, android.R.color.system_accent1_200, 0xFFD0BCFF.toInt()))
            icon.imageTintList = ColorStateList.valueOf(BloomShapes.color(ctx, android.R.color.system_accent1_900, 0xFF21005D.toInt()))
            icon.setPadding(pad, pad, pad, pad)
            icon.layoutParams = icon.layoutParams?.apply { width = (72 * d).toInt(); height = (72 * d).toInt() }
            icon.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }
        val footer = runCatching { root.getMixin(FooterBarMixin::class.java) }.getOrNull() ?: return
        (call(footer, "getButtonContainer") as? View)?.setBackgroundColor(Color.TRANSPARENT)
        (call(footer, "getPrimaryButtonView") as? Button)?.let { pill(it, filled = true) }
        (call(footer, "getSecondaryButtonView") as? Button)?.let { pill(it, filled = false) }
    }

    private fun call(target: Any, name: String, vararg typesAndArgs: Any): Any? = runCatching {
        val types = typesAndArgs.filterIndexed { i, _ -> i % 2 == 0 }.map { it as Class<*> }.toTypedArray()
        val args = typesAndArgs.filterIndexed { i, _ -> i % 2 == 1 }.toTypedArray()
        var c: Class<*>? = target.javaClass
        while (c != null) {
            runCatching { c!!.getDeclaredMethod(name, *types) }.getOrNull()?.let { m ->
                m.isAccessible = true
                return@runCatching m.invoke(target, *args)
            }
            c = c.superclass
        }
        null
    }.onFailure { Log.w(TAG, "$name failed", it) }.getOrNull()

    private fun applyWindow(activity: Activity) {
        val w = activity.window
        // Bloom: deep Material You ink with a lilac bloom and two faint cookies
        w.setBackgroundDrawable(BloomBackground(activity))
        @Suppress("DEPRECATION")
        run {
            w.statusBarColor = Color.TRANSPARENT
            w.navigationBarColor = Color.TRANSPARENT
        }
        // white status / nav bar icons on the dark glow
        w.insetsController?.setSystemBarsAppearance(
            0,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
        )
    }

    private fun pill(b: Button, filled: Boolean) {
        val c = b.context
        val d = b.resources.displayMetrics.density
        // Bloom: Next is a lilac pill, Skip is a soft tonal pill; both squish when pressed
        val bg = if (filled) BloomShapes.color(c, android.R.color.system_accent1_200, 0xFFD0BCFF.toInt())
            else BloomShapes.color(c, android.R.color.system_accent1_800, 0xFF381E72.toInt())
        val fg = if (filled) BloomShapes.color(c, android.R.color.system_accent1_900, INK)
            else BloomShapes.color(c, android.R.color.system_accent1_100, 0xFFEADDFF.toInt())
        b.backgroundTintList = null
        b.background = GradientDrawable().apply {
            cornerRadius = 30 * d
            setColor(bg)
        }
        b.setTextColor(fg)
        b.minHeight = (52 * d).toInt()
        b.setPadding((26 * d).toInt(), b.paddingTop, (26 * d).toInt(), b.paddingBottom)
        b.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        b.isAllCaps = false
        squish(b)
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun squish(v: View) {
        v.setOnTouchListener { view, e ->
            when (e.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN ->
                    view.animate().scaleX(0.93f).scaleY(0.86f).setDuration(110)
                        .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).setDuration(420)
                        .setInterpolator(android.view.animation.OvershootInterpolator(3f)).start()
            }
            false
        }
    }
}
