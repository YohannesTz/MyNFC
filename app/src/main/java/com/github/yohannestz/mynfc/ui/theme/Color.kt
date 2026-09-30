package com.github.yohannestz.mynfc.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Blue = Color(0xFF2D7CF6)
val Sky = Color(0xFF36A9FF)
val Violet = Color(0xFF7B61FF)
val Orange = Color(0xFFFF7A18)
val Amber = Color(0xFFFFB347)
val Emerald = Color(0xFF10B981)
val Teal = Color(0xFF0FA3B1)
val Pink = Color(0xFFFF4D8D)
val Purple = Color(0xFFA855F7)
val Red = Color(0xFFEF4444)
val Indigo = Color(0xFF6366F1)

object Gradients {
    val read = Brush.linearGradient(listOf(Sky, Blue, Violet))
    val write = Brush.linearGradient(listOf(Orange, Color(0xFFFF9A3D), Amber))
    val saved = Brush.linearGradient(listOf(Emerald, Teal))
    val tools = Brush.linearGradient(listOf(Pink, Purple))
    val danger = Brush.linearGradient(listOf(Color(0xFFFF5F6D), Red))
    val rfid = Brush.linearGradient(listOf(Indigo, Color(0xFF7C3AED), Color(0xFF3B0764)))
}
