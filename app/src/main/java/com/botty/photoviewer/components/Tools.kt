package com.botty.photoviewer.components

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
object Tools {
    val standardDateParser : SimpleDateFormat
        get() = SimpleDateFormat("dd MMM yyyy - HH:mm:ss").apply {
                timeZone = TimeZone.getTimeZone("UTC")
        }
}