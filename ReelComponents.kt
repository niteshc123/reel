package com.caratlane.android.mvvm.reels.view


import android.os.Looper
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.caratlane.android.R
import com.caratlane.android.Utils.Constants
import com.caratlane.android.Utils.Methods
import com.caratlane.android.Utils.compose.DashedDivider
import com.caratlane.android.Utils.compose.MyCustomFont
import com.caratlane.android.mvvm.reels.model.ReelVideoData
import com.caratlane.android.mvvm.reels.viewmodel.ReelViewModel
import com.caratlane.android.mvvm.util.checkSkuAlreadyInWihslit
import com.caratlane.android.mvvm.util.helpers.EncryptedSharedPrefs.Companion.instance
import com.caratlane.android.mvvm.util.helpers.SharedPrefHlpr
import com.caratlane.android.mvvm.util.isWishlistAvailable
import com.example.tiktokcompose.ui.effect.AnimationEffect
import com.example.tiktokcompose.ui.effect.PlayerErrorEffect
import com.example.tiktokcompose.ui.effect.ResetAnimationEffect
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.logging.Handler


var isTooltipVisible =
    mutableStateOf(false)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsScreen(
    viewModel: ReelViewModel,
    onBackPressed: () -> Unit,
    onProductClick: (Constants.Product, ReelVideoData.ProductInfo) -> Unit,
    onLikeClick: (reelData: ReelVideoData.Video, index: Int) -> Unit,
    onShareClick: (reelData: ReelVideoData.Video?) -> Unit,
) {
    isTooltipVisible = remember {
        mutableStateOf(false)
    }
    viewModel.isMuted = remember {
        mutableStateOf(false)
    }
    //var exoplayer: ExoPlayer? = null

    //var pauseButtonVisibility by remember { mutableStateOf(false) }

    var animatedIconDrawable by remember {
        mutableStateOf(0)
    }
    val iconVisibleState = remember {
        MutableTransitionState(false)
    }
    var animationJob: Job? by remember {
        mutableStateOf(null)
    }
    val context = LocalContext.current

    viewModel.onLiked = remember {
        { index: Int, liked: Boolean, count: Int ->
            viewModel.videos.get(index).copy(
                likesInfo = ReelVideoData.LikesInfo(
                    count = count.toLong(),
                    likedByUser = liked
                )
            ).let {
                viewModel.videos.set(
                    index,
                    it
                )
            }
        }
    }
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) { -> viewModel.videos.size }

    val fling = PagerDefaults.flingBehavior(
        state = pagerState, lowVelocityAnimationSpec = tween(
            easing = LinearEasing, durationMillis = 300
        )
    )


    /*LaunchedEffect(key1 = true) {
        Log.d("reel_screen", "current page in launched effect" + pagerState.currentPage)
        snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { page ->
            if (page == (viewModel.videos.size.minus(2))) {
                viewModel.offset = viewModel.offset.plus(viewModel.limit)
                viewModel.fetchReelInfo(
                    viewModel.offset,
                    limit = viewModel.limit,
                    isReelsPage = true,
                    exclusionVideoId = 0
                )
            }
            *//*Log.d("reel_screen", "current position -" + state.player?.currentPosition.toString())
            Log.d("reel_screen", "total duration -" + state.player?.duration.toString())*//*
            *//* viewModel.sendCLRunwayVideoDropOffEvent(
                 dropOffDuration = state.player?.currentPosition ?: 0,
                 totalDuration = state.player?.duration ?: 0
             )*//*
            pagerState.animateScrollToPage(page)
        }
    }*/

    Box(
        modifier = Modifier
            .graphicsLayer()
    )
    {
        VerticalPager(
            state = pagerState,
            flingBehavior = fling,
            beyondBoundsPageCount = 1,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { index ->
            if (pagerState.settledPage == (viewModel.videos.size.minus(2))) {
                viewModel.offset = viewModel.offset.plus(viewModel.limit)
                viewModel.fetchReelInfo(
                    viewModel.offset,
                    limit = viewModel.limit,
                    isReelsPage = true,
                    exclusionVideoId = 0
                )
            }
            viewModel.currentVideoData = viewModel.videos[index]
            viewModel.currentIndex = index

            if (pagerState.currentPage % 10 == 0) {
                Log.d(
                    ReelsFragment.TAG,
                    "trigger after every 10" + viewModel.videos[pagerState.currentPage].toString()
                )
                viewModel.sendCLRunwayVideoTileScrollEvent(currentPosition = pagerState.currentPage)
            }

            Log.d("lifecycle_event_Exo", "current page ${pagerState.currentPage}")
            Log.d("lifecycle_event_Exo", "current page ${pagerState.settledPage}")



            Box(modifier = Modifier.fillMaxSize()) {
                viewModel.videos.let {
                    viewModel.exoPlayer = reelVideoPlayer(
                        it[index], pagerState, index, viewModel,
                        onSingleTap = {
                            viewModel.onTappedScreen(viewModel.exoPlayer)
                        },
                        onVideoDispose = {
                            //pauseButtonVisibility = false
                        },
                        onVideoGoBackground = {
                            Log.d("lifecycle_event_Exo", "on video go background")

                            //pauseButtonVisibility = false
                        }
                    )
                }
                ReelItem(
                    viewModel = viewModel,
                    index = index,
                    reel = viewModel.videos.get(index),
                    onIconClicked = { icon ->
                        when (icon) {
                            Constants.Icon.SHARE -> {
                                Log.d(
                                    ReelsFragment.TAG,
                                    "current position -" + viewModel.exoPlayer?.currentPosition.toString()
                                )
                                Log.d(
                                    ReelsFragment.TAG,
                                    "total duration -" + viewModel.exoPlayer?.duration.toString()
                                )
                                viewModel.videos.get(index)
                                    .let { onShareClick.invoke(viewModel.videos.get(index)) }
                            }

                            Constants.Icon.LIKE -> {
                                viewModel.videos.get(index).let { onLikeClick.invoke(it, index) }
                                if (!Methods.isGuestUser()) {
                                    if (viewModel.videos.get(
                                            index
                                        ).likesInfo.likedByUser
                                    ) {
                                        viewModel.onLiked(
                                            index, false,
                                            viewModel.videos.get(index).likesInfo.count.minus(1)
                                                .toInt() ?: 0
                                        )
                                    } else {
                                        viewModel.onLiked(
                                            index, true,
                                            viewModel.videos.get(index).likesInfo.count.plus(1)
                                                .toInt() ?: 0
                                        )
                                    }
                                }
                            }

                            else -> {}
                        }
                    },
                    onProductClick = onProductClick,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            ReelHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                viewModel = viewModel
            ) {
                when (it) {
                    Constants.Icon.BACK -> {
                        Log.d("onClick", "backbutton")
                        viewModel.sendCLRunwayVideoClosedEvent(
                            viewModel.exoPlayer?.currentPosition ?: 0,
                            viewModel.exoPlayer?.duration ?: 0
                        )
                        onBackPressed.invoke()
                    }

                    Constants.Icon.MUTE -> {
                        Log.d("onClick", "mute")
                        viewModel.onClickMute(viewModel.exoPlayer)
                        if (viewModel.isMuted.value) {
                            viewModel.sendCLRunwayVideoUnMuteEvent()
                        } else {
                            viewModel.sendCLRunwayVideoMuteEvent()
                        }
                    }

                    else -> {}
                }
            }
        }
        AnimatedVisibility(
            visibleState = iconVisibleState,
            enter = scaleIn(
                spring(Spring.DampingRatioMediumBouncy)
            ),
            exit = scaleOut(tween(150)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            androidx.compose.material.Icon(
                painter = painterResource(animatedIconDrawable),
                contentDescription = null,
                tint = Color.White.copy(0.90f),
                modifier = Modifier
                    .size(100.dp)
            )
        }
        ReelsTooltip(viewModel = viewModel) //fn to show tooltip for first time
        LaunchedEffect(key1 = true) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is PlayerErrorEffect -> {
                        val message =
                            if (effect.code == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED || effect.code == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
                                "Please check your internet connection"
                            else
                                "An error occurred. Code: ${effect.code}"
                        Methods.showToast(message, context)
                    }

                    is AnimationEffect -> {
                        animatedIconDrawable = effect.drawable
                        animationJob = launch {
                            iconVisibleState.targetState = true
                            delay(800)
                            iconVisibleState.targetState = false
                        }
                    }

                    is ResetAnimationEffect -> {
                        iconVisibleState.targetState = false
                        animationJob?.cancel()
                    }
                }
            }
        }
    }

}

