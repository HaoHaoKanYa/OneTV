package top.cywin.onetv.tv.ui.screens.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.cywin.onetv.core.data.utils.Globals
import top.cywin.onetv.core.util.utils.ApkInstaller
import top.cywin.onetv.tv.ui.material.PopupContent
import top.cywin.onetv.tv.ui.material.Snackbar
import top.cywin.onetv.tv.ui.material.SnackbarType
import top.cywin.onetv.tv.ui.screens.settings.SettingsViewModel
import top.cywin.onetv.tv.ui.screens.update.components.UpdateContent
import top.cywin.onetv.tv.ui.utils.captureBackKey
import java.io.File

@Composable
fun UpdateScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = viewModel(),
    updateViewModel: UpdateViewModel = viewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val packageInfo = rememberPackageInfo()

    // 启动时进行更新检查
    LaunchedEffect(Unit) {
        delay(3000)
        updateViewModel.checkUpdate(packageInfo.versionName, settingsViewModel.updateChannel)
    }

    val latestRelease = updateViewModel.latestRelease
    val isUpdateAvailable = updateViewModel.isUpdateAvailable

    // 如果有更新，显示更新提示
    if (isUpdateAvailable) {
        updateViewModel.visible = true
    }

    // 处理 APK 安装逻辑
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context.packageManager.canRequestPackageInstalls()) {
                ApkInstaller.installApk(context, File(Globals.cacheDir, "latest.apk").path)
            } else {
                Snackbar.show("未授予安装权限", type = SnackbarType.ERROR)
            }
        }
    }

    // 处理更新对话框的显示和操作
    PopupContent(
        visibleProvider = { updateViewModel.visible },
        onDismissRequest = { updateViewModel.visible = false },
    ) {
        UpdateContent(
            modifier = modifier,
            onDismissRequest = { updateViewModel.visible = false },
            releaseProvider = { updateViewModel.latestRelease },
            isUpdateAvailableProvider = { updateViewModel.isUpdateAvailable },
            onUpdateAndInstall = {
                updateViewModel.visible = false
                coroutineScope.launch(Dispatchers.IO) {
                    updateViewModel.downloadAndUpdate(File(Globals.cacheDir, "latest.apk"))
                }
            },
        )
    }
}

@Composable
private fun rememberPackageInfo(context: Context = LocalContext.current): PackageInfo =
    context.packageManager.getPackageInfo(context.packageName, 0)
