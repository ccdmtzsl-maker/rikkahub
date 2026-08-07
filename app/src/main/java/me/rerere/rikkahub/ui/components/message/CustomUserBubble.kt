package me.rerere.rikkahub.ui.components.message

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.clipPath
import kotlinx.datetime.LocalDateTime
import me.rerere.rikkahub.data.datastore.UserBubbleStyle
import coil3.compose.rememberAsyncImagePainter

private const val IMESSAGE_TAIL_URL = "https://imgbed.heliar.top/i/OE0IpZ9UDvkx2caP.webp"

@Composable
fun CustomUserBubble(
    style: UserBubbleStyle,
    isDark: Boolean,
    createdAt: LocalDateTime?,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val bg = remember(style, isDark) {
        Color(if (isDark) style.backgroundDark else style.background)
            .copy(alpha = style.opacity.coerceIn(0f, 1f))
    }
    val fg = remember(style, isDark) {
        Color(if (isDark) style.textColorDark else style.textColor)
    }
    val strokeColor = remember(style, isDark) {
        Color(if (isDark) style.borderColorDark else style.borderColor)
    }
    val shape = remember(style.cornerRadius) { RoundedCornerShape(style.cornerRadius.dp) }
    val innerPadding = remember(style) {
        PaddingValues(
            start = style.paddingStart.dp,
            top = style.paddingTop.dp,
            end = style.paddingEnd.dp,
            bottom = style.paddingBottom.dp,
        )
    }
    val timeText = remember(createdAt, style.showTime, style.timeFormat) {
        if (style.showTime && createdAt != null) formatBubbleTime(createdAt, style.timeFormat) else null
    }
    val useOutline = style.outlineOffset > 0f && style.borderWidth > 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = style.marginStart.dp,
                end = style.marginEnd.dp,
                top = style.marginVertical.dp,
                bottom = style.marginVertical.dp,
            )
    ) {
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            if (style.showTail && style.tailStyle == UserBubbleStyle.TailStyle.IMESSAGE) {
                val tailScale = 2.1f
                val tailWidth = style.tailSize.dp * tailScale
                val tailHeight = style.tailSize.dp * 1.2f * tailScale
                val tailOffsetX = style.tailSize.dp * 0.95f
                val tailOffsetY = style.tailSize.dp * 0.18f
                // 不参与父布局测量；旧 TRIANGLE 数据不再渲染，避免 Telegram 尾巴残留。
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawWithContent {
                            val seam = 2.dp.toPx()
                            val radius = minOf(
                                style.cornerRadius.dp.toPx().coerceAtLeast(0f),
                                size.width / 2f,
                                size.height / 2f,
                            )
                            // EvenOdd: 全画布减去气泡内缩区域 → 只保留气泡外部+边缘2dp接缝
                            val clip = Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(Rect(0f, 0f, size.width, size.height))
                                addRoundRect(
                                    RoundRect(
                                        left = seam,
                                        top = seam,
                                        right = size.width - seam,
                                        bottom = size.height - seam,
                                        topLeft = CornerRadius(radius, radius),
                                        topRight = CornerRadius(radius, radius),
                                        bottomRight = CornerRadius(radius, radius),
                                        bottomLeft = CornerRadius(radius, radius),
                                    )
                                )
                            }
                            clipPath(clip) {
                                this@drawWithContent.drawContent()
                            }
                        }
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(IMESSAGE_TAIL_URL),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(bg, BlendMode.SrcIn),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = tailOffsetX, y = tailOffsetY)
                            .size(width = tailWidth, height = tailHeight),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Surface(
                modifier = Modifier.animateContentSize(),
                shape = shape,
                color = bg,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = if (!useOutline && style.borderWidth > 0f) {
                    BorderStroke(style.borderWidth.dp, strokeColor)
                } else null,
                onClick = { onClick?.invoke() },
            ) {
                Box(modifier = Modifier.padding(innerPadding)) {
                    Column {
                        ProvideTextStyle(LocalTextStyle.current.copy(color = fg)) {
                            content()
                        }
                        if (timeText != null && style.timePosition == UserBubbleStyle.TimePosition.BELOW) {
                            Text(
                                text = timeText,
                                fontSize = style.timeSize.sp,
                                color = fg.copy(alpha = 0.55f),
                                textAlign = TextAlign.End,
                                modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                            )
                        }
                    }

                }
            }
            if (useOutline) {
                val off = style.outlineOffset.dp
                val w = style.borderWidth.dp
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = -off, y = off)
                        .border(w, strokeColor, shape)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = off, y = -off)
                        .border(w, strokeColor, shape)
                )
            }
        if (timeText != null && style.timePosition == UserBubbleStyle.TimePosition.INLINE) {
            Text(
                text = timeText,
                fontSize = style.timeSize.sp,
                color = fg.copy(alpha = 0.4f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = style.marginVertical.dp),
            )
        }
        }
    }
}

fun formatBubbleTime(t: LocalDateTime, format: UserBubbleStyle.TimeFormat): String {
    fun p(v: Int) = v.toString().padStart(2, '0')
    return when (format) {
        UserBubbleStyle.TimeFormat.HH_MM -> "${p(t.hour)}:${p(t.minute)}"
        UserBubbleStyle.TimeFormat.HH_MM_SS -> "${p(t.hour)}:${p(t.minute)}:${p(t.second)}"
        UserBubbleStyle.TimeFormat.MD_HH_MM -> "${p(t.monthNumber)}-${p(t.dayOfMonth)} ${p(t.hour)}:${p(t.minute)}"
    }
}
