package top.cywin.onetv.tv.ui.screens.update // 定义包名

import android.content.Context // 导入Context类，用于获取应用上下文
import android.content.Intent // 导入Intent类，用于启动Activity或发送广播
import android.content.pm.PackageInfo // 导入PackageInfo类，用于获取应用的包信息
import android.os.Build // 导入Build类，提供设备硬件和版本信息
import android.provider.Settings // 导入Settings类，用于访问系统设置
import androidx.activity.compose.rememberLauncherForActivityResult // 用于创建ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts // 定义了常见的ActivityResultContract
import androidx.compose.foundation.gestures.detectTapGestures // 导入手势检测函数
import androidx.compose.runtime.Composable // 标识可组合函数
import androidx.compose.runtime.LaunchedEffect // 在组合中执行副作用操作
import androidx.compose.runtime.remember // 记住并返回一个值，以便在重组时保留状态
import androidx.compose.runtime.rememberCoroutineScope // 获取CoroutineScope实例
import androidx.compose.ui.Modifier // 修改UI组件的行为或外观
import androidx.compose.ui.input.pointer.pointerInput // 处理指针输入事件
import androidx.compose.ui.platform.LocalContext // 提供当前组合中的Context
import androidx.lifecycle.viewmodel.compose.viewModel // 提供ViewModel实例
import kotlinx.coroutines.Dispatchers // 提供协程调度器
import kotlinx.coroutines.delay // 挂起协程一段时间
import kotlinx.coroutines.launch // 启动新的协程
import top.cywin.onetv.core.data.utils.Globals // 包含全局变量的工具类
import top.cywin.onetv.core.util.utils.ApkInstaller // APK安装工具类
import top.cywin.onetv.tv.ui.material.PopupContent // 弹出内容组件
import top.cywin.onetv.tv.ui.material.Snackbar // 短暂消息提示组件
import top.cywin.onetv.tv.ui.material.SnackbarType // 消息提示类型枚举
import top.cywin.onetv.tv.ui.screens.settings.SettingsViewModel // 设置界面的ViewModel
import top.cywin.onetv.tv.ui.screens.update.components.UpdateContent // 更新界面的内容组件
import top.cywin.onetv.tv.ui.utils.captureBackKey // 捕捉返回键事件的扩展函数
import java.io.File // 文件操作类

// 使用@Composable注解的函数UpdateScreen，构建更新界面
@Composable
fun UpdateScreen(
    modifier: Modifier = Modifier, // 通过参数传递Modifier，默认值为Modifier
    settingsViewModel: SettingsViewModel = viewModel(), // 获取SettingsViewModel实例
    updateViewModel: UpdateViewModel = viewModel(), // 获取UpdateViewModel实例
) {
    val context = LocalContext.current // 获取当前组合中的Context
    val coroutineScope = rememberCoroutineScope() // 创建并记住一个CoroutineScope
    val packageInfo = rememberPackageInfo() // 记住并获取当前应用的PackageInfo
    val latestFile = remember { File(Globals.cacheDir, "latest.apk") } // 记住最新APK文件的路径

    // 使用LaunchedEffect监听Unit的变化，延迟3秒后检查更新
    LaunchedEffect(Unit) {
        delay(3000L) // 延迟3秒
        updateViewModel.checkUpdate(packageInfo.versionName, settingsViewModel.updateChannel) // 检查是否有可用更新

        val latestRelease = updateViewModel.latestRelease // 获取最新的发布信息
        if (
            updateViewModel.isUpdateAvailable && // 如果有可用更新
            latestRelease.version != settingsViewModel.appLastLatestVersion // 并且不是已经提醒过的版本
        ) {
            settingsViewModel.appLastLatestVersion = latestRelease.version // 更新最后提醒的版本号

            if (settingsViewModel.updateForceRemind) { // 如果强制提醒更新
                updateViewModel.visible = true // 显示更新弹窗
            } else {
                Snackbar.show("发现新版本: v${latestRelease.version}") // 显示短暂的消息提示
            }
        }
    }

    // 创建ActivityResultLauncher，用于处理ActivityResult
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // 如果设备运行的是Android O及以上版本
            if (context.packageManager.canRequestPackageInstalls()) { // 如果可以请求安装未知来源的应用
                ApkInstaller.installApk(context, latestFile.path) // 安装APK
            } else {
                Snackbar.show("未授予安装权限", type = SnackbarType.ERROR) // 显示错误消息提示
            }
        }
    }

    // 监听updateViewModel.updateDownloaded的变化，如果下载完成则尝试安装APK
    LaunchedEffect(updateViewModel.updateDownloaded) {
        if (!updateViewModel.updateDownloaded) return@LaunchedEffect // 如果还没有下载完成，则退出

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { // 如果设备运行的是Android O以下版本
            ApkInstaller.installApk(context, latestFile.path) // 安装APK
        } else {
            if (context.packageManager.canRequestPackageInstalls()) { // 如果可以请求安装未知来源的应用
                ApkInstaller.installApk(context, latestFile.path) // 安装APK
            } else {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES) // 创建意图打开设置页面以管理未知来源的应用
                    launcher.launch(intent) // 发起活动结果请求
                } catch (_: Exception) { // 捕获可能的异常
                    Snackbar.show(
                        "无法找到相应的设置项，请手动启用未知来源安装权限。",
                        type = SnackbarType.ERROR,
                    ) // 显示错误消息提示
                }
            }
        }
    }

    // 显示更新弹窗内容
    PopupContent(
        visibleProvider = { updateViewModel.visible }, // 控制弹窗是否可见
        onDismissRequest = { updateViewModel.visible = false }, // 当请求关闭弹窗时调用
    ) {
        UpdateContent(
            modifier = modifier
                .captureBackKey { updateViewModel.visible = false } // 捕捉返回键事件
                .pointerInput(Unit) { detectTapGestures { } }, // 处理点击手势
            onDismissRequest = { updateViewModel.visible = false }, // 请求关闭弹窗时调用
            releaseProvider = { updateViewModel.latestRelease }, // 提供最新的发布信息
            isUpdateAvailableProvider = { updateViewModel.isUpdateAvailable }, // 提供是否有可用更新的信息
            onUpdateAndInstall = {
                updateViewModel.visible = false // 隐藏更新弹窗
                coroutineScope.launch(Dispatchers.IO) { // 在IO线程中异步下载并更新
                    updateViewModel.downloadAndUpdate(latestFile)
                }
            },
        )
    }
}

// Composable函数rememberPackageInfo，用于获取并记住当前应用的PackageInfo
@Composable
private fun rememberPackageInfo(context: Context = LocalContext.current): PackageInfo =
    context.packageManager.getPackageInfo(context.packageName, 0) // 获取应用的包信息