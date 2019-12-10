package com.botty.photoviewer.addGallery

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
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
import com.botty.photoviewer.data.ConnectionParams
import com.botty.photoviewer.data.ConnectionParams_
import com.botty.photoviewer.data.Gallery
import com.botty.photoviewer.data.ObjectBox
import com.botty.photoviewer.tools.*
import com.botty.photoviewer.tools.network.Network
import com.botty.photoviewer.tools.network.responses.containers.Share
import io.objectbox.kotlin.query
import kotlinx.android.synthetic.main.activity_add_share.*
import kotlinx.android.synthetic.main.add_gallery_dialog.view.*
import kotlinx.coroutines.*

class AddShareActivity : FragmentActivity(), CoroutineScope by MainScope() {

    private val viewModel by lazy { ViewModelProvider(this).get(AddShareViewModel::class.java) }
    private val actualPath by lazy { mutableListOf<String>() }

    private lateinit var connectionParams: ConnectionParams
    private lateinit var sid: String

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
            recyclerViewFolders.layoutManager = GridAutofitLayoutManager(this@AddShareActivity, width)
            recyclerViewFolders.setHasFixedSize(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_share)

        fun login(result: FormResult) {
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

                    ObjectBox.connectionParamsBox.query {
                        equal(ConnectionParams_.address, address)
                        equal(ConnectionParams_.user, username)
                    }.findFirst()?.run {
                        showErrorToast(getString(R.string.connection_exist))
                        return
                    }

                    performLogin(ConnectionParams(address, username, password, port, https))
                }
            } else {
                showErrorToast(R.string.fill_all_fields)
            }
        }

        val loginForm = form {
            input(editTextAddress){ isNotEmpty() }
            input(editTextUsername) { isNotEmpty() }
            input(editTextPassword) { isNotEmpty() }
            submitWith(buttonLogin, ::login)
        }

        editTextPassword.setOnEditorActionListener { _, id, _ ->
            if(id == EditorInfo.IME_ACTION_DONE) {
                loginForm.validate().run {::login}
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        buttonSetActualFolder.setOnClickListener { onSetActualFolderClick() }

        viewModel.connectionsLiveData.observe(this) { connections ->
            connectionsAdapter.setConnections(connections)
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

            performLogin(this)
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

    private fun performLogin(tempConnectionParams: ConnectionParams){
        setLoginControlsEnabled(false)
        progressLoader.show()

        suspend fun login() {
            runCatching {
                Network.login(tempConnectionParams)
            }.onFailure { e ->
                onRemoteError(e, true)
            }.onSuccess { response ->
                sid = response.sid
                connectionParams = tempConnectionParams
                if(connectionParams.id != 0L) {
                    ObjectBox.connectionParamsBox.put(connectionParams)
                }
                loadRootShares(true)
            }
        }

        launch {
            login()
        }
    }

    private suspend fun onRemoteError(exception: Throwable, isLogin: Boolean = false) = withContext(Dispatchers.Main) {
        progressLoader.hide()
        if(isLogin) {
            setLoginControlsEnabled(true)
        } else {
            recyclerViewFolders.show()
            enableButtonSetActualFolder()
            recyclerViewFolders.requestFocus()
        }
        exception.message?.let { showErrorToast(it) } ?: showErrorToast(R.string.error)
        exception.log()
    }

    private suspend fun loadRootShares(isLogin: Boolean) {
        runCatching {
            Network.getShares(connectionParams.toSessionParams(sid))
        }.onFailure { e ->
            onRemoteError(e, isLogin)
        }.onSuccess { response ->
            if(isLogin) {
                recyclerViewFolders.adapter = foldersAdapter
                buttonSetActualFolder.isEnabled = true
            }
            actualPath.clear()
            setNewFolders(response.shares)
        }
    }

    private fun setNewFolders(folders: List<Share>) {
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
        loadFolder(folder.path, folder.name)
    }

    private fun loadFolder(path: String, nameToAdd: String?) {
        suspend fun onSuccess(folders: List<Share>) = withContext(Dispatchers.Main) {
            val visibleFolders = async(Dispatchers.Default) {
                folders.filter { share ->
                    share.isNotHidden()
                }
            }
            setNewFolders(visibleFolders.await())
        }

        buttonSetActualFolder.isEnabled = false
        recyclerViewFolders.hide()
        progressLoader.show()
        launch {
            runCatching {
                Network.getFolders(connectionParams.toSessionParams(sid), path)
            }.onFailure { e ->
                onRemoteError(e)
            }.onSuccess { response ->
                nameToAdd?.run { actualPath.add(this) } ?: actualPath.removeLast()
                onSuccess(response.files)
            }
        }
    }

    private fun onBackFolderClick() {
        if(actualPath.size <= 1) {
            buttonSetActualFolder.isEnabled = false
            recyclerViewFolders.hide()
            progressLoader.show()
            launch { loadRootShares(false) }
        } else {
            var path = "/"
            for(i in 0 until actualPath.size - 1) {
                path += "${actualPath[i]}/"
            }
            path = path.dropLast(1)
            loadFolder(path, null)
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
        val path = "/".concat(actualPath)
        val gallery = Gallery(galleryName, path)
        if(connectionParams.id != 0L) {
            gallery.connectionParams.targetId = connectionParams.id
        } else {
            gallery.connectionParams.target = connectionParams
        }
        ObjectBox.galleryBox.put(gallery)
        finish()
    }
}