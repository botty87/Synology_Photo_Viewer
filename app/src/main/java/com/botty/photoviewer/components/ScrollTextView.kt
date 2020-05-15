package com.botty.photoviewer.components

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet

class ScrollTextView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0)
    : androidx.appcompat.widget.AppCompatTextView(context!!, attrs, defStyle) {

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