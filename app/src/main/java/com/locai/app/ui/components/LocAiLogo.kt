package com.locai.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.locai.app.R

/** LocAi's brand mark — the same artwork wherever the app identifies itself. */
@Composable
fun LocAiLogo(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Image(
        painter = painterResource(R.drawable.ic_locai_logo),
        contentDescription = "LocAi",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}
