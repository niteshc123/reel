package com.caratlane.android.mvvm.reels.view

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.caratlane.android.mvvm.reels.model.ReelVideoData
import com.caratlane.android.mvvm.reels.viewmodel.ReelViewModel
import com.caratlane.android.mvvm.util.helpers.EncryptedSharedPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun reelVideoPlayer(
    video: ReelVideoData.Video,
    pagerState: PagerState,
    pageIndex: Int,
    viewModel : ReelViewModel,
    onSingleTap: (exoPlayer: ExoPlayer) -> Unit,
    onVideoDispose: (exoPlayer : ExoPlayer) -> Unit = {},
    onVideoGoBackground: () -> Unit = {}
):ExoPlayer? {
    val context = LocalContext.current
    var thumbnail by remember {
        mutableStateOf(Pair<String,Boolean>("",true))
    }
    var exoPlayer : ExoPlayer? = null
    var isFirstFrameLoad = remember { false }

    LaunchedEffect(key1 = true) {
            withContext(Dispatchers.Main) {
                thumbnail = thumbnail.copy(first = video.thumbnail, second = thumbnail.second)
            }
    }

    if (pagerState.settledPage == pageIndex) {
        Log.d("reel_screen", "settled page" + pagerState.settledPage)
        exoPlayer = remember(context) {
            ExoPlayer.Builder(context).build().apply {
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                repeatMode = Player.REPEAT_MODE_ONE
                setMediaItem(MediaItem.fromUri(Uri.parse(video.videoURL)))
                playWhenReady = true
                volume = if (viewModel.isMuted.value) 0f else 1f
                prepare()
                addListener(object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        super.onRenderedFirstFrame()
                        viewModel.sendCLRunwayVideoPlayedEvent()
                        val tooltipVisible = EncryptedSharedPrefs.instance?.reelTooltipVisible ?: true
                        isTooltipVisible.value = tooltipVisible
                        isFirstFrameLoad = true
                        thumbnail = thumbnail.copy(second = false)
                    }
                })
            }
        }

        val lifecycleOwner by rememberUpdatedState(LocalLifecycleOwner.current)
        DisposableEffect(key1 = lifecycleOwner) {
            val lifeCycleObserver = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        Log.d("lifecycle_event_Exo","in stop")
                        exoPlayer.pause()
                        onVideoGoBackground()
                    }
                    Lifecycle.Event.ON_START -> exoPlayer.play()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(lifeCycleObserver)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(lifeCycleObserver)
            }
        }

        val playerView = remember {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }

        DisposableEffect(key1 = AndroidView(factory = {
            playerView
        }, modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                onSingleTap(exoPlayer)
            })
        }), effect = {
            onDispose {
                thumbnail = thumbnail.copy(second = true)
                viewModel.sendCLRunwayVideoDropOffEvent(dropOffDuration = exoPlayer.currentPosition, totalDuration = exoPlayer.duration)
                exoPlayer.release()
                onVideoDispose(exoPlayer)
            }
        })
    }

    if (thumbnail.second) {
        AsyncImage(
            model = thumbnail.first,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth
        )
    }
    return exoPlayer
}





