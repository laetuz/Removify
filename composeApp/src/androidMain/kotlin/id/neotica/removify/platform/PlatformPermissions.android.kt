package id.neotica.removify.platform

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

actual fun hasMediaPermission(): Boolean {
    val context = AndroidAppContext.appContext ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
    } else {
        context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

actual suspend fun requestMediaPermission(): Boolean = hasMediaPermission()
