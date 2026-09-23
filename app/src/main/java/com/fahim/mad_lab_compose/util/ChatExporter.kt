package com.fahim.mad_lab_compose.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.fahim.mad_lab_compose.data.database.MessageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ChatExporter {

    /**
     * Formats conversation messages into a human-readable text document for export/sharing.
     */
    fun formatConversation(messages: List<MessageEntity>): String {
        if (messages.isEmpty()) return ""

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.append("═══════════════════════════════════════════════\n")
        sb.append("         MemoryBot Conversation Export\n")
        sb.append("         Date: $currentDate\n")
        sb.append("═══════════════════════════════════════════════\n\n")

        for (msg in messages) {
            val senderLabel = if (msg.sender == "user") "User" else "Bot"
            val msgTime = dateFormat.format(Date(msg.timestamp))

            sb.append("[$msgTime] $senderLabel:\n")
            sb.append("${msg.message.trim()}\n\n")
        }

        sb.append("───────────────────────────────────────────────\n")
        sb.append("Exported from Gemini AI Chatbot with Room Memory\n")
        return sb.toString()
    }

    /**
     * Triggers the standard Android system share sheet (ACTION_SEND)
     * completely locally without any cloud uploads.
     */
    fun shareConversation(context: Context, messages: List<MessageEntity>) {
        if (messages.isEmpty()) {
            Toast.makeText(context, "No messages to share in this conversation.", Toast.LENGTH_SHORT).show()
            return
        }

        val formattedText = formatConversation(messages)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "MemoryBot Conversation")
            putExtra(Intent.EXTRA_TEXT, formattedText)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Conversation via")
        context.startActivity(chooserIntent)
    }
}
