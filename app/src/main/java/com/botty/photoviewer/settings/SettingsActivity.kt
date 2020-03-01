package com.botty.photoviewer.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.settings.GalleriesAdapter
import com.botty.tvrecyclerview.TvRecyclerView
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : FragmentActivity() {

    private val galleriesAdapter by lazy {
        recyclerViewGalleries.setHasFixedSize(true)
        recyclerViewGalleries.run {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(this@SettingsActivity, DividerItemDecoration.VERTICAL))
            setSelectPadding(5, 0, 5, 0)
        }
        GalleriesAdapter().apply {
            recyclerViewGalleries.setOnItemStateListener(object : TvRecyclerView.OnItemStateListener {
                override fun onItemViewClick(view: View?, position: Int) {
                    removeGallery(position)
                }

                override fun onItemViewFocusChanged(gainFocus: Boolean, view: View?, position: Int) {}
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        checkboxShowPictureInfo.isChecked = Settings.showPicInfoFullScreen
        checkboxShowPictureInfo.setOnCheckedChangeListener { _, checked ->
            Settings.showPicInfoFullScreen = checked
        }

        editTextPresentationTimeout.setText(Settings.presentationTimeout.toString())

        recyclerViewGalleries.adapter = galleriesAdapter
    }

    override fun onStop() {
        runCatching {
            editTextPresentationTimeout.text.toString().toInt()
        }.onSuccess { seconds ->
            if(seconds > 0) Settings.presentationTimeout = seconds
        }
        super.onStop()
    }
}
