package com.example.pettysms.utils

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A custom RecyclerView that prevents scrolling past the last item.
 * This eliminates the extra space that appears after the last item in a standard RecyclerView.
 */
class NoOverScrollRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    override fun canScrollVertically(direction: Int): Boolean {
        // If scrolling down (direction > 0) and can't scroll further, return false
        if (direction > 0) {
            val layoutManager = layoutManager as? LinearLayoutManager ?: return super.canScrollVertically(direction)
            
            // Check if the last item is completely visible
            val lastItemPosition = layoutManager.itemCount - 1
            if (lastItemPosition >= 0) {
                val lastVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition()
                
                // If the last item is completely visible, prevent further scrolling down
                if (lastVisibleItemPosition == lastItemPosition) {
                    return false
                }
            }
        }
        
        // For all other cases, use the default behavior
        return super.canScrollVertically(direction)
    }
} 