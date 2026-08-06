package me.rerere.rikkahub.ui.pages.setting.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

private val PRESET_COLORS = listOf(
    0xFFFFF1F6, 0xFFFFE0EC, 0xFFFFD6E7, 0xFFF8BBD0,
    0xFFE1F5FE, 0xFFD7F0E3, 0xFFFFF9C4, 0xFFFFE0B2,
    0xFFEDE7F6, 0xFFF5F5F5, 0xFFFFFFFF, 0xFF554040,
    0xFF3A2A32, 0xFF6B5450, 0xFF2C2C2C, 0xFF000000,
)

@Composable
fun ColorPickerRow(
    label: String,
    value: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hexText by remember(value) { mutableStateOf(hexOf(value)) }
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(value))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            )
            OutlinedTextField(
                value = hexText,
                onValueChange = { input ->
                    hexText = input
                    parseHex(input)?.let(onValueChange)
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = parseHex(hexText) == null,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PRESET_COLORS.take(8).forEach { c ->
                ColorDot(c, c == value) { onValueChange(c); hexText = hexOf(c) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PRESET_COLORS.drop(8).forEach { c ->
                ColorDot(c, c == value) { onValueChange(c); hexText = hexOf(c) }
            }
        }
    }
}

@Composable
private fun ColorDot(color: Long, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(if (selected) 28.dp else 24.dp)
            .clip(CircleShape)
            .background(Color(color))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
    )
}

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    unit: String = "dp",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.4f),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (unit == "%") "${(value * 100).toInt()}%" else "${value.toInt()}$unit",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.22f),
        )
    }
}

private fun hexOf(argb: Long): String =
    "#" + (argb and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()

private fun parseHex(text: String): Long? {
    val h = text.trim().removePrefix("#")
    if (h.length != 6 && h.length != 8) return null
    val v = h.lowercase().toLongOrNull(16) ?: return null
    return if (h.length == 6) 0xFF000000L or v else v
}
