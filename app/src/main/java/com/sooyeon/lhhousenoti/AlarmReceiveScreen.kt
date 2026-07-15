package com.sooyeon.lhhousenoti

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sooyeon.lhhousenoti.model.LHHouseInfo
import com.sooyeon.lhhousenoti.model.LHHouseModel
import com.sooyeon.lhhousenoti.viewmodel.LHHouseViewModel

@Composable
fun AlarmReceiveScreen(
    viewModel: LHHouseViewModel,
    onNavigateToDetail: (LHHouseModel, String) -> Unit
) {
    val alarms by viewModel.alarmHouses.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // 📱 상단 고정 헤더 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFFFFFF))
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = "받은 알림",
                color = Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "받은 알림이 없습니다.",
                    color = Color(0xFF7F8C8D),
                    fontSize = 20.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 15.dp, bottom = 76.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(alarms) { house ->
                    AlarmNoticeCard(
                        house = house,
                        onClick = {
                            val model = LHHouseModel(
                                panId = house.PAN_ID,
                                dtlUrl = house.DTL_URL,
                                title = house.title,
                                cnpCdNm = house.CNP_CD_NM,
                                panSs = house.PAN_SS,
                                panNm = house.PAN_NM,
                                aisTpCdNm = house.AIS_TP_CD_NM,
                                uppAisTpCd = house.UPP_AIS_TP_CD,
                                panNtStDt = house.PAN_NT_ST_DT,
                                clsgDt = house.CLSG_DT
                            )
                            onNavigateToDetail(model, house.DTL_URL)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmNoticeCard(
    house: LHHouseInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 뱃지 컨테이너
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Badge(
                    text = house.PAN_SS,
                    backgroundColor = Color(0xFFE6F7ED),
                    textColor = Color(0xFF219653)
                )
                Badge(
                    text = house.CNP_CD_NM,
                    backgroundColor = Color(0xFFE8F2FF),
                    textColor = Color(0xFF2F80ED)
                )
            }

            // 공고 제목
            Text(
                text = house.PAN_NM,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
                color = Color(0xFF333333),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 날짜 영역
            HorizontalDivider(
                color = Color(0xFFF2F2F2),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                DateItem(label = "공고일", date = house.PAN_NT_ST_DT)
                DateItem(label = "마감일", date = house.CLSG_DT, isDeadline = true)
            }
        }
    }
}
