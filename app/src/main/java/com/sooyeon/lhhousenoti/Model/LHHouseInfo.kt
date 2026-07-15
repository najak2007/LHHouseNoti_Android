package com.sooyeon.lhhousenoti.model

import io.realm.kotlin.types.RealmInstant
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class LHHouseInfo : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()
    var isFavorite: Boolean = false
    var DTL_URL: String = ""
    var title: String = ""
    var PAN_ID: String = ""
    var  CNP_CD_NM: String = ""
    var  PAN_SS: String = ""
    var  PAN_NM: String = ""
    var  AIS_TP_CD_NM: String = ""
    var  UPP_AIS_TP_CD: String = ""
    var  PAN_NT_ST_DT: String = ""
    var  CLSG_DT: String = ""
    var  registerDate: RealmInstant = RealmInstant.now()
    var  isAlarmFlag: Boolean = false
}