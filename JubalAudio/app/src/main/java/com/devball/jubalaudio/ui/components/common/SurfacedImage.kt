package com.devball.jubalaudio.ui.components.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun SurfacedImage(
    model: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    fallbackIcon: ImageVector = Icons.Default.MusicNote,
    fallbackIconTint: Color = MaterialTheme.colorScheme.surfaceTint,
    surfaceColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    sizeInDp: Dp = 40.dp,
    radius: Dp = sizeInDp / 10f
) {
    //Common painter for error + loading image, customized by tint (defaulted to surfaceTint from MaterialTheme)
    val fallbackPainter = rememberTintedVectorPainter(
        image = fallbackIcon,
        tint = fallbackIconTint
    )

    //Surface for the image to sit atop of
    Surface(
        modifier = modifier.size(sizeInDp),
        shape = RoundedCornerShape(radius),
        color = surfaceColor
    ) {
        //Coil is used for loading the image
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            placeholder = fallbackPainter,
            error = fallbackPainter
        )
    }
}

@Composable
fun rememberTintedVectorPainter(image: ImageVector, tint: Color): Painter {
    val painter = rememberVectorPainter(image = image)
    return remember(painter, tint) {
        object : Painter() {
            override val intrinsicSize: Size = painter.intrinsicSize
            override fun DrawScope.onDraw() {
                with(painter) {
                    //Draw fallback icon with the given tint (from MaterialTheme)
                    //This will not tint the real image (model) if it exists
                    draw(size, colorFilter = ColorFilter.tint(tint))
                }
            }
        }
    }
}

//@Composable
//fun SurfacedImage(
//    model: String?,
//    contentDescription: String,
//    modifier: Modifier = Modifier,
//    fallbackIcon: ImageVector = Icons.Default.MusicNote,
//    fallbackIconTint: Color = MaterialTheme.colorScheme.surfaceTint,
//    surfaceColor: Color = MaterialTheme.colorScheme.surfaceVariant,
//    sizeInDp: Dp = 40.dp,
//    radius: Dp = sizeInDp / 10f
//) {
//    //Common block for error + loading image
//    val fallbackIconBlock: @Composable SubcomposeAsyncImageScope.(AsyncImagePainter.State) -> Unit = {
//        Icon(imageVector = fallbackIcon, contentDescription = null, tint = fallbackIconTint)
//    }
//
//    //Surface for the image to sit atop of
//    Surface(
//        modifier = modifier.size(sizeInDp),
//        shape = RoundedCornerShape(radius),
//        color = surfaceColor
//    ) {
//        //Actual image - Subcompose must be used so that error + loading image can have the custom tint
//        SubcomposeAsyncImage(
//            model = model,
//            contentDescription = contentDescription,
//            contentScale = ContentScale.Crop,
//            modifier = Modifier.fillMaxSize(),
//            error = fallbackIconBlock,
//            loading = fallbackIconBlock
//        )
//    }
//}

