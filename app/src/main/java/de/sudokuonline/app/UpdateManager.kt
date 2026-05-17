package de.sudokuonline.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val versionCode: Int, val versionName: String, val apkUrl: String)

object UpdateManager {

    private const val VERSION_URL =
        "https://outblade.github.io/sudoku-online-download/version.json"

    // Returns true when installed from the Play Store — update is handled by Play Store itself.
    fun isPlayStoreInstall(context: Context): Boolean {
        val installer = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.packageManager
                    .getInstallSourceInfo(context.packageName)
                    .installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        } catch (_: Exception) { null }
        return installer == "com.android.vending"
    }

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(VERSION_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(body)
            val remoteCode = json.getInt("versionCode")
            if (remoteCode > currentVersionCode) {
                UpdateInfo(
                    versionCode = remoteCode,
                    versionName = json.getString("versionName"),
                    apkUrl = json.getString("apkUrl")
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun openDownloadPage(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW,
            Uri.parse("https://outblade.github.io/sudoku-online-download/"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
