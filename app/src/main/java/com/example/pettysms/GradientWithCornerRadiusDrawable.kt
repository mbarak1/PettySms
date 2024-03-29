package com.example.pettysms

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Parcel
import android.os.Parcelable

class GradientWithCornerRadiusDrawable(
    context: Context,
    private val startColor: Int,
    private val endColor: Int,
    private val cornerRadius: Float
) : Drawable() {

    private val gradientDrawable: GradientDrawable

    init {
        val colors = intArrayOf(startColor, endColor)
        gradientDrawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors)
        gradientDrawable.cornerRadius = cornerRadius
        // Set other properties of the gradient drawable as needed
    }

    override fun draw(canvas: Canvas) {
        gradientDrawable.setBounds(bounds)
        gradientDrawable.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        gradientDrawable.alpha = alpha
    }

    override fun getOpacity(): Int {
        return gradientDrawable.opacity
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        gradientDrawable.colorFilter = colorFilter
    }

}