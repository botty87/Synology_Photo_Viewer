package com.botty.photoviewer.tools.network.responses.containers

import com.botty.photoviewer.MyApplication
import com.botty.photoviewer.R

data class Error(var code: Int?) {
    val message: String
    get() {
        return when(code) {
            400 -> MyApplication.getString(R.string.account_or_password_wrong)
            401 -> MyApplication.getString(R.string.account_disabled)
            402 -> MyApplication.getString(R.string.permission_denied)
            403 -> MyApplication.getString(R.string.two_step_login)
            else -> "${MyApplication.getString(R.string.error_code)}: $code"
        }
    }

    val newLoginNeeded: Boolean
    get() = code == 119
}