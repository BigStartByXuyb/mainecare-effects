package com.mainecare.effects.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mainecare.effects.deck.CardDeck
import com.mainecare.effects.glass.GlassBackdrop
import com.mainecare.effects.glass.GlassSurface
import com.mainecare.effects.glass.MaineGlassMaterials

private data class Feature(val tag: String, val title: String, val body: String, val hint: String)

private val features = listOf(
    Feature(
        tag = "01 / AGSL",
        title = "液态玻璃",
        body = "边缘折射、色散与高光在单个 AGSL pass 内合成，参数对齐 liquid-glass-react。",
        hint = "拖动玻璃，观察高光跟随",
    ),
    Feature(
        tag = "02 / OBSIDIAN",
        title = "黑金材质",
        body = "v21 的深色玻璃底膜、顶沿高光、内侧阴影与香槟金细节完整还原。",
        hint = "背景照片提供真实折射源",
    ),
    Feature(
        tag = "03 / CAPTURE",
        title = "背景取样",
        body = "背景先抽样为低分辨率模糊图，着色器按屏幕坐标精确取样，不随卡片重建。",
        hint = "静态照片 / 任意快照背景",
    ),
)

@Composable
fun GlassGalleryScreen() {
    val backdrop = rememberBackdropAsset("backdrop.png") ?: return
    GlassBackdrop(image = backdrop, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            SectionLabel("OBSIDIAN VEIL · V21")
            Spacer(Modifier.height(4.dp))
            Text(
                text = "今日建议",
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.SemiBold,
                color = Obsidian.Fg,
            )
            Spacer(Modifier.height(12.dp))
            AdviceCard()
            Spacer(Modifier.height(16.dp))

            val pager = rememberPagerState { features.size }
            CardDeck(
                pageCount = features.size,
                state = pager,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(216.dp),
            ) { index ->
                GlassSurface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp),
                    material = MaineGlassMaterials.Card,
                    interactive = true,
                ) {
                    FeatureCard(features[index])
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(features.size) { index ->
                        Box(
                            Modifier
                                .height(3.dp)
                                .width(if (index == pager.currentPage) 22.dp else 8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (index == pager.currentPage) Obsidian.Gold else Color(0x33EBEFF4),
                                ),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "左右滑动",
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    color = Obsidian.Faint,
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill("拖动玻璃 · 高光")
                GlassPill("v21 · API 36")
            }
            Spacer(Modifier.height(16.dp))
            InsightCard()
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AdviceCard() {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp),
        material = MaineGlassMaterials.Advice,
        interactive = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Obsidian.Gold),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "今日建议",
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    color = Obsidian.Gold,
                )
                Spacer(Modifier.weight(1f))
                Text("AI · 08:40", fontSize = 10.sp, color = Obsidian.Faint)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "186",
                    fontSize = 52.sp,
                    lineHeight = 54.sp,
                    fontWeight = FontWeight.Light,
                    color = Obsidian.Fg,
                )
                Text(" g", fontSize = 16.sp, color = Obsidian.Muted)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("目标 300g", fontSize = 11.sp, color = Obsidian.Faint)
                    Text(
                        "62%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = Obsidian.Gold,
                    )
                }
            }
            Text(
                text = "今天的三餐节奏稳定，晚餐还差 114g。",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Obsidian.Muted,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("较昨日 +12g", fontSize = 11.sp, color = Obsidian.Cool)
                Spacer(Modifier.weight(1f))
                Text("查看详情  →", fontSize = 11.sp, color = Obsidian.Gold)
            }
        }
    }
}

@Composable
private fun FeatureCard(feature: Feature) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = feature.tag,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = Obsidian.Gold,
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0x66E3C08A)),
            )
        }
        Column {
            Text(
                text = feature.title,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = Obsidian.Fg,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = feature.body,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Obsidian.Muted,
            )
        }
        Text(
            text = feature.hint,
            fontSize = 10.sp,
            letterSpacing = 0.6.sp,
            color = Obsidian.Faint,
        )
    }
}

@Composable
private fun GlassPill(label: String) {
    val material = MaineGlassMaterials.Dock
    GlassSurface(
        modifier = Modifier.height(34.dp),
        material = material,
        params = material.liquid.copy(cornerRadius = 17.dp),
        interactive = true,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 11.sp,
            color = Obsidian.Muted,
        )
    }
}

@Composable
private fun InsightCard() {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
        material = MaineGlassMaterials.Insight,
        interactive = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x1FE3C08A)),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", fontSize = 16.sp, color = Obsidian.Gold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "识别到 3 种食材",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Obsidian.Fg,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "鸡肉 · 南瓜 · 三文鱼",
                    fontSize = 11.sp,
                    color = Obsidian.Muted,
                )
            }
            Text("84%", fontSize = 22.sp, color = Obsidian.Gold)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 9.sp,
        letterSpacing = 2.2.sp,
        color = Obsidian.Faint,
    )
}
