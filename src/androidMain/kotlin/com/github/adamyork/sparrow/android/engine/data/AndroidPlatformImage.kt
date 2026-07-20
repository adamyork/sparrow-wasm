package com.github.adamyork.sparrow.android.engine.data

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.github.adamyork.sparrow.platform.engine.data.PlatformImage

class AndroidPlatformImage(
	val image: ImageBitmap,
	val bitmap: Bitmap
) : PlatformImage {
	constructor(bitmap: Bitmap) : this(bitmap.asImageBitmap(), bitmap)
}
