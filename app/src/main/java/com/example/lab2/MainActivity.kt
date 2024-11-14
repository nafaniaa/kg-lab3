package com.example.lab2

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lab2.ui.theme.Lab2Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class MainActivity : ComponentActivity() {

    private val getImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val inputStream: InputStream? = contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                selectedImage = bitmap
                processedImage = null
            }
        }

    private var selectedImage by mutableStateOf<Bitmap?>(null)
    private var processedImage by mutableStateOf<Bitmap?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageProcessingApp()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ImageProcessingApp() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Image Processing with Filters") },
                    actions = {
                        IconButton(onClick = { getImage.launch("image/*") }) {
                            Icon(Icons.Default.Add, contentDescription = "Load Image")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    selectedImage?.let { image ->
                        Text(text = "Original Image:")
                        Image(
                            bitmap = image.asImageBitmap(),
                            contentDescription = "Original Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { applyMedianFilter() }) {
                            Text("Median Filter")
                        }
                        Button(onClick = { applyHistogramEqualization() }) {
                            Text("Equalize Histogram")
                        }
                        Button(onClick = { applyLinearContrast() }) {
                            Text("Linear Contrast")
                        }
                    }
                }

                item {
                    processedImage?.let { image ->
                        Text(text = "Processed Image:")
                        Image(
                            bitmap = image.asImageBitmap(),
                            contentDescription = "Processed Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }

    private fun applyMedianFilter() {
        selectedImage?.let { originalBitmap ->
            launchProcessing { medianFilter(originalBitmap) }
        }
    }

    private fun applyHistogramEqualization() {
        selectedImage?.let { originalBitmap ->
            launchProcessing { histogramEqualization(originalBitmap) }
        }
    }

    private fun applyLinearContrast() {
        selectedImage?.let { originalBitmap ->
            launchProcessing { linearContrast(originalBitmap) }
        }
    }

    private fun launchProcessing(block: suspend () -> Bitmap?) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.Default) { block() }
            processedImage = result
        }
    }

    fun medianFilter(image: Bitmap): Bitmap {
        val width = image.width
        val height = image.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 1 until width - 1) {
            for (y in 1 until height - 1) {
                val reds = IntArray(9)
                val greens = IntArray(9)
                val blues = IntArray(9)

                var index = 0
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val pixel = image.getPixel(x + dx, y + dy)
                        reds[index] = (pixel shr 16) and 0xFF
                        greens[index] = (pixel shr 8) and 0xFF
                        blues[index] = pixel and 0xFF
                        index++
                    }
                }

                reds.sort()
                greens.sort()
                blues.sort()
                val medianPixel = (0xFF shl 24) or (reds[4] shl 16) or (greens[4] shl 8) or blues[4]
                result.setPixel(x, y, medianPixel)
            }
        }

        return result
    }

    fun histogramEqualization(image: Bitmap): Bitmap {
        val width = image.width
        val height = image.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val histogram = IntArray(256)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = image.getPixel(x, y)
                val gray = ((0.2989 * (pixel shr 16 and 0xFF)) +
                        (0.5870 * (pixel shr 8 and 0xFF)) +
                        (0.1140 * (pixel and 0xFF))).toInt()
                histogram[gray]++
            }
        }

        val cumulativeHistogram = IntArray(256)
        cumulativeHistogram[0] = histogram[0]
        for (i in 1 until 256) {
            cumulativeHistogram[i] = cumulativeHistogram[i - 1] + histogram[i]
        }

        val totalPixels = width * height
        val scaleFactor = 255.0 / totalPixels
        val lut = IntArray(256)
        for (i in 0 until 256) {
            lut[i] = (cumulativeHistogram[i] * scaleFactor).toInt().coerceIn(0, 255)
        }

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = image.getPixel(x, y)
                val gray = ((0.2989 * (pixel shr 16 and 0xFF)) +
                        (0.5870 * (pixel shr 8 and 0xFF)) +
                        (0.1140 * (pixel and 0xFF))).toInt()
                val newGray = lut[gray]
                val newPixel = (0xFF shl 24) or (newGray shl 16) or (newGray shl 8) or newGray
                result.setPixel(x, y, newPixel)
            }
        }

        return result
    }

    fun linearContrast(image: Bitmap): Bitmap {
        val width = image.width
        val height = image.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        var minGray = 255
        var maxGray = 0

        val grayscaleValues = Array(width) { IntArray(height) }

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = image.getPixel(x, y)

                val gray = ((0.2989 * (pixel shr 16 and 0xFF)) +
                        (0.5870 * (pixel shr 8 and 0xFF)) +
                        (0.1140 * (pixel and 0xFF))).toInt()

                grayscaleValues[x][y] = gray

                minGray = minOf(minGray, gray)
                maxGray = maxOf(maxGray, gray)
            }
        }

        if (maxGray == minGray) {
            return image
        }

        for (x in 0 until width) {
            for (y in 0 until height) {
                val gray = grayscaleValues[x][y]

                val newGray = ((gray - minGray) * 255) / (maxGray - minGray)

                val newPixel = (0xFF shl 24) or (newGray shl 16) or (newGray shl 8) or newGray

                result.setPixel(x, y, newPixel)
            }
        }

        return result
    }

}
