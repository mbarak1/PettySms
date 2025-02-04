package com.example.pettysms

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.google.android.material.color.MaterialColors


class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private var mPath: Path = Path()
    private var mPaint: Paint = Paint()

    init {
        setup()
    }

    private fun setup() {
        val colorControlNormal = MaterialColors.getColor(context, com.google.android.material.R.attr.colorControlNormal, "")

        mPaint.isAntiAlias = true
        mPaint.color = colorControlNormal // Black color for the signature
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = 10f // Set the stroke width
        mPaint.strokeJoin = Paint.Join.ROUND
        mPaint.strokeCap = Paint.Cap.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d("SignatureView", "onSizeChanged: w=$w, h=$h")

        if (mBitmap != null) {
            // Create a new bitmap only if the size has changed
            if (w != oldw || h != oldh) {
                // Create a new bitmap
                val newBitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888)
                val newCanvas = Canvas(newBitmap)

                // If the previous bitmap is not null, draw its contents onto the new bitmap
                mCanvas?.let { oldCanvas ->
                    newCanvas.drawBitmap(mBitmap!!, 0f, 0f, null)
                }
                // Recycle the old bitmap
                mBitmap?.recycle()
                mBitmap = newBitmap
                mCanvas = newCanvas
            }
        } else {
            // If the bitmap is null, create a new one
            mBitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888)
            mCanvas = Canvas(mBitmap!!)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Create a rounded rectangle for clipping
        val path = Path().apply {
            addRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 4f, 4f, Path.Direction.CW)
        }
        canvas.clipPath(path) // Clip the canvas to the rounded rectangle

        // Draw the bitmap and the path
        canvas.drawBitmap(mBitmap!!, 0f, 0f, null)
        canvas.drawPath(mPath, mPaint)
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mPath.moveTo(x, y)
                // Request parent to not intercept touch events
                parent.requestDisallowInterceptTouchEvent(true)
                return true // Consume the event
            }
            MotionEvent.ACTION_MOVE -> {
                mPath.lineTo(x, y)
            }
            MotionEvent.ACTION_UP -> {
                mCanvas?.drawPath(mPath, mPaint)
                mPath.reset()
                // Allow parent to intercept touch events again
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        invalidate() // Refresh the view
        return true // Consume the event
    }

    // Clear the signature
    fun clearSignature() {
        mBitmap?.eraseColor(0x00000000) // Clear the bitmap
        invalidate() // Refresh the view
    }

    // Optional: Method to get the signature bitmap
    fun getSignatureBitmap(): Bitmap? {
        return mBitmap
    }

    fun isSignatureEmpty(): Boolean {
        mBitmap?.let { bitmap ->
            for (x in 0 until bitmap.width) {
                for (y in 0 until bitmap.height) {
                    // Check if the pixel is not transparent
                    if (bitmap.getPixel(x, y) != 0) {
                        return false // Signature has content
                    }
                }
            }
        }
        return true // Signature is empty
    }

    fun setSignatureBitmap(bitmap: Bitmap) {
        mBitmap = bitmap
        mCanvas = Canvas(mBitmap!!)
        invalidate() // Redraw the view with the restored bitmap
    }


}