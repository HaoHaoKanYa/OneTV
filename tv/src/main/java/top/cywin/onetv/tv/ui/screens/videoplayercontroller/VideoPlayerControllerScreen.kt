package top.cywin.onetv.tv.ui.screens.videoplayercontroller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import top.cywin.onetv.tv.ui.material.Drawer
import top.cywin.onetv.tv.ui.material.DrawerPosition
import top.cywin.onetv.tv.ui.screens.videoplayercontroller.components.VideoPlayerControllerPositionCtrl
import top.cywin.onetv.tv.ui.screens.videoplayercontroller.components.VideoPlayerControllerStateCtrl
import top.cywin.onetv.tv.ui.theme.MyTVTheme
import top.cywin.onetv.tv.ui.tooling.PreviewWithLayoutGrids
import top.cywin.onetv.tv.ui.utils.captureBackKey
import top.cywin.onetv.tv.ui.utils.focusOnLaunchedSaveable

@Composable
fun VideoPlayerControllerScreen(
    modifier: Modifier = Modifier,
    isVideoPlayerPlayingProvider: () -> Boolean = { false },
    isVideoPlayerBufferingProvider: () -> Boolean = { false },
    videoPlayerCurrentPositionProvider: () -> Long = { 0L },
    videoPlayerDurationProvider: () -> Pair<Long, Long> = { 0L to 0L },
    onVideoPlayerPlay: () -> Unit = {},
    onVideoPlayerPause: () -> Unit = {},
    onVideoPlayerSeekTo: (Long) -> Unit = {},
    onClose: () -> Unit = {},
) {
    Drawer(
        modifier = modifier.captureBackKey { onClose() },
        onDismissRequest = onClose,
        position = DrawerPosition.Bottom,
        header = { Text("播放控制") },
    ) {
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VideoPlayerControllerStateCtrl(
                modifier = Modifier.focusOnLaunchedSaveable(),
                isPlayingProvider = isVideoPlayerPlayingProvider,
                isBufferingProvider = isVideoPlayerBufferingProvider,
                onPlay = onVideoPlayerPlay,
                onPause = onVideoPlayerPause,
            )

            VideoPlayerControllerPositionCtrl(
                currentPositionProvider = videoPlayerCurrentPositionProvider,
                durationProvider = videoPlayerDurationProvider,
                onSeekTo = onVideoPlayerSeekTo,
            )
        }
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun VideoPlayerControllerScreenPreview() {
    MyTVTheme {
        PreviewWithLayoutGrids {
            VideoPlayerControllerScreen(
                videoPlayerCurrentPositionProvider = { System.currentTimeMillis() },
                videoPlayerDurationProvider = {
                    System.currentTimeMillis() - 1000L * 60 * 60 to System.currentTimeMillis() + 1000L * 60 * 60
                },
            )
        }
    }
}