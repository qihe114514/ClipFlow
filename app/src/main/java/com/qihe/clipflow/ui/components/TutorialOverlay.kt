package com.qihe.clipflow.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TutorialOverlay(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "tutorial_arrow")
    val arrowOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tutorial_arrow_offset"
    )
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("2193", fontSize = 40.sp, color = Color.White, modifier = Modifier.offset(y = arrowOffset.dp))
            Spacer(Modifier.height(8.dp))
            Text("点击封面在线播放", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("长按封面保存到相册", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Spacer(Modifier.height(120.dp))
            Text("点任意位置关闭", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }
    }
}
