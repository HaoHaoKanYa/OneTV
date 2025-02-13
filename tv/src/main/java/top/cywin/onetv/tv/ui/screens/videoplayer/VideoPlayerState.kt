package top.cywin.onetv.tv.ui.screens.videoplayer

import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import top.cywin.onetv.tv.ui.screens.videoplayer.player.Media3VideoPlayer
import top.cywin.onetv.tv.ui.screens.videoplayer.player.VideoPlayer

@Stable
class VideoPlayerState(
    private val instance: VideoPlayer, // 视频播放器实例
    private var defaultDisplayModeProvider: () -> VideoPlayerDisplayMode = { VideoPlayerDisplayMode.ORIGINAL }, // 默认显示模式提供者
) {
    /** 显示模式 */
    var displayMode by mutableStateOf(defaultDisplayModeProvider()) // 当前显示模式

    /** 视频宽高比 */
    var aspectRatio by mutableFloatStateOf(16f / 9f) // 当前视频的宽高比，默认 16:9

    /** 错误 */
    var error by mutableStateOf<String?>(null) // 存储错误信息

    /** 正在缓冲 */
    var isBuffering by mutableStateOf(false) // 视频是否正在缓冲

    /** 正在播放 */
    var isPlaying by mutableStateOf(false) // 视频是否正在播放

    /** 总时长 */
    var duration by mutableLongStateOf(0L) // 视频总时长

    /** 当前播放位置 */
    var currentPosition by mutableLongStateOf(0L) // 当前播放进度

    /** 元数据 */
    var metadata by mutableStateOf(VideoPlayer.Metadata()) // 当前视频元数据（如标题、作者等）

    // 初始化视频播放器
    fun prepare(url: String) {
        error = null // 清空错误
        instance.prepare(url) // 准备视频播放
    }

    // 播放视频
    fun play() {
        instance.play() // 调用播放器的播放方法
    }

    // 暂停视频
    fun pause() {
        instance.pause() // 调用播放器的暂停方法
    }

    // 跳转到指定播放位置
    fun seekTo(position: Long) {
        instance.seekTo(position) // 调用播放器的跳转方法
    }

    // 停止播放
    fun stop() {
        instance.stop() // 调用播放器的停止方法
    }

    // 设置视频的 SurfaceView
    fun setVideoSurfaceView(surfaceView: SurfaceView) {
        instance.setVideoSurfaceView(surfaceView) // 绑定 SurfaceView 用于显示视频
    }

    // 保存播放器事件的监听器
    private val onReadyListeners = mutableListOf<() -> Unit>()
    private val onErrorListeners = mutableListOf<() -> Unit>()
    private val onInterruptListeners = mutableListOf<() -> Unit>()

    // 注册播放器准备好的监听器
    fun onReady(listener: () -> Unit) {
        onReadyListeners.add(listener)
    }

    // 注册播放器错误的监听器
    fun onError(listener: () -> Unit) {
        onErrorListeners.add(listener)
    }

    // 注册播放器中断的监听器
    fun onInterrupt(listener: () -> Unit) {
        onInterruptListeners.add(listener)
    }

    // 初始化播放器，绑定各种事件监听器
    fun initialize() {
        instance.initialize() // 初始化播放器实例
        instance.onResolution { width, height ->
            if (width > 0 && height > 0) aspectRatio = width.toFloat() / height // 设置视频的宽高比
        }
        instance.onError { ex ->
            error = ex?.let { "${it.errorCodeName}(${it.errorCode})" } // 发生错误时记录错误信息
                ?.apply { onErrorListeners.forEach { it.invoke() } } // 执行错误监听器
        }
        instance.onReady {
            onReadyListeners.forEach { it.invoke() } // 播放器准备好时执行监听器
            error = null // 清除错误信息
            displayMode = defaultDisplayModeProvider() // 设置默认显示模式
        }
        instance.onBuffering {
            isBuffering = it // 设置缓冲状态
            if (it) error = null // 如果正在缓冲，清除错误信息
        }
        instance.onPrepared { } // 视频准备完成时的回调
        instance.onIsPlayingChanged { isPlaying = it } // 播放状态改变时的回调
        instance.onDurationChanged { duration = it } // 视频时长改变时的回调
        instance.onCurrentPositionChanged { currentPosition = it } // 当前播放位置改变时的回调
        instance.onMetadata { metadata = it } // 元数据更新时的回调
        instance.onInterrupt { onInterruptListeners.forEach { it.invoke() } } // 播放中断时的回调
    }

    // 释放播放器资源
    fun release() {
        onReadyListeners.clear() // 清除准备好监听器
        onErrorListeners.clear() // 清除错误监听器
        instance.release() // 释放播放器资源
    }
}

@Composable
fun rememberVideoPlayerState(
    defaultDisplayModeProvider: () -> VideoPlayerDisplayMode = { VideoPlayerDisplayMode.ORIGINAL }, // 默认显示模式提供者
): VideoPlayerState {
    val context = LocalContext.current // 获取当前上下文
    val lifecycleOwner = LocalLifecycleOwner.current // 获取生命周期所有者
    val coroutineScope = rememberCoroutineScope() // 创建协程作用域
    val state = remember {
        VideoPlayerState(
            Media3VideoPlayer(context, coroutineScope), // 使用 Media3VideoPlayer 创建播放器实例
            defaultDisplayModeProvider, // 传入默认显示模式提供者
        )
    }

    // 初始化播放器状态
    DisposableEffect(Unit) {
        state.initialize() // 初始化播放器
        onDispose { state.release() } // 组件销毁时释放资源
    }

    // 监听生命周期事件，播放和暂停控制
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state.play() // 在活动恢复时播放
            else if (event == Lifecycle.Event.ON_STOP) state.pause() // 在活动停止时暂停
        }
        lifecycleOwner.lifecycle.addObserver(observer) // 添加生命周期观察者
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) } // 组件销毁时移除观察者
    }

    return state // 返回视频播放器状态
}

// 视频播放器显示模式
enum class VideoPlayerDisplayMode(
    val label: String, // 显示模式标签
    val value: Int, // 显示模式值
) {
    /** 原始 */
    ORIGINAL("原始", 0), // 保持原始宽高比

    /** 填充 */
    FILL("填充", 1), // 填充整个容器

    /** 裁剪 */
    CROP("裁剪", 2), // 裁剪多余部分

    /** 4:3 */
    FOUR_THREE("4:3", 3), // 4:3 比例

    /** 16:9 */
    SIXTEEN_NINE("16:9", 4), // 16:9 比例

    /** 2.35:1 */
    WIDE("2.35:1", 5); // 宽屏比例

    companion object {
        // 根据值获取对应的显示模式
        fun fromValue(value: Int): VideoPlayerDisplayMode {
            return entries.firstOrNull { it.value == value } ?: ORIGINAL // 如果找不到，则返回默认的原始模式
        }
    }
}
