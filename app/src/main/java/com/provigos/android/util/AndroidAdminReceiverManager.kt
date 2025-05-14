package com.provigos.android.util

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import com.provigos.android.data.local.SharedPreferenceManager
import timber.log.Timber

class AndroidAdminReceiverManager: DeviceAdminReceiver() {

    private val sharedPrefs = SharedPreferenceManager.get()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Timber.tag("AdminReceiver").d("Received intent ${intent.action}")
    }
    override fun onPasswordFailed(context: Context, intent: Intent, user: UserHandle) {
        super.onPasswordFailed(context, intent, user)
        sharedPrefs.incrementUnlockAttemptsCount()
    }

}