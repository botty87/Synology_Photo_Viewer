package com.botty.photoviewer.activities.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.settings.GalleriesAdapter
import com.botty.tvrecyclerview.TvRecyclerView
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.remove_gallery_dialog.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsActivity : FragmentActivity() {

    private val viewModel: SettingsActivityViewModel by viewModel()

    private val galleriesAdapter by lazy {
        recyclerViewGalleries.setHasFixedSize(true)
        recyclerViewGalleries.run {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(this@SettingsActivity, DividerItemDecoration.VERTICAL))
            setSelectPadding(5, 0, 5, 0)
        }

        GalleriesAdapter(viewModel.galleries, this).apply {
            recyclerViewGalleries.setOnItemStateListener(object : TvRecyclerView.OnItemStateListener {
                override fun onItemViewClick(view: View?, position: Int) {
                    //Ask before to remove
                    MaterialDialog(this@SettingsActivity).show {
                        customView(R.layout.remove_gallery_dialog)
                        getCustomView().run {
                            buttonNo.setOnClickListener { dismiss() }
                            buttonYes.setOnClickListener {
                                viewModel.removeGallery(position)
                                dismiss()
                            }
                        }
                    }
                }

                override fun onItemViewFocusChanged(gainFocus: Boolean, view: View?, position: Int) {}
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        checkboxShowPictureInfo.isChecked = viewModel.showPicInfoFullScreen
        checkboxShowPictureInfo.setOnCheckedChangeListener { _, checked ->
            viewModel.showPicInfoFullScreen = checked
        }

        checkBoxDBMode.isChecked = viewModel.dbMode
        checkBoxDBMode.setOnCheckedChangeListener { _, checked ->
            viewModel.dbMode = checked
        }

        editTextPresentationTimeout.setText(viewModel.presentationTimeout.toString())

        recyclerViewGalleries.adapter = galleriesAdapter
    }

    override fun onStop() {
        runCatching {
            editTextPresentationTimeout.text.toString().toInt()
        }.onSuccess { seconds ->
            if(seconds > 0) viewModel.presentationTimeout = seconds
        }
        super.onStop()
    }

    override fun onBackPressed() {
        setResult(RESULT_OK)
        super.onBackPressed()
    }
}
