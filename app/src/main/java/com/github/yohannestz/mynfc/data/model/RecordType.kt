package com.github.yohannestz.mynfc.data.model

import kotlinx.serialization.Serializable

enum class RecordCategory(val title: String) {
    WEB_TEXT("Web & Text"),
    CONTACT("Contact & Business"),
    NETWORK("Network & Automation"),
    SOCIAL("Social Media"),
}

enum class FieldInput { TEXT, MULTILINE, URL, PHONE, EMAIL, DECIMAL, PASSWORD, CHOICE }

data class FieldSpec(
    val key: String,
    val label: String,
    val placeholder: String,
    val input: FieldInput = FieldInput.TEXT,
    val required: Boolean = false,
    val options: List<String> = emptyList(),
    val defaultValue: String = "",
)

@Serializable
enum class RecordType(
    val title: String,
    val description: String,
    val category: RecordCategory,
    val fields: List<FieldSpec>,
) {
    URL(
        "Web Link", "Open a website", RecordCategory.WEB_TEXT,
        listOf(FieldSpec("url", "URL", "https://example.com", FieldInput.URL, required = true)),
    ),
    TEXT(
        "Text", "Plain text note", RecordCategory.WEB_TEXT,
        listOf(
            FieldSpec("text", "Text", "Type your message…", FieldInput.MULTILINE, required = true),
            FieldSpec("lang", "Language code", "en", defaultValue = "en"),
        ),
    ),
    CONTACT(
        "Business Card", "Share a contact (vCard)", RecordCategory.CONTACT,
        listOf(
            FieldSpec("name", "Full name", "Abebe Kebede", required = true),
            FieldSpec("org", "Company", "Acme Inc."),
            FieldSpec("title", "Job title", "Product Designer"),
            FieldSpec("phone", "Phone", "+251 911 000 000", FieldInput.PHONE),
            FieldSpec("email", "Email", "name@company.com", FieldInput.EMAIL),
            FieldSpec("website", "Website", "https://company.com", FieldInput.URL),
            FieldSpec("address", "Address", "Bole Road, Addis Ababa"),
        ),
    ),
    PHONE(
        "Phone Number", "Start a call", RecordCategory.CONTACT,
        listOf(FieldSpec("number", "Phone number", "+251 911 000 000", FieldInput.PHONE, required = true)),
    ),
    EMAIL(
        "Email", "Compose an email", RecordCategory.CONTACT,
        listOf(
            FieldSpec("to", "To", "name@company.com", FieldInput.EMAIL, required = true),
            FieldSpec("subject", "Subject", "Hello!"),
            FieldSpec("body", "Message", "Write your message…", FieldInput.MULTILINE),
        ),
    ),
    SMS(
        "SMS", "Compose a text message", RecordCategory.CONTACT,
        listOf(
            FieldSpec("number", "Phone number", "+251 911 000 000", FieldInput.PHONE, required = true),
            FieldSpec("body", "Message", "Write your message…", FieldInput.MULTILINE),
        ),
    ),
    WIFI(
        "Wi-Fi", "Join a network with a tap", RecordCategory.NETWORK,
        listOf(
            FieldSpec("ssid", "Network name (SSID)", "MyHomeWiFi", required = true),
            FieldSpec(
                "auth", "Security", "", FieldInput.CHOICE,
                options = listOf("WPA/WPA2", "WPA2", "WPA", "Open"), defaultValue = "WPA/WPA2",
            ),
            FieldSpec("password", "Password", "••••••••", FieldInput.PASSWORD),
        ),
    ),
    LOCATION(
        "Location", "Open a place on the map", RecordCategory.NETWORK,
        listOf(
            FieldSpec("lat", "Latitude", "9.0108", FieldInput.DECIMAL, required = true),
            FieldSpec("lng", "Longitude", "38.7613", FieldInput.DECIMAL, required = true),
            FieldSpec("label", "Label", "Meskel Square"),
        ),
    ),
    APP(
        "Launch App", "Open an Android app", RecordCategory.NETWORK,
        listOf(FieldSpec("package", "Package name", "com.android.chrome", required = true)),
    ),
    INSTAGRAM("Instagram", "Open a profile", RecordCategory.SOCIAL, social("Username", "yourname")),
    LINKEDIN("LinkedIn", "Open a profile", RecordCategory.SOCIAL, social("Profile ID", "your-name")),
    TELEGRAM("Telegram", "Open a chat", RecordCategory.SOCIAL, social("Username", "yourname")),
    WHATSAPP(
        "WhatsApp", "Start a chat", RecordCategory.SOCIAL,
        listOf(FieldSpec("handle", "Phone (international)", "251911000000", FieldInput.PHONE, required = true)),
    ),
    YOUTUBE("YouTube", "Open a channel", RecordCategory.SOCIAL, social("Channel handle", "yourchannel")),
    X("X (Twitter)", "Open a profile", RecordCategory.SOCIAL, social("Username", "yourname")),
    FACEBOOK("Facebook", "Open a page", RecordCategory.SOCIAL, social("Page / username", "yourpage")),
    TIKTOK("TikTok", "Open a profile", RecordCategory.SOCIAL, social("Username", "yourname")),
    ;

    fun defaults(): Map<String, String> =
        fields.filter { it.defaultValue.isNotEmpty() }.associate { it.key to it.defaultValue }
}

private fun social(label: String, placeholder: String) =
    listOf(FieldSpec("handle", label, placeholder, required = true))
