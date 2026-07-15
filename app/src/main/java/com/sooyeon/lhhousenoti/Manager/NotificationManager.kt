package com.sooyeon.lhhousenoti.manager

import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log

object NotificationManager {

    fun getPushToken(onTokenReceived: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("NotificationManager", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("NotificationManager", "Token: $token")
            onTokenReceived(token)
        }
    }
}