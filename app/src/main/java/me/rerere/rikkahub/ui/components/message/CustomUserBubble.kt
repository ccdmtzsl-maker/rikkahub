package me.rerere.rikkahub.ui.components.message

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.datetime.LocalDateTime
import me.rerere.rikkahub.data.datastore.UserBubbleStyle
import kotlin.math.roundToInt

private const val IMESSAGE_BUBBLE_URL = "https://imgbed.heliar.top/i/a84EPVMPta7xHRe6.webp"

// 九宫格固定边距（dp），角落区域不拉伸
private val NINE_PATCH_LEFT = 18.dp
private val NINE_PATCH_RIGHT = 23.dp
private val NINE_PATCH_TOP = 18.dp
private val NINE_PATCH_BOTTOM = 22.dp

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
    val useNinePatch = style.showTail && style.tailStyle == UserBubbleStyle.TailStyle.IMESSAGE

    if (useNinePatch) {
        NinePatchBubble(
            bg = bg,
            fg = fg,
            style = style,
            timeText = timeText,
            onClick = onClick,
            content = content,
        )
    } else {
        SurfaceBubble(
            bg = bg,
            fg = fg,
            strokeColor = strokeColor,
            shape = shape,
            innerPadding = innerPadding,
            style = style,
            timeText = timeText,
            useOutline = useOutline,
            onClick = onClick,
            content = content,
        )
    }
}

@Composable
private fun NinePatchBubble(
    bg: Color,
    fg: Color,
    style: UserBubbleStyle,
    timeText: String?,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context).data(IMESSAGE_BUBBLE_URL).build()
        val result = loader.execute(request)
        if (result is SuccessResult) {
            bitmap = result.image.toBitmap().asImageBitmap()
        }
    }
    val colorFilter = remember(bg) { ColorFilter.tint(bg, BlendMode.SrcIn) }

    // 九宫格源图切割比例
    val sliceHStart = 0.20f
    val sliceHEnd = 0.87f
    val sliceVStart = 0.45f
    val sliceVEnd = 0.46f

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
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .widthIn(max = 320.dp)
                .drawBehind {
                    val img = bitmap ?: return@drawBehind
                    drawNinePatch(
                        image = img,
                        dstSize = size,
                        density = density,
                        sliceHStart = sliceHStart,
                        sliceHEnd = sliceHEnd,
                        sliceVStart = sliceVStart,
                        sliceVEnd = sliceVEnd,
                        colorFilter = colorFilter,
                    )
                }
                .padding(
                    start = NINE_PATCH_LEFT,
                    top = NINE_PATCH_TOP,
                    end = NINE_PATCH_RIGHT,
                    bottom = NINE_PATCH_BOTTOM,
                )
                .animateContentSize()
        ) {
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
}

/**
 * 九宫格绘制：四角保持固定 dp 尺寸不变形，中间区域拉伸填充。
 * sliceH/V 定义源图中"可拉伸区域"的起止比例。
 */
private fun DrawScope.drawNinePatch(
    image: ImageBitmap,
    dstSize: Size,
    density: Density,
    sliceHStart: Float,
    sliceHEnd: Float,
    sliceVStart: Float,
    sliceVEnd: Float,
    colorFilter: ColorFilter,
) {
    val imgW = image.width
    val imgH = image.height

    // 源图切割像素
    val srcLeft = (imgW * sliceHStart).toInt()
    val srcRight = (imgW * sliceHEnd).toInt()
    val srcTop = (imgH * sliceVStart).toInt()
    val srcBottom = (imgH * sliceVEnd).toInt()

    // 目标尺寸：角落用固定 dp 值
    val dstLeftPx = with(density) { NINE_PATCH_LEFT.toPx() }
    val dstRightPx = with(density) { NINE_PATCH_RIGHT.toPx() }
    val dstTopPx = with(density) { NINE_PATCH_TOP.toPx() }
    val dstBottomPx = with(density) { NINE_PATCH_BOTTOM.toPx() }

    val dstW = dstSize.width
    val dstH = dstSize.height
    val dstMiddleW = (dstW - dstLeftPx - dstRightPx).coerceAtLeast(0f)
    val dstMiddleH = (dstH - dstTopPx - dstBottomPx).coerceAtLeast(0f)

    fun slice(srcOff: IntOffset, srcSz: IntSize, dstX: Float, dstY: Float, dstW: Float, dstH: Float) {
        if (srcSz.width <= 0 || srcSz.height <= 0 || dstW <= 0f || dstH <= 0f) return
        drawImage(
            image = image,
            srcOffset = srcOff,
            srcSize = srcSz,
            dstOffset = IntOffset(dstX.roundToInt(), dstY.roundToInt()),
            dstSize = IntSize(dstW.roundToInt().coerceAtLeast(1), dstH.roundToInt().coerceAtLeast(1)),
            colorFilter = colorFilter,
        )
    }

    // Top row
    slice(IntOffset(0, 0), IntSize(srcLeft, srcTop), 0f, 0f, dstLeftPx, dstTopPx)
    slice(IntOffset(srcLeft, 0), IntSize(srcRight - srcLeft, srcTop), dstLeftPx, 0f, dstMiddleW, dstTopPx)
    slice(IntOffset(srcRight, 0), IntSize(imgW - srcRight, srcTop), dstLeftPx + dstMiddleW, 0f, dstRightPx, dstTopPx)
    // Middle row
    slice(IntOffset(0, srcTop), IntSize(srcLeft, srcBottom - srcTop), 0f, dstTopPx, dstLeftPx, dstMiddleH)
    slice(IntOffset(srcLeft, srcTop), IntSize(srcRight - srcLeft, srcBottom - srcTop), dstLeftPx, dstTopPx, dstMiddleW, dstMiddleH)
    slice(IntOffset(srcRight, srcTop), IntSize(imgW - srcRight, srcBottom - srcTop), dstLeftPx + dstMiddleW, dstTopPx, dstRightPx, dstMiddleH)
    // Bottom row
    slice(IntOffset(0, srcBottom), IntSize(srcLeft, imgH - srcBottom), 0f, dstTopPx + dstMiddleH, dstLeftPx, dstBottomPx)
    slice(IntOffset(srcLeft, srcBottom), IntSize(srcRight - srcLeft, imgH - srcBottom), dstLeftPx, dstTopPx + dstMiddleH, dstMiddleW, dstBottomPx)
    slice(IntOffset(srcRight, srcBottom), IntSize(imgW - srcRight, imgH - srcBottom), dstLeftPx + dstMiddleW, dstTopPx + dstMiddleH, dstRightPx, dstBottomPx)
}

@Composable
private fun SurfaceBubble(
    bg: Color,
    fg: Color,
    strokeColor: Color,
    shape: RoundedCornerShape,
    innerPadding: PaddingValues,
    style: UserBubbleStyle,
    timeText: String?,
    useOutline: Boolean,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
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

fun formatBubbleTime(t: LocalDateTime, format: UserBubbleStyle.TimeFormat): String {
    fun p(v: Int) = v.toString().padStart(2, '0')
    return when (format) {
        UserBubbleStyle.TimeFormat.HH_MM -> "${p(t.hour)}:${p(t.minute)}"
        UserBubbleStyle.TimeFormat.HH_MM_SS -> "${p(t.hour)}:${p(t.minute)}:${p(t.second)}"
        UserBubbleStyle.TimeFormat.MD_HH_MM -> "${p(t.monthNumber)}-${p(t.dayOfMonth)} ${p(t.hour)}:${p(t.minute)}"
    }
}
