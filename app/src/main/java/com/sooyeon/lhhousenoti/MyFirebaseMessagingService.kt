package com.sooyeon.lhhousenoti

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sooyeon.lhhousenoti.Model.LHHouseInfo
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("MyFCM", "Message received: ${remoteMessage.data}")

        val data = remoteMessage.data
        // React에서 보내는 키값(소문자/카멜케이스)에 맞춰 수정
        val panId = data["panId"] ?: ""
        
        // 메시지를 받은 즉시 Realm에 저장 (isAlarmFlag = true)
        if (panId.isNotEmpty()) {
            updateAlarmFlag(data)
        }

        // 알림 센터에 표시 (앱이 포그라운드일 때도 표시됨)
        sendNotification(remoteMessage.notification?.title ?: "🏠 새 LH 분양 공고", 
                         remoteMessage.notification?.body ?: (data["panNm"] ?: "새로운 공고가 등록되었습니다."), 
                         data)
    }

    private fun updateAlarmFlag(data: Map<String, String>) {
        val panId = data["panId"] ?: ""
        CoroutineScope(Dispatchers.IO).launch {
            val realm = Realm.open(LHRealmConfig.config)
            try {
                realm.write {
                    val existing = query<LHHouseInfo>("PAN_ID == $0", panId).first().find()
                    if (existing != null) {
                        existing.isAlarmFlag = true
                    } else {
                        copyToRealm(LHHouseInfo().apply {
                            this.DTL_URL = data["dtlUrl"] ?: ""
                            this.isAlarmFlag = true
                            this.PAN_NM = data["panNm"] ?: "새로운 공고"
                            this.PAN_ID = data["panId"] ?: ""
                            this.CNP_CD_NM = data["cnpCdNm"] ?: ""
                            this.PAN_SS = data["panSs"] ?: ""
                            this.PAN_NT_ST_DT = data["panNtStDt"] ?: ""
                            this.CLSG_DT = data["panClsgDT"] ?: ""
                            this.AIS_TP_CD_NM = data["aisTpCdNm"] ?: ""
                            this.UPP_AIS_TP_CD = data["uppAisTpCd"] ?: ""
                            this.title = data["title"] ?: ""
                            this.isFavorite = false
                        })
                    }
                }
                Log.d("MyFCM", "Successfully saved to Realm: $panId")
            } catch (e: Exception) {
                Log.e("MyFCM", "Error saving to Realm", e)
            } finally {
                realm.close()
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "lhhouse_noti_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "LH House Notification",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("MyFCM", "Refreshed token: $token")
    }
}
