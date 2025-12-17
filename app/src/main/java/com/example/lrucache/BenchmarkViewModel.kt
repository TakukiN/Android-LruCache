package com.example.lrucache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "LruCacheBenchmark"

data class BenchmarkResult(
    val processType: String,
    val withCacheTime: Long,
    val withoutCacheTime: Long,
    val iterations: Int,
    val imageName: String,
    val cacheHitRate: Float = 0f
) {
    val speedupRatio: Float
        get() = if (withCacheTime > 0) withoutCacheTime.toFloat() / withCacheTime else Float.MAX_VALUE

    val savedTime: Long
        get() = withoutCacheTime - withCacheTime
}

data class BenchmarkState(
    val isRunning: Boolean = false,
    val currentProcess: String = "",
    val progress: Float = 0f,
    val results: List<BenchmarkResult> = emptyList(),
    val originalBitmap: Bitmap? = null,
    val processedBitmap: Bitmap? = null,
    val errorMessage: String? = null
)

class BenchmarkViewModel : ViewModel() {

    private val _state = MutableStateFlow(BenchmarkState())
    val state: StateFlow<BenchmarkState> = _state.asStateFlow()

    private val cacheManager = ImageCacheManager.getInstance()

    fun runBenchmark(context: Context, iterations: Int = 5) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isRunning = true,
                results = emptyList(),
                errorMessage = null
            )

            try {
                val results = mutableListOf<BenchmarkResult>()

                // 1. 超解像処理のベンチマーク
                _state.value = _state.value.copy(
                    currentProcess = "超解像処理 (2x)",
                    progress = 0.1f
                )
                val superResResult = benchmarkWithSourceImageCache(
                    context,
                    "超解像 (2x)",
                    iterations
                ) { bitmap ->
                    ImageProcessor.superResolution2x(bitmap)
                }
                results.add(superResResult)
                _state.value = _state.value.copy(progress = 0.4f)

                // 2. シャープネス処理のベンチマーク
                _state.value = _state.value.copy(
                    currentProcess = "シャープネス処理",
                    progress = 0.5f
                )
                val sharpenResult = benchmarkWithSourceImageCache(
                    context,
                    "シャープネス",
                    iterations
                ) { bitmap ->
                    ImageProcessor.sharpen(bitmap)
                }
                results.add(sharpenResult)
                _state.value = _state.value.copy(progress = 0.7f)

                // 3. ガウシアンブラー処理のベンチマーク
                _state.value = _state.value.copy(
                    currentProcess = "ガウシアンブラー処理",
                    progress = 0.8f
                )
                val blurResult = benchmarkWithSourceImageCache(
                    context,
                    "ガウシアンブラー",
                    iterations
                ) { bitmap ->
                    ImageProcessor.gaussianBlur(bitmap, 5)
                }
                results.add(blurResult)

                // 表示用に画像を取得
                val originalBitmap = loadSampleImageFromFile(context)
                val lastProcessed = ImageProcessor.superResolution2x(originalBitmap)

                _state.value = _state.value.copy(
                    isRunning = false,
                    progress = 1f,
                    results = results,
                    originalBitmap = originalBitmap,
                    processedBitmap = lastProcessed,
                    currentProcess = "完了"
                )

                // ログに結果を出力
                Log.i(TAG, "========== ベンチマーク結果 ==========")
                Log.i(TAG, "画像サイズ: ${originalBitmap.width}x${originalBitmap.height}")
                Log.i(TAG, "繰り返し回数: $iterations")
                Log.i(TAG, "")
                Log.i(TAG, "【測定内容】")
                Log.i(TAG, "キャッシュなし: 毎回ファイルから画像読み込み → 画像処理")
                Log.i(TAG, "キャッシュあり: LruCacheから画像取得 → 画像処理")
                Log.i(TAG, "※ 画像処理は両方とも毎回実行")
                Log.i(TAG, "")
                results.forEach { result ->
                    Log.i(TAG, "-----------------------------------")
                    Log.i(TAG, "処理: ${result.processType}")
                    Log.i(TAG, "キャッシュなし（ファイル読込+処理）: ${result.withoutCacheTime}ms")
                    Log.i(TAG, "キャッシュあり（メモリ読込+処理）: ${result.withCacheTime}ms")
                    Log.i(TAG, "高速化倍率: ${String.format("%.2f", result.speedupRatio)}倍")
                    Log.i(TAG, "節約時間: ${result.savedTime}ms")
                }
                val totalSaved = results.sumOf { it.savedTime }
                val avgSpeedup = results.map { it.speedupRatio }.average()
                Log.i(TAG, "===================================")
                Log.i(TAG, "合計節約時間: ${totalSaved}ms")
                Log.i(TAG, "平均高速化倍率: ${String.format("%.2f", avgSpeedup)}倍")
                Log.i(TAG, "===================================")

            } catch (e: Exception) {
                Log.e(TAG, "ベンチマークエラー", e)
                _state.value = _state.value.copy(
                    isRunning = false,
                    errorMessage = "エラー: ${e.message}"
                )
            }
        }
    }

    /**
     * 元画像のキャッシュ効果を測定するベンチマーク
     *
     * - キャッシュなし: 毎回ファイルから画像を読み込み → 画像処理実行
     * - キャッシュあり: LruCacheから元画像を取得 → 画像処理実行
     *
     * ※ 画像処理自体は両方とも毎回実行する
     */
    private suspend fun benchmarkWithSourceImageCache(
        context: Context,
        processName: String,
        iterations: Int,
        process: (Bitmap) -> Bitmap
    ): BenchmarkResult = withContext(Dispatchers.Default) {
        val cacheKey = "source_image"

        // ========== キャッシュなし: 毎回ファイルから読み込み ==========
        cacheManager.clearCache()

        var totalTimeWithoutCache = 0L
        for (i in 0 until iterations) {
            val startTime = System.nanoTime()

            // 毎回ファイルから画像を読み込み
            val bitmap = withContext(Dispatchers.IO) {
                loadSampleImageFromFile(context)
            }
            // 画像処理を実行
            val result = process(bitmap)

            val endTime = System.nanoTime()
            totalTimeWithoutCache += (endTime - startTime)

            // 結果は使い捨て
        }
        val avgWithoutCache = totalTimeWithoutCache / iterations / 1_000_000

        // ========== キャッシュあり: LruCacheから元画像を取得 ==========
        cacheManager.clearCache()

        // 最初に元画像をキャッシュに配置
        val sourceImage = withContext(Dispatchers.IO) {
            loadSampleImageFromFile(context)
        }
        cacheManager.addBitmapToCache(cacheKey, sourceImage)

        var totalTimeWithCache = 0L
        for (i in 0 until iterations) {
            val startTime = System.nanoTime()

            // LruCacheから元画像を取得
            val bitmap = cacheManager.getBitmapFromCache(cacheKey)!!
            // 画像処理を実行（毎回実行）
            val result = process(bitmap)

            val endTime = System.nanoTime()
            totalTimeWithCache += (endTime - startTime)

            // 結果は使い捨て
        }
        val avgWithCache = totalTimeWithCache / iterations / 1_000_000

        Log.i(TAG, "$processName: ファイル読込=${avgWithoutCache}ms, キャッシュ=${avgWithCache}ms")

        BenchmarkResult(
            processType = processName,
            withCacheTime = avgWithCache,
            withoutCacheTime = avgWithoutCache,
            iterations = iterations,
            imageName = "sample",
            cacheHitRate = 1.0f // キャッシュありは常にヒット
        )
    }

    /**
     * ファイルから画像を読み込む（キャッシュを使わない）
     */
    private fun loadSampleImageFromFile(context: Context): Bitmap {
        return try {
            context.assets.open("sample.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: createSampleBitmap()
        } catch (e: Exception) {
            try {
                context.assets.open("sample.jpg").use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                } ?: createSampleBitmap()
            } catch (e2: Exception) {
                Log.w(TAG, "サンプル画像が見つからないため、生成画像を使用します")
                createSampleBitmap()
            }
        }
    }

    private fun createSampleBitmap(): Bitmap {
        val width = 256
        val height = 256
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = (x * 255 / width)
                val g = (y * 255 / height)
                val b = ((x + y) * 255 / (width + height))
                pixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    fun clearResults() {
        cacheManager.clearCache()
        _state.value = BenchmarkState()
    }
}
