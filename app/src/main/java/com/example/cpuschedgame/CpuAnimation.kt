package com.example.cpuschedgame

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.cpuschedgame.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun CpuAnimation(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    LaunchedEffect(Unit) {
        delay(3000L)
        onSplashComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(R.raw.cpuloading_animation)
                    .build(),
                imageLoader        = imageLoader,
                contentDescription = "CPU loading animation",
                modifier           = Modifier.size(220.dp)
            )

            Text(
                "CPU SCHEDULER",
                color         = GoldenBright,
                fontSize      = 20.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Text(
                "Loading...",
                color         = TextSecondary,
                fontSize      = 11.sp,
                letterSpacing = 2.sp
            )
        }
    }
}