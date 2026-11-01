package com.mainecare.effects.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mainecare.effects.deck.CardDeck
import com.mainecare.effects.deck.CardStack
import com.mainecare.effects.deck.Coverflow
import com.mainecare.effects.glass.GlassBackdrop
import com.mainecare.effects.glass.GlassSurface
import com.mainecare.effects.glass.MaineGlassMaterials

private data class Meal(
    val id: String,
    val time: String,
    val name: String,
    val grams: String,
    val state: String,
)

private val meals = listOf(
    Meal("breakfast", "08:10", "早餐", "62g", "已计入"),
    Meal("lunch", "12:30", "午餐", "78g", "已计入"),
    Meal("dinner", "18:40", "晚餐", "46g", "已计入"),
    Meal("snack", "21:10", "加餐", "12g", "待补充"),
    Meal("water", "全天", "饮水", "180ml", "进行中"),
)

private val analyses = listOf(
    Triple("01 · PROTEIN", "蛋白质分布", "鸡胸与三文鱼贡献了今日 71% 的蛋白质，晚间摄入偏低。"),
    Triple("02 · RHYTHM", "进食节奏", "三餐间隔稳定在 4.2 小时，晚餐略晚于上周中位数。"),
    Triple("03 · BALANCE", "营养均衡", "脂肪比例 28%，纤维略少，建议明天加入一份绿叶菜。"),
    Triple("04 · TREND", "七日趋势", "本周日均 194g，比上周增加 6%，体重维持稳定。"),
    Triple("05 · HINT", "下次建议", "晚餐可提前 20 分钟，并将主食减至 40g。"),
)

@Composable
fun DeckLabScreen() {
    val backdrop = rememberBackdropAsset("backdrop-obsidian.png") ?: return
    var fpsEnabled by remember { mutableStateOf(false) }
    val fps = rememberFps(fpsEnabled)
    var stackIndex by remember { mutableIntStateOf(0) }
    var coverIndex by remember { mutableIntStateOf(2) }
    val deckState = rememberPagerState { analyses.size }

    GlassBackdrop(image = backdrop, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "V18 MOTION · V21 MATERIAL",
                        fontSize = 9.sp,
                        letterSpacing = 2.0.sp,
                        color = Obsidian.Faint,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (fpsEnabled) "FPS ON" else "FPS",
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp,
                        color = if (fpsEnabled) Obsidian.Gold else Obsidian.Faint,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { fpsEnabled = !fpsEnabled }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Text(
                    text = "Card Lab",
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Obsidian.Fg,
                )

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "CARD DECK",
                        fontSize = 9.sp,
                        letterSpacing = 1.8.sp,
                        color = Obsidian.Faint,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "0${deckState.currentPage + 1} / 0${analyses.size}",
                        fontSize = 10.sp,
                        color = Obsidian.Gold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                CardDeck(
                    pageCount = analyses.size,
                    state = deckState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                ) { index ->
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 2.dp),
                        material = MaineGlassMaterials.Card,
                        interactive = false,
                    ) {
                        AnalysisCard(analyses[index], index)
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ANALYSIS STACK",
                        fontSize = 9.sp,
                        letterSpacing = 1.8.sp,
                        color = Obsidian.Faint,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "0${stackIndex + 1} / 0${analyses.size}",
                        fontSize = 10.sp,
                        color = Obsidian.Gold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                CardStack(
                    count = analyses.size,
                    initialIndex = stackIndex,
                    onIndexChanged = { stackIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(224.dp),
                ) { index ->
                    GlassSurface(
                        modifier = Modifier.fillMaxSize(),
                        material = MaineGlassMaterials.Insight,
                        interactive = false,
                    ) {
                        AnalysisCard(analyses[index], index)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "← 拖动或甩出卡片，超过 22% 宽度即切换 →",
                    fontSize = 9.sp,
                    color = Obsidian.Faint,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "MEAL COVERFLOW",
                        fontSize = 9.sp,
                        letterSpacing = 1.8.sp,
                        color = Obsidian.Faint,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "0${coverIndex + 1} / 0${meals.size}",
                        fontSize = 10.sp,
                        color = Obsidian.Gold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Coverflow(
                    items = meals,
                    initialIndex = 2,
                    itemWidth = 154.dp,
                    itemHeight = 72.dp,
                    onIndexChanged = { coverIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                ) { meal, index ->
                    val base = MaineGlassMaterials.MealFront
                    val material = if (index == coverIndex) {
                        base.copy(
                            borderColor = Color(0x6BE3C08A),
                            topHighlightColor = Color(0x40FFFFFF),
                        )
                    } else {
                        base
                    }
                    GlassSurface(
                        modifier = Modifier.fillMaxSize(),
                        material = material,
                        interactive = false,
                    ) {
                        MealCard(meal)
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
            if (fpsEnabled) {
                Text(
                    text = String.format("%.1f fps", fps),
                    fontSize = 10.sp,
                    color = Obsidian.Gold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 34.dp, end = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun AnalysisCard(analysis: Triple<String, String, String>, index: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                analysis.first,
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                color = Obsidian.Gold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "0${index + 1} / 05",
                fontSize = 9.sp,
                color = Obsidian.Faint,
            )
        }
        Column {
            Text(
                analysis.second,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = Obsidian.Fg,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                analysis.third,
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = Obsidian.Muted,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("置信度 84%", fontSize = 9.sp, color = Obsidian.Cool)
            Spacer(Modifier.weight(1f))
            Text("查看  →", fontSize = 9.sp, color = Obsidian.Gold)
        }
    }
}

@Composable
private fun MealCard(meal: Meal) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                meal.time,
                fontSize = 7.5.sp,
                letterSpacing = 0.8.sp,
                color = Obsidian.Faint,
            )
            Spacer(Modifier.weight(1f))
            Text(
                meal.state,
                fontSize = 7.5.sp,
                color = if (meal.state == "已计入") Obsidian.Gold else Obsidian.Faint,
            )
        }
        Text(
            meal.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Obsidian.Fg,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(meal.grams, fontSize = 9.sp, color = Obsidian.Muted)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .width(16.dp)
                    .height(1.dp)
                    .background(Color(0x66E3C08A)),
            )
        }
    }
}
