package com.example.pettysms

import android.graphics.Canvas
import android.graphics.Color
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class StickyHeaderItemDecoration(private val adapter: TransactorsAdapter) : RecyclerView.ItemDecoration() {

    private var headerView: View? = null
    private var stickyHeaderHeight: Int = 0

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        val topChild = parent.getChildAt(0) ?: return
        val topChildPosition = parent.getChildAdapterPosition(topChild)
        if (topChildPosition == RecyclerView.NO_POSITION) {
            return
        }

        val headerPosition = findHeaderPositionForItem(topChildPosition)
        if (headerPosition == -1) {
            return
        }

        val currentHeader = getHeaderViewForItem(headerPosition, parent)
        fixLayoutSize(parent, currentHeader)

        val contactPoint = currentHeader.bottom + stickyHeaderHeight
        val childInContact = getChildInContact(parent, contactPoint) ?: return
        if (adapter.getItemViewType(parent.getChildAdapterPosition(childInContact)) == TransactorsAdapter.VIEW_TYPE_HEADER) {
            moveHeader(c, currentHeader, childInContact)
            return
        }

        drawHeader(c, currentHeader)
    }

    private fun getHeaderViewForItem(headerPosition: Int, parent: RecyclerView): View {
        if (headerView == null) {
            headerView = LayoutInflater.from(parent.context).inflate(R.layout.item_section_header, parent, false)
            fixLayoutSize(parent, headerView!!)
        }
        val header = adapter.transactors[headerPosition]
        if (header is Char) {
            val headerTextView = headerView!!.findViewById<TextView>(R.id.sectionTitle)
            headerTextView.text = header.toString()
        }
        return headerView!!
    }

    private fun drawHeader(c: Canvas, header: View) {
        c.save()
        c.translate(0f, 0f)
        header.draw(c)
        c.restore()
    }

    private fun moveHeader(c: Canvas, currentHeader: View, nextHeader: View) {
        c.save()
        c.translate(0f, (nextHeader.top - currentHeader.height).toFloat())
        currentHeader.draw(c)
        c.restore()
    }

    private fun getChildInContact(parent: RecyclerView, contactPoint: Int): View? {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child.top <= contactPoint && child.bottom >= contactPoint) {
                return child
            }
        }
        return null
    }

    private fun findHeaderPositionForItem(itemPosition: Int): Int {
        for (i in itemPosition downTo 0) {
            if (adapter.getItemViewType(i) == TransactorsAdapter.VIEW_TYPE_HEADER) {
                return i
            }
        }
        return -1
    }

    private fun fixLayoutSize(parent: ViewGroup, view: View) {
        // Specs for height and width are passed to child views.
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

        val childWidth = ViewGroup.getChildMeasureSpec(widthSpec, parent.paddingLeft + parent.paddingRight, view.layoutParams.width)
        val childHeight = ViewGroup.getChildMeasureSpec(heightSpec, parent.paddingTop + parent.paddingBottom, view.layoutParams.height)

        view.measure(childWidth, childHeight)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        stickyHeaderHeight = view.measuredHeight
    }
}