package com.sooyeon.lhhousenoti

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sooyeon.lhhousenoti.ViewModel.LHHouseViewModel

@Composable
fun AlarmSettingScreen(viewModel: LHHouseViewModel) {
    val context = LocalContext.current
    val locationNames = viewModel.remoteConfigManager.locationNames
    val panSSNames = viewModel.remoteConfigManager.panSSNames
    val uppaistpcdNames = viewModel.remoteConfigManager.uppaistpcdNames

    LaunchedEffect(Unit) {
        viewModel.loadUserSettings(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // 상단 타이틀
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFFFFFF))
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = "알림 설정",
                color = Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            // 지역 설정 섹션
            item {
                SectionHeader(title = "지역 설정")
            }
            
            val locationPairs = locationNames.chunked(2)
            items(locationPairs) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    val (name1, code1) = splitOption(pair[0])
                    SettingToggleItem(
                        label = name1,
                        isOn = viewModel.userSettings["CNP_CD_NM"]?.get(name1) ?: false,
                        onToggle = { isOn ->
                            viewModel.setUsersNotices(context, isOn, "CNP_CD_NM", name1, code1)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (pair.size > 1) {
                        val (name2, code2) = splitOption(pair[1])
                        SettingToggleItem(
                            label = name2,
                            isOn = viewModel.userSettings["CNP_CD_NM"]?.get(name2) ?: false,
                            onToggle = { isOn ->
                                viewModel.setUsersNotices(context, isOn, "CNP_CD_NM", name2, code2)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = Color.Gray.copy(alpha = 0.3f)
                )
            }
/*
            // 공고 상태 설정 섹션
            item {
                SectionHeader(title = "공고 상태 설정")
            }
            val panSSPairs = panSSNames.chunked(2)
            items(panSSPairs) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    val (name1, code1) = splitOption(pair[0])
                    SettingToggleItem(
                        label = name1,
                        isOn = viewModel.userSettings["PAN_SS"]?.get(name1) ?: false,
                        onToggle = { isOn ->
                            viewModel.setUsersNotices(context, isOn, "PAN_SS", name1, code1)
                        },
                        modifier = Modifier.weight(1f),
                        activeColor = Color(0xFF219653)
                    )
                    
                    if (pair.size > 1) {
                        val (name2, code2) = splitOption(pair[1])
                        SettingToggleItem(
                            label = name2,
                            isOn = viewModel.userSettings["PAN_SS"]?.get(name2) ?: false,
                            onToggle = { isOn ->
                                viewModel.setUsersNotices(context, isOn, "PAN_SS", name2, code2)
                            },
                            modifier = Modifier.weight(1f),
                            activeColor = Color(0xFF219653)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = Color.Gray.copy(alpha = 0.3f)
                )
            }

            // 공고 유형 섹션
            item {
                SectionHeader(title = "공고 유형")
            }
            val uppAisTpCdPairs = uppaistpcdNames.chunked(2)
            items(uppAisTpCdPairs) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    val (name1, code1) = splitOption(pair[0])
                    SettingToggleItem(
                        label = name1,
                        isOn = viewModel.userSettings["UPP_AIS_TP_CD"]?.get(name1) ?: false,
                        onToggle = { isOn ->
                            viewModel.setUsersNotices(context, isOn, "UPP_AIS_TP_CD", name1, code1)
                        },
                        modifier = Modifier.weight(1f),
                        activeColor = Color.Black
                    )
                    
                    if (pair.size > 1) {
                        val (name2, code2) = splitOption(pair[1])
                        SettingToggleItem(
                            label = name2,
                            isOn = viewModel.userSettings["UPP_AIS_TP_CD"]?.get(name2) ?: false,
                            onToggle = { isOn ->
                                viewModel.setUsersNotices(context, isOn, "UPP_AIS_TP_CD", name2, code2)
                            },
                            modifier = Modifier.weight(1f),
                            activeColor = Color.Black
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = Color.Gray.copy(alpha = 0.3f)
                )
            }
 */
        }
    }
}

// "서울특별시:11" 형태를 분리하는 헬퍼 함수
private fun splitOption(option: String): Pair<String, String> {
    val parts = option.split(":")
    return if (parts.size >= 2) {
        parts[0] to parts[1]
    } else {
        option to option
    }
}

@Composable
fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
    }
}

@Composable
fun SettingToggleItem(
    label: String,
    isOn: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF2F80ED)
) {
    Row(
        modifier = modifier
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isOn,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = activeColor,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}
