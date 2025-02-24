package com.example.pettysms.utils

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan

object TextHighlighter {
    fun highlightText(originalText: String?, searchQuery: String): SpannableString {
        if (originalText == null) return SpannableString("")
        if (searchQuery.isEmpty()) return SpannableString(originalText)

        val spannable = SpannableString(originalText)
        val startPos = originalText.lowercase().indexOf(searchQuery.lowercase())
        
        if (startPos >= 0) {
            spannable.setSpan(
                BackgroundColorSpan(Color.YELLOW),
                startPos,
                startPos + searchQuery.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        return spannable
    }
} 