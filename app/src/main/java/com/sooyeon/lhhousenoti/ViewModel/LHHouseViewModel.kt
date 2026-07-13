package com.sooyeon.lhhousenoti.ViewModel

import android.content.Context
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sooyeon.lhhousenoti.LHRealmConfig
import com.sooyeon.lhhousenoti.DeviceIdentifier
import com.sooyeon.lhhousenoti.Manager.RemoteConfigManager
import com.sooyeon.lhhousenoti.Model.LHHouseInfo
import com.sooyeon.lhhousenoti.Model.LHHouseModel
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LHHouseViewModel : ViewModel() {
    private val realm = Realm.open(LHRealmConfig.config)
    private val db = FirebaseFirestore.getInstance()
    private val messaging = FirebaseMessaging.getInstance()
    val remoteConfigManager = RemoteConfigManager()

    // 현재 상세 화면에서 보고 있는 주택 모델 (모든 클래스에서 참조 가능)
    var selectedHouseModel: LHHouseModel? by mutableStateOf(null)

    // 알림 설정 상태 (fieldKey -> (itemCode -> isOn))
    // SnapshotStateMap을 중첩하여 내부 맵의 변경도 감지되도록 함
    var userSettings = mutableStateMapOf<String, SnapshotStateMap<String, Boolean>>()

    // 푸시 알림 이벤트를 전달하기 위한 SharedFlow
    private val _pushNotificationEvent = MutableSharedFlow<Map<String, String>>()
    val pushNotificationEvent: SharedFlow<Map<String, String>> = _pushNotificationEvent.asSharedFlow()

    // 읽지 않은 알림 개수 (isAlarmFlag == true)
    val lhhouseAlarmsCount: StateFlow<Int> = realm.query<LHHouseInfo>("isAlarmFlag == true")
        .asFlow()
        .map { it.list.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        remoteConfigManager.fetchRemoteValues {
            // 초기화 시 필요한 로직
        }
    }

    /**
     * 푸시 알림 클릭 이벤트를 발생시킵니다.
     */
    fun emitPushNotificationEvent(userInfo: Map<String, String>) {
        viewModelScope.launch {
            _pushNotificationEvent.emit(userInfo)
        }
    }

    /**
     * 특정 주택의 알림 플래그를 해제합니다.
     */
    fun markAsRead(dtlUrl: String) {
        viewModelScope.launch {
            realm.write {
                val house = query<LHHouseInfo>("DTL_URL == $0", dtlUrl).first().find()
                house?.isAlarmFlag = false
            }
        }
    }

    /**
     * Firestore에서 사용자 알림 설정 로드
     */
    fun loadUserSettings(context: Context) {
        val uuid = DeviceIdentifier.getDeviceUUID(context)
        viewModelScope.launch {
            try {
                val document = db.collection("users").document(uuid).get().await()
                if (document.exists()) {
                    val data = document.data ?: emptyMap()
                    // 캡쳐 화면처럼 루트 필드에서 직접 읽어옴 (CNP_CD_NM, PAN_SS, UPP_AIS_TP_CD 등)
                    data.forEach { (key, value) ->
                        if (value is List<*>) {
                            val map = userSettings.getOrPut(key) { mutableStateMapOf() }
                            value.forEach { code -> 
                                if (code is String) {
                                    map[code] = true 
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LHHouseViewModel", "Error loading user settings", e)
            }
        }
    }

    /**
     * 알림 설정 변경 및 Firestore 저장 + Firebase Topic 구독/해지
     */
    fun setUsersNotices(context: Context, isOn: Boolean, fieldKey: String, fieldItemName: String, fieldItemCode: String) {
        val uuid = DeviceIdentifier.getDeviceUUID(context)
        
        // Topic 이름 생성 (코드를 사용: 예: CNP_11)
        val prefix = when(fieldKey) {
            "CNP_CD_NM" -> "CNP_"
            "PAN_SS" -> "PAN_"
            "UPP_AIS_TP_CD" -> "UPP_"
            else -> ""
        }
        val topicName = "$prefix$fieldItemCode"

        // 1. 로컬 상태 업데이트 (UI 즉시 반영용: 저장된 이름 기준)
        val categoryMap = userSettings.getOrPut(fieldKey) { mutableStateMapOf() }
        if (isOn) {
            categoryMap[fieldItemName] = true
            if (topicName.isNotEmpty()) {
                messaging.subscribeToTopic(topicName)
                    .addOnCompleteListener { if (it.isSuccessful) Log.d("LHHouseViewModel", "Subscribed: $topicName") }
            }
        } else {
            categoryMap.remove(fieldItemName)
            if (topicName.isNotEmpty()) {
                messaging.unsubscribeFromTopic(topicName)
                    .addOnCompleteListener { if (it.isSuccessful) Log.d("LHHouseViewModel", "Unsubscribed: $topicName") }
            }
        }

        // 2. Firestore 저장 (이름을 사용: 예: 서울특별시)
        viewModelScope.launch {
            try {
                val docRef = db.collection("users").document(uuid)
                
                val updateData = if (isOn) {
                    mapOf(fieldKey to FieldValue.arrayUnion(fieldItemName))
                } else {
                    mapOf(fieldKey to FieldValue.arrayRemove(fieldItemName))
                }

                docRef.set(updateData, SetOptions.merge()).await()
                Log.d("LHHouseViewModel", "Firestore updated root field $fieldKey with name $fieldItemName")
            } catch (e: Exception) {
                Log.e("LHHouseViewModel", "Error saving user settings", e)
            }
        }
    }

    // 모든 즐겨찾기 목록을 관찰 가능한 Flow로 제공
    val favoriteHouses: StateFlow<List<LHHouseInfo>> = realm.query<LHHouseInfo>("isFavorite == true")
        .asFlow()
        .map { it.list }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 알림 받은 목록을 관찰 가능한 Flow로 제공
    val alarmHouses: StateFlow<List<LHHouseInfo>> = realm.query<LHHouseInfo>("isAlarmFlag == true")
        .asFlow()
        .map { it.list }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 특정 PAN_ID에 해당하는 주택 정보 검색
     */
    fun getHouseByPanId(panId: String): LHHouseInfo? {
        return realm.query<LHHouseInfo>("PAN_ID == $0", panId).find().firstOrNull()
    }

    /**
     * 특정 DTL_URL에 해당하는 주택 정보 검색
     */
    fun getHouseByDtlUrl(dtlUrl: String): LHHouseInfo? {
        return realm.query<LHHouseInfo>("DTL_URL == $0", dtlUrl).find().firstOrNull()
    }

    /**
     * LHHouseModel 데이터를 기반으로 Realm에 저장하거나 즐겨찾기 상태를 토글
     */
    fun toggleFavorite(model: LHHouseModel) {
        viewModelScope.launch {
            realm.write {
                val panId = model.panId ?: return@write
                val existing = query<LHHouseInfo>("PAN_ID == $0", panId).first().find()
                if (existing != null) {
                    existing.isFavorite = !existing.isFavorite
                } else {
                    copyToRealm(LHHouseInfo().apply {
                        this.PAN_ID = model.panId ?: ""
                        this.DTL_URL = model.dtlUrl ?: ""
                        this.title = model.title ?: ""
                        this.CNP_CD_NM = model.cnpCdNm ?: ""
                        this.PAN_SS = model.panSs ?: ""
                        this.PAN_NM = model.panNm ?: ""
                        this.AIS_TP_CD_NM = model.aisTpCdNm ?: ""
                        this.UPP_AIS_TP_CD = model.uppAisTpCd ?: ""
                        this.PAN_NT_ST_DT = model.panNtStDt ?: ""
                        this.CLSG_DT = model.clsgDt ?: ""
                        this.isFavorite = true
                    })
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        realm.close()
    }
}