package top.cywin.onetv.tv.ui.screens.videoplayerdiaplaymode

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import kotlinx.collections.immutable.toPersistentList
import top.cywin.onetv.tv.ui.material.Drawer
import top.cywin.onetv.tv.ui.material.DrawerPosition
import top.cywin.onetv.tv.ui.screens.components.rememberScreenAutoCloseState
import top.cywin.onetv.tv.ui.screens.videoplayer.VideoPlayerDisplayMode
import top.cywin.onetv.tv.ui.screens.videoplayerdiaplaymode.components.VideoPlayerDisplayModeItemList
import top.cywin.onetv.tv.ui.theme.MyTVTheme
import top.cywin.onetv.tv.ui.tooling.PreviewWithLayoutGrids
import top.cywin.onetv.tv.ui.utils.captureBackKey

@Composable
fun VideoPlayerDisplayModeScreen(
    modifier: Modifier = Modifier,
    currentDisplayModeProvider: () -> VideoPlayerDisplayMode = { VideoPlayerDisplayMode.ORIGINAL },
    onDisplayModeChanged: (VideoPlayerDisplayMode) -> Unit = {},
    onApplyToGlobal: (() -> Unit)? = null,
    onClose: () -> Unit = {},
) {
    val screenAutoCloseState = rememberScreenAutoCloseState(onTimeout = onClose)

    Drawer(
        modifier = modifier.captureBackKey { onClose() },
        onDismissRequest = onClose,
        position = DrawerPosition.End,
        header = { Text("显示模式") },
    ) {
        VideoPlayerDisplayModeItemList(
            modifier = Modifier.width(268.dp),
            displayModeListProvider = { VideoPlayerDisplayMode.entries.toPersistentList() },
            currentDisplayModeProvider = currentDisplayModeProvider,
            onSelected = onDisplayModeChanged,
            onApplyToGlobal = onApplyToGlobal,
            onUserAction = { screenAutoCloseState.active() },
        )
    }
}

@Preview(device = "id:Android TV (720p)")
@Composable
private fun VideoPlayerDisplayModeScreenPreview() {
    MyTVTheme {
        PreviewWithLayoutGrids {
            VideoPlayerDisplayModeScreen()
        }
    }
}