/*
 * SPDX-FileCopyrightText: bitDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** bitDroid Bloom: the scalloped "cookie" shape and the Bloom page background for setup. */
object BloomShapes {
    /** A cookie with [lobes] soft scallops filling a circle of [radius] around (cx, cy). */
    fun cookiePath(cx: Float, cy: Float, radius: Float, lobes: Int, depth: Float = 0.07f, turn: Float = 0f): Path {
        val p = Path()
        val steps = lobes * 24
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * 2f * PI.toFloat()
            val r = radius * (1f + depth * cos(lobes * t)) / (1f + depth)
            val x = cx + r * cos(t + turn)
            val y = cy + r * sin(t + turn)
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    fun color(context: Context, id: Int, fallback: Int): Int =
        runCatching { context.getColor(id) }.getOrDefault(fallback)
}

/** A filled cookie (used behind each page's icon). */
class CookieDrawable(private val fill: Int, private val lobes: Int = 9) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val r = min(b.width(), b.height()) / 2f
        canvas.drawPath(BloomShapes.cookiePath(b.exactCenterX(), b.exactCenterY(), r, lobes), paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

/**
 * The Bloom page: deep ink tinted with the Material You palette, a soft lilac bloom of light up
 * top, and two big faint cookies drifting off the corners.
 */
class BloomBackground(context: Context) : Drawable() {
    private val ink = BloomShapes.color(context, android.R.color.system_accent1_900, 0xFF21005D.toInt())
    private val deep = BloomShapes.color(context, android.R.color.system_neutral1_900, 0xFF141218.toInt())
    private val bloom = BloomShapes.color(context, android.R.color.system_accent1_600, 0xFF7F67BE.toInt())
    private val petal = BloomShapes.color(context, android.R.color.system_accent1_200, 0xFFD0BCFF.toInt())
    private val petal2 = BloomShapes.color(context, android.R.color.system_accent3_200, 0xFFEFB8C8.toInt())
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        val b = bounds
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        paint.shader = LinearGradient(0f, 0f, 0f, h, ink, deep, Shader.TileMode.CLAMP)
        paint.alpha = 255
        canvas.drawRect(b, paint)
        paint.shader = RadialGradient(w * 0.25f, h * 0.08f, w * 0.95f,
            (bloom and 0x00FFFFFF) or 0x8C000000.toInt(), bloom and 0x00FFFFFF, Shader.TileMode.CLAMP)
        canvas.drawRect(b, paint)
        paint.shader = null
        paint.color = petal
        paint.alpha = 0x1E
        canvas.drawPath(BloomShapes.cookiePath(w * 0.98f, h * 0.16f, w * 0.42f, 12, 0.06f, 0.2f), paint)
        paint.color = petal2
        paint.alpha = 0x14
        canvas.drawPath(BloomShapes.cookiePath(w * 0.02f, h * 0.9f, w * 0.5f, 9, 0.07f), paint)
    }

    override fun onBoundsChange(bounds: Rect) = invalidateSelf()
    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.OPAQUE
}
