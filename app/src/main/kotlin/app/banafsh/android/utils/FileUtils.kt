package app.banafsh.android.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

fun createFile(
    context: Context,
    directoryName: String,
    fileName: String,
    body: String
): File {
    val root = createDirectory(context, directoryName)
    val filePath = "$root/$fileName"
    val file = File(filePath)

    if (!file.exists()) {
        try {
            file.createNewFile()
            file.writeText(body)
        } catch (_: IOException) {
        }
    }
    return file
}

fun createDirectory(context: Context, directoryName: String): File {
    val file = File(context.getExternalFilesDir(directoryName).toString())
    if (!file.exists()) {
        file.mkdir()
    }
    return file
}

fun shareFile(context: Context, file: File, mimeType: String) {
    Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(
            Intent.EXTRA_STREAM,
            FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName,
                file
            )
        )
        context.startActivity(Intent.createChooser(this, null))
    }
}
