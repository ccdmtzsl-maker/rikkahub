package me.rerere.rikkahub.data.datastore

import kotlinx.serialization.Serializable

@Serializable
data class UserBubbleStyle(
    val background: Long = 0xFFFFF1F6,
    val textColor: Long = 0xFF554040,
    val borderColor: Long = 0xFF554040,
    val backgroundDark: Long = 0xFF3A2A32,
    val textColorDark: Long = 0xFFE8D8DC,
    val borderColorDark: Long = 0xFF6B5450,
    val cornerRadius: Float = 4f,
    val opacity: Float = 0.7f,
    val borderWidth: Float = 1f,
    val outlineOffset: Float = 2f,
    val paddingStart: Float = 8f,
    val paddingTop: Float = 10f,
    val paddingEnd: Float = 20f,
    val paddingBottom: Float = 10f,
    val marginStart: Float = 32f,
    val marginEnd: Float = 8f,
    val marginVertical: Float = 4f,
    val showTime: Boolean = false,
    val timeSize: Float = 10f,
    val timeFormat: TimeFormat = TimeFormat.HH_MM,
    val timePosition: TimePosition = TimePosition.BELOW,
) {
    @Serializable
    enum class TimeFormat { HH_MM, HH_MM_SS, MD_HH_MM }

    @Serializable
    enum class TimePosition { BELOW, INLINE }
}
