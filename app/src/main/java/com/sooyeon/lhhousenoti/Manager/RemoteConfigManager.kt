package com.sooyeon.lhhousenoti.manager

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import org.json.JSONArray

class RemoteConfigManager {
    private val remoteConfig = Firebase.remoteConfig

    var locationNames by mutableStateOf<List<String>>(emptyList())
    var panSSNames by mutableStateOf<List<String>>(emptyList())
    var uppaistpcdNames by mutableStateOf<List<String>>(emptyList())

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 86400 // 1일 캐시
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // iOS와 동일한 디폴트 값 설정
        val defaults = mapOf(
            "location_names" to "[\"서울특별시:11\", \"부산광역시:26\", \"대구광역시:27\", \"인천광역시:28\", \"광주광역시:29\", \"대전광역시:30\", \"울산광역시:31\", \"세종특별자치시:36110\", \"경기도:41\", \"강원도:42\", \"충청북도:43\", \"충청남도:44\", \"전라북도:52\", \"전라남도:46\", \"경상북도:47\", \"경상남도:48\", \"제주특별자치도:50\"]",
            "panss_names" to "[\"공고중\", \"접수중\", \"접수마감\", \"상담요청\", \"정정공고중\"]",
            "uppaistpcd_names" to "[\"분양주택\", \"토지\", \"임대주택\", \"주거복지\", \"상가\", \"신혼희망타운\"]"
        )
        remoteConfig.setDefaultsAsync(defaults)
        
        fetchRemoteValues()
    }

    fun fetchRemoteValues(onComplete: (() -> Unit)? = null) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("RemoteConfigManager", "Config params updated: ${task.isSuccessful}")
            } else {
                Log.e("RemoteConfigManager", "Fetch failed")
            }
            updateValues()
            onComplete?.invoke()
        }
    }

    private fun updateValues() {
        locationNames = getStringList("location_names")
        panSSNames = getStringList("panss_names")
        uppaistpcdNames = getStringList("uppaistpcd_names")
    }

    private fun getStringList(key: String): List<String> {
        val jsonString = remoteConfig.getString(key)
        if (jsonString.isEmpty()) return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            Log.e("RemoteConfigManager", "Error parsing $key", e)
            emptyList()
        }
    }
}
