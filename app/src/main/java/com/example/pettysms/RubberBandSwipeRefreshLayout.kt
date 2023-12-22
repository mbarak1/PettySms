package com.example.pettysms

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class RubberBandSwipeRefreshLayout(context: Context, attrs: AttributeSet) : SwipeRefreshLayout(context, attrs) {

    private val OVERSCROLL_SCALE = 0.5f // Adjust this value as needed
    private var initialTouchY = 0f

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        // Prevent SwipeRefreshLayout from intercepting touch events to allow overscroll
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                // Get the y-delta of the overscroll
                val overscroll = event.y - initialTouchY

                // Apply a rubber band effect by scaling the overscroll
                if (overscroll > 0) {
                    val scaledOverscroll = overscroll * OVERSCROLL_SCALE
                    event.setLocation(event.x, initialTouchY + scaledOverscroll)
                }
            }
            MotionEvent.ACTION_DOWN -> {
                // Store the initial touch position
                initialTouchY = event.y
            }
        }

        return super.onTouchEvent(event)
    }
}