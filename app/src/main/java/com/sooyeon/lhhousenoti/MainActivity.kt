package com.sooyeon.lhhousenoti

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.sooyeon.lhhousenoti.model.LHHouseModel
import com.sooyeon.lhhousenoti.viewmodel.LHHouseViewModel
import android.webkit.WebView
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 권한 허용됨
        } else {
            // 권한 거부됨
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (0 != (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        askNotificationPermission()

        setContent {
            val viewModel: LHHouseViewModel = viewModel()
            
            // 앱 실행 시 또는 새로운 인텐트 발생 시 푸시 데이터 처리
            LaunchedEffect(intent) {
                processIntent(intent, viewModel)
            }

            MaterialTheme {
                MainTabView(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun processIntent(intent: Intent, viewModel: LHHouseViewModel) {
        val extras = intent.extras ?: return
        val dtlUrl = extras.getString("dtlUrl") ?: extras.getString("DTL_URL")
        
        if (dtlUrl != null) {
            val userInfo = mutableMapOf<String, String>()
            extras.keySet().forEach { key ->
                extras.get(key)?.let { userInfo[key] = it.toString() }
            }
            userInfo["DTL_URL"] = dtlUrl
            viewModel.emitPushNotificationEvent(userInfo)
        }
    }
}

@Composable
fun MainTabView(viewModel: LHHouseViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // 앱 시작 시 Firestore에서 사용자 설정 로드
    LaunchedEffect(Unit) {
        viewModel.loadUserSettings(context)
    }

    // 현재 백스택 상태 확인 (어떤 탭이 활성화되어 있는지, 상세 화면인지 판단)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // SwiftUI의 NotificationCenter 이벤트 수신 (푸시 알림 클릭 등)
    LaunchedEffect(Unit) {
        // ViewModel 등에서 푸시 알림 클릭 이벤트를 Flow로 흘려주면 감지하여 네비게이션 처리
        viewModel.pushNotificationEvent.collect { userInfo ->
            // 예: 알림 탭으로 이동 후 상세화면 푸시
            val dtlUrl = userInfo["DTL_URL"] ?: ""
            if (dtlUrl.isNotEmpty()) {
                // 알림 탭으로 먼저 이동
                navController.navigate(Screen.Alarms.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                // 그 다음 상세 화면으로 푸시
                navController.navigate(Screen.Detail.createRoute(dtlUrl, isAlarmRead = true))
            }
        }
    }

    Scaffold(
        bottomBar = {
            // SwiftUI의 .toolbar(.hidden, for: .tabBar) 효과 구현
            // 현재 화면이 상세 화면("detail/...")이 아닐 때만 하단 탭바를 보여줍니다.
            if (currentRoute?.startsWith("detail") == false) {
                NavigationBar {
                    TabItem.entries.forEach { tab ->
                        val isSelected = currentRoute == tab.screen.route

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    navController.navigate(tab.screen.route) {
                                        // iOS TabView처럼 각 탭의 스택 상태를 보존하는 옵션
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                // SwiftUI의 .badge(viewModel.lhhouseAlarms.count) 구현
                                val alarmCount by viewModel.lhhouseAlarmsCount.collectAsState()
                                if (tab == TabItem.Alarms && alarmCount > 0) {
                                    BadgedBox(badge = { Badge { Text(alarmCount.toString()) } }) {
                                        Icon(tab.icon, contentDescription = tab.title)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tab.title)
                                }
                            },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // 공통 네비게이션 호스트
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. 홈 탭 (웹뷰가 들어가는 공간)
            composable(Screen.Home.route) {
                JSWebViewScreen(
                    url = "https://lhhousenoti.web.app",
                    viewModel = viewModel,
                    onNavigateToDetail = { model: LHHouseModel, url ->
                        viewModel.selectedHouseModel = model
                        Log.d("MainActivity", "onNavigateToDetail called with url: $url")
                        navController.navigate(Screen.Detail.createRoute(url, isAlarmRead = false, isFavoriteState = viewModel.getHouseByPanId(model.panId ?: "")?.isFavorite ?: false))
                    }
                )
            }

            // 2. 즐겨찾기 탭
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { model, url ->
                        viewModel.selectedHouseModel = model
                        navController.navigate(Screen.Detail.createRoute(url, isAlarmRead = false, isFavoriteState = viewModel.getHouseByPanId(model.panId ?: "")?.isFavorite ?: false))
                    }
                )
            }

            // 3. 알림 탭
            composable(Screen.Alarms.route) {
                AlarmReceiveScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { model, url ->
                        viewModel.selectedHouseModel = model
                        // 알림을 클릭해서 들어갈 때는 isAlarmReaded = true 처리
                        navController.navigate(Screen.Detail.createRoute(url, isAlarmRead = true, isFavoriteState = viewModel.getHouseByPanId(model.panId ?: "")?.isFavorite ?: false))
                    }
                )
            }

            // 4. 설정 탭
            composable(Screen.Settings.route) {
                AlarmSettingScreen(viewModel = viewModel)
            }

            // 5. 공통 상세 화면 (SwiftUI의 ExpandWebView)
            // 하단 탭바 영역 밖으로 푸시되는 효과를 위해 NavHost 바로 아래에 둡니다.
            @OptIn(ExperimentalMaterial3Api::class)
            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument("dtlUrl") { type = NavType.StringType },
                    navArgument("isAlarmRead") { type = NavType.BoolType },
                    navArgument("isFavoriteState") { type = NavType.BoolType },
                )
            ) { backStackEntry ->
                val dtlUrl = backStackEntry.arguments?.getString("dtlUrl") ?: ""
                val isAlarmRead = backStackEntry.arguments?.getBoolean("isAlarmRead") ?: false
                val isFavoriteState = backStackEntry.arguments?.getBoolean("isFavoriteState") ?: false

                // 알림을 통해 들어온 경우 읽음 처리
                if (isAlarmRead) {
                    LaunchedEffect(dtlUrl) {
                        viewModel.markAsRead(dtlUrl)
                    }
                }

                // 즐겨찾기 상태 관리
                var isFavorite by remember(dtlUrl) {
                    mutableStateOf(viewModel.getHouseByDtlUrl(dtlUrl)?.isFavorite ?: isFavoriteState)
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("상세 보기") },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { 
                                    viewModel.selectedHouseModel?.let { model ->
                                        viewModel.toggleFavorite(model)
                                        isFavorite = !isFavorite
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFavorite) Color(0xFF050400) else LocalContentColor.current
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                            )
                        )
                    }
                ) { padding ->
                    JSWebViewScreen(
                        url = dtlUrl,
                        viewModel = viewModel,
                        modifier = Modifier.padding(padding),
                        onNavigateToDetail = { _: LHHouseModel, url ->
                            navController.navigate(Screen.Detail.createRoute(url, isAlarmRead = false))
                        },
                        onCloseExpandWebView = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}