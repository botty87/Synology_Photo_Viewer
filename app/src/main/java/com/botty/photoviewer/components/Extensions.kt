package com.botty.photoviewer.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.AsyncListDiffer
import com.github.florent37.inlineactivityresult.Result
import com.github.florent37.inlineactivityresult.kotlin.KotlinActivityResult
import com.github.florent37.inlineactivityresult.kotlin.startForResult
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import es.dmoral.toasty.Toasty
import timber.log.Timber
import java.io.File
import java.io.Serializable

fun Context.showErrorToast(stringRes: Int, length: Int = Toasty.LENGTH_SHORT) {
    Toasty.error(this, stringRes, length).show()
}

fun Context.showErrorToast(message: String, length: Int = Toasty.LENGTH_SHORT) {
    Toasty.error(this, message, length).show()
}

fun Context.showSuccesToast(stringRes: Int, length: Int = Toasty.LENGTH_SHORT) {
    Toasty.success(this, stringRes, length).show()
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide(gone: Boolean = false) {
    if(gone)
        this.visibility = View.GONE
    else
        this.visibility = View.INVISIBLE
}

fun <T> MutableList<T>.removeLast() {
    if(size >= 1) removeAt(this.size - 1)
}

inline fun <reified T> FragmentActivity.startActivity(vararg params: Pair<String, Any?>) {
    startActivity(Intent(this, T::class.java).apply { fillIntentArguments(params) })
}

inline fun <reified T> FragmentActivity.startActivityForResult(
    vararg params: Pair<String, Any?>,
    crossinline onResult: ((Result) -> Unit)) : KotlinActivityResult {

    val intent = Intent(this, T::class.java).apply { fillIntentArguments(params) }
    return startForResult(intent) { result ->
        onResult.invoke(result)
    }
}

fun Intent.fillIntentArguments(params: Array<out Pair<String, Any?>>) {
    params.forEach {
        when (val value = it.second) {
            null -> putExtra(it.first, null as Serializable?)
            is Int -> putExtra(it.first, value)
            is Long -> putExtra(it.first, value)
            is CharSequence -> putExtra(it.first, value)
            is String -> putExtra(it.first, value)
            is Float -> putExtra(it.first, value)
            is Double -> putExtra(it.first, value)
            is Char -> putExtra(it.first, value)
            is Short -> putExtra(it.first, value)
            is Boolean -> putExtra(it.first, value)
            is Serializable -> putExtra(it.first, value)
            is Bundle -> putExtra(it.first, value)
            is Parcelable -> putExtra(it.first, value)
            is Array<*> -> when {
                value.isArrayOf<CharSequence>() -> putExtra(it.first, value)
                value.isArrayOf<String>() -> putExtra(it.first, value)
                value.isArrayOf<Parcelable>() -> putExtra(it.first, value)
                else -> throw Exception("Intent extra ${it.first} has wrong type ${value.javaClass.name}")
            }
            is IntArray -> putExtra(it.first, value)
            is LongArray -> putExtra(it.first, value)
            is FloatArray -> putExtra(it.first, value)
            is DoubleArray -> putExtra(it.first, value)
            is CharArray -> putExtra(it.first, value)
            is ShortArray -> putExtra(it.first, value)
            is BooleanArray -> putExtra(it.first, value)
            else -> throw Exception("Intent extra ${it.first} has wrong type ${value.javaClass.name}")
        }
    }
}

fun String.concat(strings: List<String>, separator: String = ""): String {
    var result = this
    strings.forEach { result += "$it$separator" }
    return result
}

val <T> AsyncListDiffer<T>.size: Int
        get() { return currentList.size }

operator fun <T> AsyncListDiffer<T>.get(pos: Int): T {
    return this.currentList[pos]
}

fun Throwable.log() {
    Timber.e(this)
}

fun Any?.isNotNull() = this != null

fun Any?.isNull() = this == null

fun <T> SparseArray<T>.isNotEmpty() = !this.isEmpty()

fun <T> SparseArray<T>.forEach(action: ((item: T?) -> Unit)) {
    for(i in 0 until this.size()) {
        action.invoke(this.valueAt(i))
    }
}

fun EditText.clear() {
    this.setText("")
}
fun TextView.clear() {
    this.text = ""
}

fun <T> SparseArray<T>.isEmpty(): Boolean = this.size() == 0

fun File.notExists() = !exists()

fun String.endsWithNoCase(suffix: String) = this.endsWith(suffix, true)

fun Handler?.removeAll() {
    this?.removeCallbacksAndMessages(null)
}

fun AdView.loadAdWithFailListener() {
    AdRequest.Builder().build().let { req ->
        this.loadAd(req)
        this.adListener = object: AdListener() {
            override fun onAdFailedToLoad(errorCode : Int) {
                LoadAdException(errorCode).log()
            }
        }
    }
}