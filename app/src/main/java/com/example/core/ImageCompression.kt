package com.example.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

object ImageCompression {
  fun createTempCameraUri(context: Context): Uri {
    val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
    val photoFile = File(cameraDir, "toto_capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
  }

  fun compressUriToBase64(context: Context, uri: Uri, maxDimension: Int = 480, quality: Int = 60): String? {
    return try {
      val inputStream = context.contentResolver.openInputStream(uri) ?: return null
      val original = inputStream.use { BitmapFactory.decodeStream(it) } ?: return null
      val scale = max(original.width, original.height).toFloat() / maxDimension.toFloat()
      val scaled =
        if (scale > 1f) {
          Bitmap.createScaledBitmap(
            original,
            (original.width / scale).toInt().coerceAtLeast(1),
            (original.height / scale).toInt().coerceAtLeast(1),
            true,
          )
        } else {
          original
        }
      val baos = ByteArrayOutputStream()
      scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
      if (scaled !== original) scaled.recycle()
      original.recycle()
      Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    } catch (_: Exception) {
      null
    }
  }

  fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
    if (base64.isNullOrBlank()) return null
    return try {
      val bytes = Base64.decode(base64, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
      null
    }
  }
}
