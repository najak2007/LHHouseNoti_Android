package com.sooyeon.lhhousenoti

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

// Navigation Route 정의
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Favorites : Screen("favorites")
    object Alarms : Screen("alarms")
    object Settings : Screen("settings")

    // 상세 화면 Route (SwiftUI의 ExpandWebView 역할)
    // 인자를 전달하기 위해 템플릿 형태로 정의합니다.
    object Detail : Screen("detail/{dtlUrl}/{isAlarmRead}/{isFavoriteState}") {
        fun createRoute(dtlUrl: String, isAlarmRead: Boolean, isFavoriteState: Boolean = false): String {
            val encodedUrl = java.net.URLEncoder.encode(dtlUrl, "UTF-8")
            return "detail/$encodedUrl/$isAlarmRead/$isFavoriteState"
        }
    }
}

// 하단 탭바에 들어갈 아이템 정의
enum class TabItem(val screen: Screen, val icon: ImageVector, val title: String) {
    Home(Screen.Home, Icons.Default.Home, "홈"),
    Favorites(Screen.Favorites, Icons.Default.Star, "즐겨찾기"),
    Alarms(Screen.Alarms, Icons.Default.Notifications, "알림"),
    Settings(Screen.Settings, Icons.Default.Settings, "설정")
}

