package com.github.yohannestz.mynfc.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.yohannestz.mynfc.data.model.RecordKind
import com.github.yohannestz.mynfc.data.model.RecordType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val RecordType.icon: ImageVector
    get() = when (this) {
        RecordType.URL -> Icons.Rounded.Language
        RecordType.TEXT -> Icons.AutoMirrored.Rounded.Notes
        RecordType.CONTACT -> Icons.Rounded.Badge
        RecordType.PHONE -> Icons.Rounded.Call
        RecordType.EMAIL -> Icons.Rounded.Email
        RecordType.SMS -> Icons.AutoMirrored.Rounded.Message
        RecordType.WIFI -> Icons.Rounded.Wifi
        RecordType.LOCATION -> Icons.Rounded.LocationOn
        RecordType.APP -> Icons.Rounded.Android
        RecordType.INSTAGRAM -> Icons.Rounded.CameraAlt
        RecordType.LINKEDIN -> Icons.Rounded.Work
        RecordType.TELEGRAM -> Icons.AutoMirrored.Rounded.Send
        RecordType.WHATSAPP -> Icons.Rounded.Chat
        RecordType.YOUTUBE -> Icons.Rounded.PlayCircle
        RecordType.X -> Icons.Rounded.AlternateEmail
        RecordType.FACEBOOK -> Icons.Rounded.ThumbUp
        RecordType.TIKTOK -> Icons.Rounded.MusicNote
    }

val RecordType.color: Color
    get() = when (this) {
        RecordType.URL -> Color(0xFF6366F1)
        RecordType.TEXT -> Color(0xFF8B5CF6)
        RecordType.CONTACT -> Color(0xFF0EA5E9)
        RecordType.PHONE -> Color(0xFF22A06B)
        RecordType.EMAIL -> Color(0xFF2D7CF6)
        RecordType.SMS -> Color(0xFF06B6D4)
        RecordType.WIFI -> Color(0xFFFF7A18)
        RecordType.LOCATION -> Color(0xFFF97316)
        RecordType.APP -> Color(0xFF3DDC84)
        RecordType.INSTAGRAM -> Color(0xFFE1306C)
        RecordType.LINKEDIN -> Color(0xFF0A66C2)
        RecordType.TELEGRAM -> Color(0xFF229ED9)
        RecordType.WHATSAPP -> Color(0xFF25D366)
        RecordType.YOUTUBE -> Color(0xFFFF0033)
        RecordType.X -> Color(0xFF64748B)
        RecordType.FACEBOOK -> Color(0xFF1877F2)
        RecordType.TIKTOK -> Color(0xFFEE1D52)
    }

val RecordKind.icon: ImageVector
    get() = when (this) {
        RecordKind.URL -> Icons.Rounded.Language
        RecordKind.TEXT -> Icons.AutoMirrored.Rounded.Notes
        RecordKind.PHONE -> Icons.Rounded.Call
        RecordKind.EMAIL -> Icons.Rounded.Email
        RecordKind.SMS -> Icons.AutoMirrored.Rounded.Message
        RecordKind.GEO -> Icons.Rounded.LocationOn
        RecordKind.WIFI -> Icons.Rounded.Wifi
        RecordKind.CONTACT -> Icons.Rounded.Badge
        RecordKind.APP -> Icons.Rounded.Android
        RecordKind.SMART_POSTER -> Icons.Rounded.Public
        RecordKind.MIME -> Icons.Rounded.Code
        RecordKind.EXTERNAL -> Icons.Rounded.Extension
        RecordKind.EMPTY -> Icons.Rounded.CropFree
        RecordKind.UNKNOWN -> Icons.Rounded.HelpOutline
    }

val RecordKind.color: Color
    get() = when (this) {
        RecordKind.URL, RecordKind.SMART_POSTER -> Color(0xFF6366F1)
        RecordKind.TEXT -> Color(0xFF8B5CF6)
        RecordKind.PHONE -> Color(0xFF22A06B)
        RecordKind.EMAIL -> Color(0xFF2D7CF6)
        RecordKind.SMS -> Color(0xFF06B6D4)
        RecordKind.GEO -> Color(0xFFF97316)
        RecordKind.WIFI -> Color(0xFFFF7A18)
        RecordKind.CONTACT -> Color(0xFF0EA5E9)
        RecordKind.APP -> Color(0xFF3DDC84)
        RecordKind.MIME, RecordKind.EXTERNAL -> Color(0xFF64748B)
        RecordKind.EMPTY, RecordKind.UNKNOWN -> Color(0xFF9CA3AF)
    }

private val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
private val fileDateFormat = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US)

fun formatDate(millis: Long): String = synchronized(dateFormat) { dateFormat.format(Date(millis)) }
fun fileStamp(millis: Long = System.currentTimeMillis()): String = synchronized(fileDateFormat) { fileDateFormat.format(Date(millis)) }

fun relativeTime(millis: Long, now: Long = System.currentTimeMillis()): String {
    val diff = (now - millis) / 1000
    return when {
        diff < 60 -> "Just now"
        diff < 3600 -> "${diff / 60} min ago"
        diff < 86_400 -> "${diff / 3600} h ago"
        diff < 7 * 86_400 -> "${diff / 86_400} d ago"
        else -> formatDate(millis)
    }
}
