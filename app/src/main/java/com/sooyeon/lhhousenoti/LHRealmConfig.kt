package com.sooyeon.lhhousenoti

import com.sooyeon.lhhousenoti.model.LHHouseInfo
import io.realm.kotlin.RealmConfiguration

object LHRealmConfig {
    val config = RealmConfiguration.Builder(schema = setOf(LHHouseInfo::class))
        .name("lhhouse.realm")
        .schemaVersion(1)
        .build()
}
