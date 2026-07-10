package com.sooyeon.lhhousenoti

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import java.util.UUID

object DeviceIdentifier {
    @SuppressLint("HardwareIds")

    /**
    * 기기 고유 식별자 (Android ID)를 반환합니다.
    * 공장 초기화 전까지 앱 재설치와 상관없이 유지됩니다.
    * */
    fun getDeviceUUID(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }

    /**
     * 앱 설치 시 생성되는 UUID를 반환합니다. (SharedPreferences 저장 방식)
     * 앱 삭제 전까지 유지되며, 보안상 더 권장되는 방식입니다.
     */
    fun getAppInstanceId(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var uuid = prefs.getString("unique_id", null)
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            prefs.edit().putString("unique_id", uuid).apply()
        }
        return uuid
    }
}