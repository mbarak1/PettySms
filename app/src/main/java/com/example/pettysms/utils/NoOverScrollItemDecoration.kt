package com.example.pettysms.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A RecyclerView ItemDecoration that prevents scrolling past the last item.
 * This adds padding to the bottom of the last item to fill the remaining space
 * in the RecyclerView, eliminating the ability to scroll past the last item.
 */
class NoOverScrollItemDecoration : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        
        val layoutManager = parent.layoutManager as? LinearLayoutManager ?: return
        val adapter = parent.adapter ?: return
        
        // Only apply to the last item
        val position = parent.getChildAdapterPosition(view)
        if (position == adapter.itemCount - 1) {
            // Calculate the remaining space
            val lastItemBottom = layoutManager.getDecoratedBottom(view)
            val recyclerViewHeight = parent.height - parent.paddingBottom
            
            // If there's space remaining, add it as bottom padding to the last item
            if (lastItemBottom < recyclerViewHeight) {
                outRect.bottom = recyclerViewHeight - lastItemBottom
            }
        }
    }
} 