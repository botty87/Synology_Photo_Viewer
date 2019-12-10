package com.botty.photoviewer.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.flaviofaria.kenburnsview.KenBurnsView
import com.github.florent37.kotlin.pleaseanimate.please

abstract class AdapterWithFocus<VH : RecyclerView.ViewHolder>(private val animatedHeather: Boolean = false) : RecyclerView.Adapter<VH>() {

    private val onFocusChangeListener = OnFocusChangeListenerZoom()

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.itemView.onFocusChangeListener = onFocusChangeListener
        verifyAnimation(holder.itemView, false)
    }

    private fun verifyAnimation(view: View, start: Boolean) {
        if(!animatedHeather) {
            return
        }

        val firstChild = (view as ViewGroup).getChildAt(0)
        if(firstChild is KenBurnsView) {
            if(start) {
                firstChild.resume()
            } else {
                firstChild.pause()
            }

        } else {
            val kenBurnsView = (firstChild as ViewGroup).getChildAt(0) as KenBurnsView
            if (start) {
                kenBurnsView.resume()
            } else {
                kenBurnsView.pause()
            }
        }
    }

    override fun onViewRecycled(holder: VH) {
        holder.itemView.onFocusChangeListener = null
        super.onViewRecycled(holder)
    }

    private inner class OnFocusChangeListenerZoom : View.OnFocusChangeListener {
        override fun onFocusChange(view: View, focused: Boolean) {
            if (focused) {
                please(200L) {
                    animate(view) {
                        scale(1.15F, 1.15F)
                    }
                }.start()
                verifyAnimation(view, true)
            } else {
                please(200L) {
                    animate(view) {
                        originalScale()
                    }
                }.start()
                verifyAnimation(view, false)
            }
        }
    }
}