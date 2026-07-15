package com.sooyeon.lhhousenoti.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * iOS의 LHHouseModel과 동일한 구조의 데이터 모델
 */
data class LHHouseModel(
    @SerializedName("DTL_URL") val dtlUrl: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("PAN_ID") val panId: String? = null,
    @SerializedName("CNP_CD_NM") val cnpCdNm: String? = null,
    @SerializedName("PAN_SS") val panSs: String? = null,
    @SerializedName("PAN_NM") val panNm: String? = null,
    @SerializedName("AIS_TP_CD_NM") val aisTpCdNm: String? = null,
    @SerializedName("UPP_AIS_TP_CD") val uppAisTpCd: String? = null,
    @SerializedName("PAN_NT_ST_DT") val panNtStDt: String? = null,
    @SerializedName("CLSG_DT") val clsgDt: String? = null,
    @SerializedName("action") val action: String? = null,
    @SerializedName("url") val url: Boolean = false
) {
    companion object {
        /**
         * JSON 문자열을 LHHouseModel 객체로 변환
         */
        fun fromJson(json: String): LHHouseModel? {
            return try {
                Gson().fromJson(json, LHHouseModel::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * LHHouseModel 객체를 JSON 문자열로 변환
     */
    fun toJson(): String {
        return Gson().toJson(this)
    }
}
