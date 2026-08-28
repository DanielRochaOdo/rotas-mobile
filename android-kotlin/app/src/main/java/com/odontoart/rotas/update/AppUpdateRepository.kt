package com.odontoart.rotas.update

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URI

class AppUpdateRepository(
    private val client: OkHttpClient = OkHttpClient(),
) {
    suspend fun fetchUpdateInfo(
        metadataUrl: String,
        fallbackApkUrl: String,
    ): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val resolvedMetadataUrl = metadataUrl.trim()
        if (!isValidHttpsUrl(resolvedMetadataUrl)) {
            Log.e(TAG, "URL de metadata de atualizacao ausente ou invalida.")
            return@withContext null
        }

        runCatching {
            val request = Request.Builder()
                .url(withCacheBuster(resolvedMetadataUrl))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) {
                    "Servidor de atualizacao respondeu HTTP ${response.code}."
                }

                val raw = response.body?.string()?.trim().orEmpty()
                check(raw.startsWith("{") && raw.endsWith("}")) {
                    "Metadata de atualizacao nao contem JSON valido."
                }

                val json = JSONObject(raw)
                val versionCode = json.optInt("versionCode", 0)
                val versionName = json.optString("versionName").trim()
                val apkUrl = json.optString("apkUrl")
                    .trim()
                    .ifBlank { fallbackApkUrl.trim() }
                val notes = json.optString("notes")
                    .trim()
                    .takeIf { it.isNotBlank() }

                check(versionCode > 0) { "versionCode invalido na metadata de atualizacao." }
                check(versionName.isNotBlank()) { "versionName ausente na metadata de atualizacao." }
                check(isValidHttpsUrl(apkUrl)) { "apkUrl ausente ou invalida na metadata de atualizacao." }

                AppUpdateInfo(
                    versionCode = versionCode,
                    versionName = versionName,
                    apkUrl = apkUrl,
                    notes = notes,
                )
            }
        }.onFailure { throwable ->
            Log.e(TAG, "Falha ao consultar atualizacao: ${throwable.message}", throwable)
        }.getOrNull()
    }

    suspend fun downloadApk(
        apkUrl: String,
        target: File,
    ): File? = withContext(Dispatchers.IO) {
        val resolvedUrl = apkUrl.trim()
        if (!isValidHttpsUrl(resolvedUrl)) {
            Log.e(TAG, "URL do APK de atualizacao ausente ou invalida.")
            return@withContext null
        }

        runCatching {
            val request = Request.Builder()
                .url(withCacheBuster(resolvedUrl))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) {
                    "Download do APK respondeu HTTP ${response.code}."
                }

                val body = response.body ?: error("Servidor nao retornou o APK.")
                target.parentFile?.mkdirs()
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            check(target.length() >= MIN_APK_SIZE_BYTES) {
                "Arquivo baixado e muito pequeno para ser um APK valido."
            }

            target.inputStream().use { input ->
                val first = input.read()
                val second = input.read()
                check(first == ZIP_MAGIC_P && second == ZIP_MAGIC_K) {
                    "Servidor retornou um arquivo que nao parece ser um APK."
                }
            }

            target
        }.onFailure { throwable ->
            Log.e(TAG, "Falha ao baixar APK: ${throwable.message}", throwable)
            runCatching { if (target.exists()) target.delete() }
        }.getOrNull()
    }

    private fun withCacheBuster(rawUrl: String): String {
        val separator = if (rawUrl.contains('?')) '&' else '?'
        return "$rawUrl${separator}ts=${System.currentTimeMillis()}"
    }

    private fun isValidHttpsUrl(rawUrl: String): Boolean {
        val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }

    private companion object {
        const val TAG = "AppUpdateRepository"
        const val MIN_APK_SIZE_BYTES = 16 * 1024L
        const val ZIP_MAGIC_P = 'P'.code
        const val ZIP_MAGIC_K = 'K'.code
    }
}
