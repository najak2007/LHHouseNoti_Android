package com.sooyeon.lhhousenoti

import com.sooyeon.lhhousenoti.Model.LHHouseInfo
import io.realm.kotlin.RealmConfiguration

object LHRealmConfig {
    val config = RealmConfiguration.Builder(schema = setOf(LHHouseInfo::class))
        .name("lhhouse.realm")
        .schemaVersion(1)
        .build()
}
