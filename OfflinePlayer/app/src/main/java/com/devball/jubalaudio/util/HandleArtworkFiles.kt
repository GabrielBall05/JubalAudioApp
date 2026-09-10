package com.devball.jubalaudio.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

fun extractArt(context: Context, retriever: MediaMetadataRetriever): String? {
    //Get embedded picture, return null if there isn't one to get
    val artBytes = retriever.embeddedPicture ?: return null

    //Create a unique filename for the image (UUID)
    val fileName = generateUniqueFileName()

    //Ensure directory exists, create it if not
    val directory = ensureDirectory(context)

    val outputFile = File(directory, fileName)

    //Write bytes to file and return path
    return try {
        FileOutputStream(outputFile).use { it.write(artBytes) }
        outputFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
    //Create a unique filename for the image (UUID)
    val fileName = generateUniqueFileName()

    //Ensure directory exists, create it if not
    val directory = ensureDirectory(context)

    val outputFile = File(directory, fileName)

    //Open temporary content URI and copy to new permanent file
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        outputFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun generateUniqueFileName(): String {
    val uniqueId = UUID.randomUUID().toString()
    return "art_$uniqueId.jpg"
}

private fun ensureDirectory(context: Context, name: String = "media_art"): File {
    return File(context.filesDir, name).apply {
        if (!exists()) mkdirs()
    }
}