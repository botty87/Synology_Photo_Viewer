package com.botty.photoviewer.settings

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.botty.photoviewer.R
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        checkBoxDailySync.isChecked = Settings.autoUpdateGallery
        checkBoxShowSubfolderPictures.isChecked = Settings.showSubFoldersPic

        checkBoxDailySync.setOnCheckedChangeListener { _, checked ->
            Settings.autoUpdateGallery = checked
        }
        checkBoxShowSubfolderPictures.setOnCheckedChangeListener { _, checked ->
            Settings.showSubFoldersPic = checked
        }
    }
}