@Composable
fun ReelHeader(
    modifier: Modifier = Modifier,
    viewModel: ReelViewModel,
    onIconClicked: (Constants.Icon) -> Unit
) {
    viewModel.iconDrawable = remember {
        mutableIntStateOf(R.drawable.ic_unmute_volume)
    }
    Box(
        modifier = modifier
            .padding(PaddingValues(8.dp, 16.dp)),
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            IconButton(
                onClick = { onIconClicked(Constants.Icon.BACK) },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back_arrow),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(30.dp)
                )
            }
        }

        IconButton(
            onClick = { onIconClicked(Constants.Icon.MUTE) },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                painter = painterResource(viewModel.iconDrawable.intValue),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(30.dp)
            )
        }
    }
}

@Composable
private fun ReelItem(
    viewModel: ReelViewModel,
    index: Int,
    reel: ReelVideoData.Video?,
    onIconClicked: (Constants.Icon) -> Unit,
    modifier: Modifier = Modifier,
    onProductClick: (Constants.Product, ReelVideoData.ProductInfo) -> Unit,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(0.5f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter)
        ) {
            ReelsInfoItems(
                viewModel = viewModel,
                index = index,
                reelInfo = reel,
                onProductClick = onProductClick,
                onIconClicked = {
                    onIconClicked(it)
                }
            )
        }
    }
}

