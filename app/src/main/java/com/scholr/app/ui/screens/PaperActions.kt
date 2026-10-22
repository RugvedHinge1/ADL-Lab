package com.scholr.app.ui.screens

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.scholr.app.data.model.Paper

/**
 * "Read full text": opens the free PDF when there is one, otherwise the paper's
 * web page (publisher / Semantic Scholar / OpenAlex) so the button is never a dead end.
 */
fun openFullText(context: Context, paper: Paper) {
    val url = paper.pdfUrl ?: paper.webUrl
    if (url == null) {
        Toast.makeText(context, "No link to the full text is available for this paper.", Toast.LENGTH_LONG).show()
        return
    }
    if (paper.pdfUrl == null) {
        Toast.makeText(context, "No free PDF found - opening the paper's page instead.", Toast.LENGTH_LONG).show()
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this link.", Toast.LENGTH_LONG).show()
    }
}

/** Downloads the free PDF into the phone's Downloads folder with the system download notification. */
fun downloadPdf(context: Context, paper: Paper) {
    val url = paper.pdfUrl
    if (url == null) {
        Toast.makeText(
            context,
            "This paper has no free PDF to download. Use the PDF button to open its page.",
            Toast.LENGTH_LONG
        ).show()
        return
    }
    val fileName = paper.title.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().take(60)
        .ifBlank { "paper" }.replace(' ', '_') + ".pdf"
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(paper.title)
            .setDescription("Scholr")
            .setMimeType("application/pdf")
            .addRequestHeader("User-Agent", "Mozilla/5.0 (Android) Scholr/1.0")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        context.getSystemService(DownloadManager::class.java).enqueue(request)
        Toast.makeText(context, "Downloading PDF to your Downloads folder...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Couldn't start the download.", Toast.LENGTH_LONG).show()
    }
}
