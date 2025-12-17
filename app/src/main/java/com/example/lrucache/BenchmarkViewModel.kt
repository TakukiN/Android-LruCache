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
                // サンプル画像を読み込み
                val originalBitmap = withContext(Dispatchers.IO) {
                    loadSampleImage(context)
                }

                Log.i(TAG, "元画像サイズ: ${originalBitmap.width}x${originalBitmap.height}")

                _state.value = _state.value.copy(originalBitmap = originalBitmap)

                val results = mutableListOf<BenchmarkResult>()

                // 1. 超解像処理のベンチマーク
                _state.value = _state.value.copy(
                    currentProcess = "超解像処理 (2x)",
                    progress = 0.1f
                )
                val superResResult = benchmarkProcessRealistic(
                    originalBitmap,
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
                val sharpenResult = benchmarkProcessRealistic(
                    originalBitmap,
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
                val blurResult = benchmarkProcessRealistic(
                    originalBitmap,
                    "ガウシアンブラー",
                    iterations
                ) { bitmap ->
                    ImageProcessor.gaussianBlur(bitmap, 5)
                }
                results.add(blurResult)

                // 最後に処理された画像を保存（超解像の結果を表示）
                val lastProcessed = ImageProcessor.superResolution2x(originalBitmap)

                _state.value = _state.value.copy(
                    isRunning = false,
                    progress = 1f,
                    results = results,
                    processedBitmap = lastProcessed,
                    currentProcess = "完了"
                )

                // ログに結果を出力
                Log.i(TAG, "========== ベンチマーク結果 ==========")
                Log.i(TAG, "画像サイズ: ${originalBitmap.width}x${originalBitmap.height}")
                Log.i(TAG, "繰り返し回数: $iterations")
                results.forEach { result ->
                    Log.i(TAG, "-----------------------------------")
                    Log.i(TAG, "処理: ${result.processType}")
                    Log.i(TAG, "キャッシュなし（毎回処理）: ${result.withoutCacheTime}ms")
                    Log.i(TAG, "キャッシュあり（2回目以降ヒット）: ${result.withCacheTime}ms")
                    Log.i(TAG, "高速化倍率: ${String.format("%.1f", result.speedupRatio)}倍")
                    Log.i(TAG, "節約時間: ${result.savedTime}ms")
                    Log.i(TAG, "キャッシュヒット率: ${String.format("%.0f", result.cacheHitRate * 100)}%")
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
     * より現実的なベンチマーク
     * - キャッシュなし: 毎回新規に処理を実行
     * - キャッシュあり: 同じキーでアクセスし、キャッシュヒット時はスキップ
     *
     * これは「同じ画像を繰り返し処理するシナリオ」での効果を測定
     */
    private suspend fun benchmarkProcessRealistic(
        bitmap: Bitmap,
        processName: String,
        iterations: Int,
        process: (Bitmap) -> Bitmap
    ): BenchmarkResult = withContext(Dispatchers.Default) {
        val cacheKey = "${processName}_${bitmap.width}x${bitmap.height}"

        // ========== キャッシュなしの処理時間を測定 ==========
        // 毎回新規に処理を実行（キャッシュを使わない）
        cacheManager.clearCache()

        var totalTimeWithoutCache = 0L
        for (i in 0 until iterations) {
            val startTime = System.nanoTime()
            val result = process(bitmap) // 毎回処理を実行
            val endTime = System.nanoTime()
            totalTimeWithoutCache += (endTime - startTime)
            // 結果は使い捨て（キャッシュに保存しない）
        }
        val avgWithoutCache = totalTimeWithoutCache / iterations / 1_000_000 // ミリ秒に変換

        // ========== キャッシュありの処理時間を測定 ==========
        // 同じキーで繰り返しアクセス（初回はミス、2回目以降はヒット）
        cacheManager.clearCache()

        var totalTimeWithCache = 0L
        var processedResult: Bitmap? = null

        for (i in 0 until iterations) {
            val startTime = System.nanoTime()

            // キャッシュを確認
            val cached = cacheManager.getBitmapFromCache(cacheKey)
            processedResult = if (cached != null) {
                // キャッシュヒット - 処理をスキップ
                cached
            } else {
                // キャッシュミス - 処理を実行してキャッシュに保存
                process(bitmap).also { result ->
                    cacheManager.addBitmapToCache(cacheKey, result)
                }
            }

            val endTime = System.nanoTime()
            totalTimeWithCache += (endTime - startTime)
        }
        val avgWithCache = totalTimeWithCache / iterations / 1_000_000 // ミリ秒に変換

        val stats = cacheManager.getCacheStats()

        // キャッシュヒット率 = (iterations - 1) / iterations （初回はミス）
        val hitRate = if (iterations > 1) (iterations - 1).toFloat() / iterations else 0f

        BenchmarkResult(
            processType = processName,
            withCacheTime = avgWithCache,
            withoutCacheTime = avgWithoutCache,
            iterations = iterations,
            imageName = "sample",
            cacheHitRate = hitRate
        )
    }

    private fun loadSampleImage(context: Context): Bitmap {
        // assetsから画像を読み込む
        return try {
            // まずPNGを試す
            context.assets.open("sample.png").use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    // 画像が大きすぎる場合はサンプリング
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)

                // リセットしてから再度読み込み
                inputStream.reset()

                val sampleSize = calculateInSampleSize(options, 512, 512)
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }

                context.assets.open("sample.png").use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                } ?: createSampleBitmap()
            }
        } catch (e: Exception) {
            try {
                // JPGを試す
                context.assets.open("sample.jpg").use { inputStream ->
                    BitmapFactory.decodeStream(inputStream) ?: createSampleBitmap()
                }
            } catch (e2: Exception) {
                Log.w(TAG, "サンプル画像が見つからないため、生成画像を使用します")
                createSampleBitmap()
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun createSampleBitmap(): Bitmap {
        // テスト用のグラデーション画像を生成（256x256）
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
