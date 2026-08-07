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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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

private const val IMESSAGE_BUBBLE_URL = "https://imgbed.heliar.top/i/a84EPVMPta7xHRe6.webp"

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
                .drawBehind {
                    val img = bitmap ?: return@drawBehind
                    drawNinePatch(
                        image = img,
                        dstSize = size,
                        sliceHStart = 0.20f,
                        sliceHEnd = 0.87f,
                        sliceVStart = 0.45f,
                        sliceVEnd = 0.46f,
                        colorFilter = colorFilter,
                    )
                }
                .padding(
                    start = 18.dp,
                    top = style.paddingTop.dp,
                    end = 23.dp,
                    bottom = style.paddingBottom.dp,
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

private fun DrawScope.drawNinePatch(
    image: ImageBitmap,
    dstSize: Size,
    sliceHStart: Float,
    sliceHEnd: Float,
    sliceVStart: Float,
    sliceVEnd: Float,
    colorFilter: ColorFilter,
) {
    val imgW = image.width
    val imgH = image.height
    val srcLeft = (imgW * sliceHStart).toInt()
    val srcRight = (imgW * sliceHEnd).toInt()
    val srcTop = (imgH * sliceVStart).toInt()
    val srcBottom = (imgH * sliceVEnd).toInt()

    val dstW = dstSize.width
    val dstH = dstSize.height
    val dstLeft = srcLeft.toFloat()
    val dstRight = (imgW - srcRight).toFloat()
    val dstTop = srcTop.toFloat()
    val dstBottom = (imgH - srcBottom).toFloat()
    val dstMiddleW = (dstW - dstLeft - dstRight).coerceAtLeast(0f)
    val dstMiddleH = (dstH - dstTop - dstBottom).coerceAtLeast(0f)

    // 9 slices: TL, TC, TR, ML, MC, MR, BL, BC, BR
    fun slice(srcOff: IntOffset, srcSz: IntSize, dstOff: Offset, dstSz: Size) {
        if (srcSz.width <= 0 || srcSz.height <= 0 || dstSz.width <= 0f || dstSz.height <= 0f) return
        drawImage(
            image = image,
            srcOffset = srcOff,
            srcSize = srcSz,
            dstOffset = dstOff,
            dstSize = dstSz,
            colorFilter = colorFilter,
        )
    }
    // Top row
    slice(IntOffset(0, 0), IntSize(srcLeft, srcTop), Offset(0f, 0f), Size(dstLeft, dstTop))
    slice(IntOffset(srcLeft, 0), IntSize(srcRight - srcLeft, srcTop), Offset(dstLeft, 0f), Size(dstMiddleW, dstTop))
    slice(IntOffset(srcRight, 0), IntSize(imgW - srcRight, srcTop), Offset(dstLeft + dstMiddleW, 0f), Size(dstRight, dstTop))
    // Middle row
    slice(IntOffset(0, srcTop), IntSize(srcLeft, srcBottom - srcTop), Offset(0f, dstTop), Size(dstLeft, dstMiddleH))
    slice(IntOffset(srcLeft, srcTop), IntSize(srcRight - srcLeft, srcBottom - srcTop), Offset(dstLeft, dstTop), Size(dstMiddleW, dstMiddleH))
    slice(IntOffset(srcRight, srcTop), IntSize(imgW - srcRight, srcBottom - srcTop), Offset(dstLeft + dstMiddleW, dstTop), Size(dstRight, dstMiddleH))
    // Bottom row
    slice(IntOffset(0, srcBottom), IntSize(srcLeft, imgH - srcBottom), Offset(0f, dstTop + dstMiddleH), Size(dstLeft, dstBottom))
    slice(IntOffset(srcLeft, srcBottom), IntSize(srcRight - srcLeft, imgH - srcBottom), Offset(dstLeft, dstTop + dstMiddleH), Size(dstMiddleW, dstBottom))
    slice(IntOffset(srcRight, srcBottom), IntSize(imgW - srcRight, imgH - srcBottom), Offset(dstLeft + dstMiddleW, dstTop + dstMiddleH), Size(dstRight, dstBottom))
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
