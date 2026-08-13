package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Requests and reports go out as chat messages, not API calls — same
 * approach as the website. We build a message, copy it to the clipboard,
 * and hand off to the community WhatsApp group or Telegram channel so the
 * user can paste and send it themselves.
 */
const val WHATSAPP_GROUP_LINK = "https://chat.whatsapp.com/E4fdQ1GVJKUIQrOM1f9B6X"
const val TELEGRAM_GROUP_LINK = "https://t.me/yocinema_chat"

enum class SharePlatform { WHATSAPP, TELEGRAM }

fun copyToClipboard(context: Context, text: String): Boolean {
    return try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("YOCINEMA", text))
        clipboard != null
    } catch (e: Exception) {
        false
    }
}

/**
 * Copies [text] to the clipboard, then opens WhatsApp or Telegram directly
 * to the community chat (opens the app itself if installed, falling back
 * to the browser otherwise — same link, both cases).
 */
fun shareViaChat(context: Context, platform: SharePlatform, text: String) {
    val copied = copyToClipboard(context, text)
    val link = when (platform) {
        SharePlatform.WHATSAPP -> WHATSAPP_GROUP_LINK
        SharePlatform.TELEGRAM -> TELEGRAM_GROUP_LINK
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(context, "Couldn't open the chat — is a browser installed?", Toast.LENGTH_SHORT).show()
        return
    }
    Toast.makeText(
        context,
        if (copied) "Message copied — paste it in the chat that just opened." else "Opened chat — go ahead and describe it there.",
        Toast.LENGTH_LONG
    ).show()
}

fun buildRequestMessage(title: String, note: String?): String {
    val lines = mutableListOf("🎬 New request on YOCINEMA", "", "Title: $title")
    if (!note.isNullOrBlank()) lines.add("Details: ${note.trim()}")
    return lines.joinToString("\n")
}

fun buildReportMessage(movieTitle: String, movieId: String, reason: String?, details: String?): String {
    val lines = mutableListOf("🚩 Problem report on YOCINEMA", "", "Title: $movieTitle (id: $movieId)")
    if (!reason.isNullOrBlank()) lines.add("Issue: $reason")
    if (!details.isNullOrBlank()) lines.add("Details: ${details.trim()}")
    return lines.joinToString("\n")
}
