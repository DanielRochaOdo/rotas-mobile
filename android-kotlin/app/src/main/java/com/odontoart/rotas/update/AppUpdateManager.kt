package com.odontoart.rotas.update

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.odontoart.rotas.BuildConfig
import kotlinx.coroutines.launch
import java.io.File

class AppUpdateManager(
    private val repository: AppUpdateRepository = AppUpdateRepository(),
) {
    private var promptedVersionCode: Int? = null
    private var pendingInstallFile: File? = null

    fun checkForUpdate(activity: ComponentActivity) {
        val metadataUrl = BuildConfig.UPDATE_METADATA_URL.trim()
        if (metadataUrl.isBlank()) return

        activity.lifecycleScope.launch {
            val update = repository.fetchUpdateInfo(
                metadataUrl = metadataUrl,
                fallbackApkUrl = BuildConfig.UPDATE_APK_URL,
            ) ?: return@launch

            if (update.versionCode <= BuildConfig.VERSION_CODE) return@launch
            if (promptedVersionCode == update.versionCode) return@launch
            if (activity.isFinishing || activity.isDestroyed) return@launch

            promptedVersionCode = update.versionCode
            showUpdateDialog(activity, update)
        }
    }

    fun resumePendingInstall(activity: ComponentActivity) {
        val pending = pendingInstallFile ?: return
        if (!pending.exists()) {
            pendingInstallFile = null
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            return
        }

        pendingInstallFile = null
        openInstaller(activity, pending)
    }

    private fun showUpdateDialog(
        activity: ComponentActivity,
        update: AppUpdateInfo,
    ) {
        val message = buildString {
            append("Nova versao disponivel: ${update.versionName}.")
            update.notes?.takeIf { it.isNotBlank() }?.let {
                append("\n\n")
                append(it)
            }
        }

        AlertDialog.Builder(activity)
            .setTitle("Atualizacao disponivel")
            .setMessage(message)
            .setNegativeButton("Depois", null)
            .setPositiveButton("Atualizar") { _, _ ->
                downloadAndInstall(activity, update)
            }
            .show()
    }

    private fun downloadAndInstall(
        activity: ComponentActivity,
        update: AppUpdateInfo,
    ) {
        Toast.makeText(activity, "Baixando atualizacao...", Toast.LENGTH_SHORT).show()

        activity.lifecycleScope.launch {
            val target = File(
                File(activity.cacheDir, "updates"),
                "odontoart-rotas-${update.versionCode}.apk",
            )

            val apk = repository.downloadApk(update.apkUrl, target)
            if (apk == null) {
                Toast.makeText(
                    activity,
                    "Nao foi possivel baixar a atualizacao.",
                    Toast.LENGTH_LONG,
                ).show()
                return@launch
            }

            installOrRequestPermission(activity, apk)
        }
    }

    private fun installOrRequestPermission(
        activity: ComponentActivity,
        apkFile: File,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            pendingInstallFile = apkFile
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}"),
            )
            activity.startActivity(settingsIntent)
            Toast.makeText(
                activity,
                "Autorize a instalacao desta fonte e volte ao Rotas.",
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        openInstaller(activity, apkFile)
    }

    private fun openInstaller(
        activity: ComponentActivity,
        apkFile: File,
    ) {
        val apkUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            apkFile,
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        runCatching {
            activity.startActivity(installIntent)
        }.onFailure {
            Toast.makeText(
                activity,
                "Nao foi possivel abrir o instalador do Android.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