@Composable
private fun ReelsInfoItems(
    viewModel: ReelViewModel,
    index: Int,
    reelInfo: ReelVideoData.Video?,
    onIconClicked: (Constants.Icon) -> Unit,
    onProductClick: (Constants.Product, ReelVideoData.ProductInfo) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Top
        ) {
            ReelsColumnIcons(
                viewModel = viewModel,
                index = index,
                onIconClicked = onIconClicked,
                reelInfo = reelInfo
            )
        }
        Box(
            modifier = Modifier
                .wrapContentHeight(),
            contentAlignment = Alignment.BottomStart
        ) {
            if (reelInfo?.productInfo != null)
                ReelsBottomItems(
                    modifier = Modifier.fillMaxWidth(),
                    reelInfo = reelInfo,
                    onProductClick = onProductClick, viewModel = viewModel
                )
        }
    }
}


@OptIn(ExperimentalSnapperApi::class)
@Composable
private fun ReelsBottomItems(
    viewModel: ReelViewModel,
    modifier: Modifier = Modifier,
    reelInfo: ReelVideoData.Video?,
    onProductClick: (Constants.Product, ReelVideoData.ProductInfo) -> Unit
) {
    val state = rememberLazyListState()
    viewModel.refreshProductList = remember {
        mutableStateOf(true)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        DashedDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp), color = colorResource(id = R.color.white)
        )
        Spacer(modifier = Modifier.height(20.dp))
        AnimatedVisibility(
            visible = viewModel.refreshProductList.value,
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                state = state, contentPadding = PaddingValues(horizontal = 16.dp),
                flingBehavior = rememberSnapperFlingBehavior(state),
                userScrollEnabled = true
            ) {
                itemsIndexed(reelInfo?.productInfo as ArrayList) { _index, data ->
                    ReelsBottomProductItem(
                        reelInfo = data,
                        onProductClick = onProductClick,
                        viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun ReelsBottomProductItem(
    reelInfo: ReelVideoData.ProductInfo?,
    onProductClick: (Constants.Product, ReelVideoData.ProductInfo) -> Unit,
    viewModel: ReelViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var isButtonClickable by remember { mutableStateOf(true) }
    val skuList = SharedPrefHlpr.instance?.wishlistSkus
    val inWishlist =
        !skuList.equals(Constants.tokenDefaultValue, ignoreCase = true) && skuList?.contains(
            reelInfo?.sku ?: ""
        ) == true

    val isWishlist = remember { mutableStateOf(inWishlist) }
    val icWishlist =
        if (isWishlist.value) painterResource(R.drawable.ic_filled_wishlist_new) else painterResource(
            R.drawable.ic_empty_wishlist
        )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(end = 18.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(
                        width = 0.5.dp,
                        shape = RoundedCornerShape(18.dp),
                        color = colorResource(id = R.color.color_CFC1FF)
                    )
            ) {
                Image(
                    painter = rememberAsyncImagePainter(reelInfo?.imageURL),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape = RoundedCornerShape(16.dp))
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = reelInfo?.name ?: "",
                    color = colorResource(id = R.color.blue_magenta),
                    textAlign = TextAlign.Center,
                    fontFamily = MyCustomFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.widthIn(max = 150.dp)
                )
                Row {
                    (if (reelInfo?.formattedSpecialPrice.isNullOrBlank()
                            .not()
                    ) reelInfo?.formattedSpecialPrice else reelInfo?.formattedPrice)?.let {
                        Text(
                            text = it,
                            color = colorResource(id = R.color.blue_magenta),
                            textAlign = TextAlign.Center,
                            fontFamily = MyCustomFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    if (reelInfo?.formattedSpecialPrice.isNullOrBlank()
                            .not() && reelInfo?.formattedPrice.isNullOrBlank().not()
                    )
                        Text(
                            text = reelInfo?.formattedPrice ?: "",
                            color = colorResource(id = R.color.blue_magenta),
                            textAlign = TextAlign.Center,
                            fontFamily = MyCustomFont,
                            fontWeight = FontWeight.Normal,
                            fontSize = 10.sp,
                            style = TextStyle(textDecoration = TextDecoration.LineThrough)
                        )
                }
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxHeight()
                ) {
                    val gradient = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to colorResource(id = R.color.color_E56EEB),
                            0.5f to colorResource(id = R.color.selectionColor)
                        )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .background(
                                brush = gradient, shape = RoundedCornerShape(8.dp)
                            )
                            .padding(top = 10.dp, bottom = 10.dp, start = 12.dp, end = 12.dp)
                            .clickable {
                                if (isButtonClickable && reelInfo != null) {
                                    coroutineScope.launch {
                                        isButtonClickable = false
                                        viewModel.sendCLRunwayShopProductsEvent(reelInfo.sku)
                                        onProductClick(Constants.Product.VIEW_DESIGN, reelInfo)
                                        delay(1000)
                                        isButtonClickable = true
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "View Design".uppercase(Locale.getDefault()),
                            color = colorResource(id = R.color.white),
                            textAlign = TextAlign.Center,
                            fontFamily = MyCustomFont,
                            fontWeight = FontWeight.Normal,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .background(
                                color = colorResource(id = R.color.color_EAE3FF),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(top = 1.dp, bottom = 1.dp, start = 1.dp, end = 1.dp)
                            .clickable {
                                if (!viewModel.isItemCTAClicked.value) {
                                    viewModel.isItemCTAClicked.value = true
                                    isWishlist.value = !isWishlist.value
                                    reelInfo?.sku?.let {
                                        wishlistOption(viewModel, it)
                                    }
                                }
                                viewModel.viewModelScope.launch {
                                    delay(600)// to prevent the frequent double click
                                    viewModel.isItemCTAClicked.value = false
                                }
                            },
                    ) {
                        Icon(
                            painter = icWishlist,
                            tint = Color.Unspecified,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReelsColumnIcons(
    viewModel: ReelViewModel,
    index: Int,
    reelInfo: ReelVideoData.Video?,
    onIconClicked: (Constants.Icon) -> Unit
) {
    viewModel.update.collectAsState(initial = false)
    val iconButtonModifier = Modifier
        .width(30.dp)
        .height(30.dp)
    TextedIcon(
        iconRes = getLikeIcon(viewModel = viewModel, index = index, reelInfo),
        text = reelInfo?.likesInfo?.count.toString(),
        modifier = Modifier.size(24.dp),
        iconButtonModifier = iconButtonModifier,
        onIconClicked = {
            onIconClicked(Constants.Icon.LIKE)
        },
    )
    Spacer(modifier = Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.CenterStart, modifier = Modifier
                .weight(4f)
        ) {
            Row(modifier = Modifier.padding(start = 2.dp)) {
                Text(
                    text = "Created by",
                    color = colorResource(id = R.color.white),
                    textAlign = TextAlign.Center,
                    fontFamily = MyCustomFont,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = reelInfo?.name ?: "",
                    color = colorResource(id = R.color.white),
                    textAlign = TextAlign.Center,
                    fontFamily = MyCustomFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                )
            }
        }
        Box(
            contentAlignment = Alignment.CenterEnd, modifier = Modifier
                .weight(1f)
        ) {
            IconButton(
                modifier = iconButtonModifier,
                onClick = { onIconClicked(Constants.Icon.SHARE) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_share_reel),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun TextedIcon(
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int,
    text: String,
    tint: Color = Color.Unspecified,
    contentDescription: String? = null,
    iconButtonModifier: Modifier = Modifier,
    onIconClicked: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        IconButton(modifier = iconButtonModifier, onClick = {
            onIconClicked()
        }) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = tint,
                modifier = modifier
            )
        }
        Text(
            text = text,
            color = Color.White
        )
    }
}

private fun setTooltipVisibility() {
    isTooltipVisible.value = false
    instance?.reelTooltipVisible = false
}

@Composable
private fun ReelsTooltip(alpha: Float = 0.7f, viewModel: ReelViewModel) {
    AnimatedVisibility(
        visible = isTooltipVisible.value,
        enter = fadeIn(animationSpec = tween(500)),
        exit = fadeOut(animationSpec = tween(500))
    ) {
        Surface(
            color = Color.Black.copy(alpha = alpha), modifier = Modifier
                .fillMaxSize()
                .clickable {
                    setTooltipVisibility()
                    viewModel.sendTooltipGotItEvent()
                }
        ) {
            Box(modifier = Modifier.fillMaxSize(1.0f), contentAlignment = Alignment.Center) {
                AnimatedVisibility(
                    visible = true,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ComposeLottieAnimation(
                            modifier = Modifier
                                .width(100.dp)
                                .height(100.dp)
                                .padding(bottom = 20.dp)
                        )
                        Text(
                            text = "Swipe up for next video",
                            color = colorResource(id = R.color.white),
                            textAlign = TextAlign.Center,
                            fontFamily = MyCustomFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .height(24.dp)
                                .wrapContentWidth()
                                .background(
                                    color = colorResource(id = R.color.white),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(top = 4.dp, bottom = 4.dp, start = 12.dp, end = 12.dp)
                                .clickable {
                                    setTooltipVisibility()
                                    viewModel.sendTooltipGotItEvent()
                                }, contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Got it",
                                color = colorResource(id = R.color.black),
                                textAlign = TextAlign.Center,
                                fontFamily = MyCustomFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun ComposeLottieAnimation(modifier: Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.swipe_up))
    LottieAnimation(
        modifier = modifier,
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
}


private fun getLikeIcon(viewModel: ReelViewModel, index: Int, reelInfo: ReelVideoData.Video?): Int {
    Log.d("getLikeIcon", viewModel.videos.get(index)?.likesInfo?.likedByUser.toString())
    Log.d("getLikeIcon", viewModel.videos.get(index).likesInfo?.count.toString())
    return if (viewModel.videos.get(index).likesInfo.likedByUser)
        R.drawable.like_filled
    else
        R.drawable.ic_like
}

fun wishlistOption(reelViewModel: ReelViewModel, sku: String) {
    if (isWishlistAvailable() == true) {

        if (checkSkuAlreadyInWihslit(sku)) {
            wishlistRemove(reelViewModel, sku)
        } else {
            wishlistAdd(reelViewModel, sku)
        }
    } else {
        generateWishlistId(reelViewModel, sku)
    }
}

private fun generateWishlistId(reelViewModel: ReelViewModel, sku: String?) {
    reelViewModel.wishlistAPICallPart(reelViewModel.CREATE_WISHLIST_ID, sku)
}

private fun wishlistAdd(reelViewModel: ReelViewModel, sku: String?) {
    reelViewModel.wishlistAPICallPart(reelViewModel.WISHLIST_ADD, sku)
}

private fun wishlistRemove(reelViewModel: ReelViewModel, sku: String?) {
    reelViewModel.wishlistAPICallPart(reelViewModel.WISHLIST_REMOVE, sku)
}
