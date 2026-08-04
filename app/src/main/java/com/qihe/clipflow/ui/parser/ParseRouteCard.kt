package com.qihe.clipflow.ui.parser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qihe.clipflow.data.repository.DouyinParseRoute
import com.qihe.clipflow.ui.components.GlassCard

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
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DouyinParseRoute.entries.forEachIndexed { index, route ->
                    SegmentedButton(
                        selected = selectedRoute == route,
                        onClick = { onRouteChange(route) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = DouyinParseRoute.entries.size
                        ),
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                text = when (route) {
                                    DouyinParseRoute.PRIMARY -> "\u7ebf\u8def1"
                                    DouyinParseRoute.BACKUP -> "\u7ebf\u8def2"
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}
