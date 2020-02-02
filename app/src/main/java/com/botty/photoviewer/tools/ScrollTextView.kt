package com.botty.photoviewer.tools

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.TextView

class ScrollTextView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0)
    : TextView(context!!, attrs, defStyle) {

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        if (focused)
            super.onFocusChanged(focused, direction, previouslyFocusedRect)
    }

    override fun onWindowFocusChanged(focused: Boolean) {
        if (focused)
            super.onWindowFocusChanged(focused)
    }

    override fun isFocused() = true
}