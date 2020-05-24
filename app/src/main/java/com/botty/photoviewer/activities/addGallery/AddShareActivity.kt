package com.botty.photoviewer.activities.addGallery

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.vvalidator.form
import com.afollestad.vvalidator.form.FormResult
import com.botty.photoviewer.R
import com.botty.photoviewer.adapters.addGallery.ConnectionParamsAdapter
import com.botty.photoviewer.adapters.addGallery.FoldersAdapter
import com.botty.photoviewer.components.*
import com.botty.photoviewer.components.network.responses.containers.Share
import com.botty.photoviewer.components.views.GridAutofitLayoutManager
import com.botty.photoviewer.data.connectionContainers.ConnectionParams
import kotlinx.android.synthetic.main.activity_add_share.*
import kotlinx.android.synthetic.main.add_gallery_dialog.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class AddShareActivity : FragmentActivity(), CoroutineScope by MainScope() {

    private val viewModel: AddShareViewModel by viewModel()

    private val connectionsAdapter by lazy {
        ConnectionParamsAdapter().apply {
            onConnectionClick = this@AddShareActivity::onConnectionClick
            recyclerViewConnections.layoutManager = LinearLayoutManager(this@AddShareActivity, LinearLayoutManager.VERTICAL, false)
            recyclerViewConnections.addItemDecoration(DividerItemDecoration(this@AddShareActivity, DividerItemDecoration.VERTICAL))
            recyclerViewConnections.setHasFixedSize(true)
            recyclerViewConnections.adapter = this
        }
    }

    private val foldersAdapter by lazy {
        FoldersAdapter().apply {
            onCallLogClick = this@AddShareActivity::onFolderClick
            onBackClick = this@AddShareActivity::onBackFolderClick

            val width = resources.getDimension(R.dimen.folder_size).toInt()
            recyclerViewFolders.layoutManager =
                GridAutofitLayoutManager(
                    this@AddShareActivity,
                    width
                )
            recyclerViewFolders.setHasFixedSize(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_share)

        fun onButtonLoginClick(result: FormResult) {
            if(result.success()) {
                result.run {
                    val address = values()[0].asString()
                    val username = values()[1].asString()
                    val password = values()[2].asString()
                    val https = checkboxHttps.isChecked
                    val port = editTextPort.text.toString().let { portString ->
                        if(portString.isBlank()) {
                            if(https) {
                                ConnectionParams.DEFAULT_HTTPS_PORT
                            } else {
                                ConnectionParams.DEFAULT_HTTP_PORT
                            }
                        } else {
                            portString.trim().toInt()
                        }
                    }

                    performLoginAndLoadRootShares(
                        ConnectionParams(
                            address,
                            username,
                            password,
                            port,
                            https
                        )
                    )
                }
            } else {
                showErrorToast(R.string.fill_all_fields)
            }
        }

        val loginForm = form {
            input(editTextAddress){ isNotEmpty() }
            input(editTextUsername) { isNotEmpty() }
            input(editTextPassword) { isNotEmpty() }
            submitWith(buttonLogin, ::onButtonLoginClick)
        }

        editTextPassword.setOnEditorActionListener { _, id, _ ->
            if(id == EditorInfo.IME_ACTION_DONE) {
                loginForm.validate().run {::onButtonLoginClick}
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        buttonSetActualFolder.setOnClickListener { onSetActualFolderClick() }

        viewModel.connections.observe(this) { connections ->
            connectionsAdapter.setConnections(connections)
        }
    }

    private fun performLoginAndLoadRootShares(tempConnectionParams: ConnectionParams){
        setLoginControlsEnabled(false)
        progressLoader.show()

        launch {
            runCatching {
                viewModel.login(tempConnectionParams)
                viewModel.getShares()
            }.onFailure { e ->
                onRemoteError(e, true)
            }.onSuccess { shares ->
                recyclerViewFolders.adapter = foldersAdapter
                buttonSetActualFolder.isEnabled = true
                setNewFolders(shares)
            }
        }
    }

    private fun onConnectionClick(connection: ConnectionParams) {
        connection.run {
            editTextAddress.setText(address)
            editTextUsername.setText(user)
            editTextPassword.setText(password)
            checkboxHttps.isChecked = https
            if((https && port != ConnectionParams.DEFAULT_HTTPS_PORT) || (!https && port != ConnectionParams.DEFAULT_HTTP_PORT)) {
                editTextPort.setText(port.toString())
            } else {
                editTextPort.clear()
            }

            performLoginAndLoadRootShares(this)
        }
    }

    private fun setLoginControlsEnabled(isEnabled: Boolean) {
        buttonLogin.isEnabled = isEnabled
        editTextAddress.isEnabled = isEnabled
        editTextUsername.isEnabled = isEnabled
        editTextPassword.isEnabled = isEnabled
        recyclerViewConnections.isEnabled = isEnabled
        editTextPort.isEnabled = isEnabled
        checkboxHttps.isEnabled = isEnabled
        if(isEnabled) {
            connectionsAdapter.onConnectionClick = this::onConnectionClick
            connectionsAdapter.isEnabled = true
            connectionsAdapter.notifyDataSetChanged()
        } else {
            connectionsAdapter.onConnectionClick = null
            connectionsAdapter.isEnabled = false
            connectionsAdapter.notifyDataSetChanged()
        }
    }

    private fun onRemoteError(exception: Throwable, isLogin: Boolean = false) {
        progressLoader.hide()
        if(isLogin) {
            setLoginControlsEnabled(true)
        } else {
            recyclerViewFolders.show()
            enableButtonSetActualFolder()
            recyclerViewFolders.requestFocus()
        }
        exception.localizedMessage?.let { showErrorToast(it) } ?: showErrorToast(R.string.error)
        exception.log()
    }

    private fun setNewFolders(folders: List<Share>) {
        val actualPath = viewModel.actualPath
        foldersAdapter.updateFolders(folders, actualPath.isNotEmpty())
        if(actualPath.isNotEmpty()) {
            var path = "/"
            actualPath.forEach { folderName ->
                path += "$folderName/"
            }
            textViewPath.text = path.dropLast(1)
        } else {
            textViewPath.text = "/"
        }
        progressLoader.hide()
        recyclerViewFolders.show()
        enableButtonSetActualFolder()
        recyclerViewFolders.requestFocus()
    }

    private fun enableButtonSetActualFolder() {
        buttonSetActualFolder.isFocusable = false
        buttonSetActualFolder.isEnabled = true
        //workaround to avoid getting focus
        launch {
            delay(500)
            buttonSetActualFolder.isFocusable = true
        }
    }

    private fun onFolderClick(folder: Share) {
        setLoadFolderViews()
        launch {
            runCatching {
                viewModel.loadFolder(folder)
            }.onSuccess { folders ->
                setNewFolders(folders)
            }.onFailure { e ->
                onRemoteError(e)
            }
        }
    }

    private fun setLoadFolderViews() {
        buttonSetActualFolder.isEnabled = false
        recyclerViewFolders.hide()
        progressLoader.show()
    }

    private fun onBackFolderClick() {
        setLoadFolderViews()
        launch {
            runCatching {
                viewModel.loadParentFolder()
            }.onSuccess { folders -> setNewFolders(folders) }
            .onFailure { e -> onRemoteError(e) }
        }
    }

    private fun onSetActualFolderClick() {
        MaterialDialog(this).show {
            customView(R.layout.add_gallery_dialog)
            getCustomView().run {
                buttonCancel.setOnClickListener { dismiss() }
                buttonAdd.setOnClickListener {
                    val name = editTextGalleryName.text.toString().trim()
                    if(name.isBlank()) {
                        showErrorToast(R.string.insert_name)
                    } else {
                        addGalleryAndClose(name)
                    }
                }
            }
        }
    }

    private fun addGalleryAndClose(galleryName: String) {
        viewModel.addGalleryToDB(galleryName)
        setResult(RESULT_OK)
        finish()
    }
}