package com.qihe.clipflow.ui.parser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.qihe.clipflow.data.repository.DouyinParseRoute
import com.qihe.clipflow.ui.components.GlassCard
import com.qihe.clipflow.ui.component.LiquidButton
import com.qihe.clipflow.ui.component.LiquidButtonTint

@Composable
fun ParseRouteCard(
    selectedRoute: DouyinParseRoute,
    onRouteChange: (DouyinParseRoute) -> Unit
) {
    GlassCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "\u89e3\u6790\u7ebf\u8def",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DouyinParseRoute.entries.forEach { route ->
                    LiquidButton(
                        onClick = { onRouteChange(route) },
                        modifier = Modifier.weight(1f),
                        tint = if (selectedRoute == route) LiquidButtonTint else Color.Unspecified,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text(
                            text = when (route) {
                                DouyinParseRoute.PRIMARY -> "\u7ebf\u8def1"
                                DouyinParseRoute.BACKUP -> "\u7ebf\u8def2\uff08\u4f4e\u6e05\uff09"
                            },
                            color = if (selectedRoute == route) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
