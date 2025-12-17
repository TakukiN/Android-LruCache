package com.example.lrucache

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * 画像処理ユーティリティクラス
 * 超解像処理やフィルター処理を提供
 */
object ImageProcessor {

    /**
     * バイキュービック補間による超解像処理（2倍）
     * 計算量が多いため、キャッシュの効果を測定しやすい
     */
    fun superResolution2x(source: Bitmap): Bitmap {
        val srcWidth = source.width
        val srcHeight = source.height
        val dstWidth = srcWidth * 2
        val dstHeight = srcHeight * 2

        val result = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888)

        // ソースピクセルを取得
        val srcPixels = IntArray(srcWidth * srcHeight)
        source.getPixels(srcPixels, 0, srcWidth, 0, 0, srcWidth, srcHeight)

        val dstPixels = IntArray(dstWidth * dstHeight)

        for (y in 0 until dstHeight) {
            for (x in 0 until dstWidth) {
                // 元画像での座標（小数点）
                val srcX = x / 2.0f
                val srcY = y / 2.0f

                // バイキュービック補間
                val color = bicubicInterpolate(srcPixels, srcWidth, srcHeight, srcX, srcY)
                dstPixels[y * dstWidth + x] = color
            }
        }

        result.setPixels(dstPixels, 0, dstWidth, 0, 0, dstWidth, dstHeight)
        return result
    }

    /**
     * バイキュービック補間
     */
    private fun bicubicInterpolate(
        pixels: IntArray,
        width: Int,
        height: Int,
        x: Float,
        y: Float
    ): Int {
        val x0 = x.toInt()
        val y0 = y.toInt()
        val dx = x - x0
        val dy = y - y0

        var r = 0.0f
        var g = 0.0f
        var b = 0.0f
        var a = 0.0f

        for (j in -1..2) {
            for (i in -1..2) {
                val px = clamp(x0 + i, 0, width - 1)
                val py = clamp(y0 + j, 0, height - 1)
                val pixel = pixels[py * width + px]

                val weight = cubicWeight(i - dx) * cubicWeight(j - dy)

                a += Color.alpha(pixel) * weight
                r += Color.red(pixel) * weight
                g += Color.green(pixel) * weight
                b += Color.blue(pixel) * weight
            }
        }

        return Color.argb(
            clamp(a.toInt(), 0, 255),
            clamp(r.toInt(), 0, 255),
            clamp(g.toInt(), 0, 255),
            clamp(b.toInt(), 0, 255)
        )
    }

    /**
     * キュービック重み関数（Catmull-Rom）
     */
    private fun cubicWeight(x: Float): Float {
        val absX = kotlin.math.abs(x)
        return when {
            absX < 1 -> 1.5f * absX * absX * absX - 2.5f * absX * absX + 1
            absX < 2 -> -0.5f * absX * absX * absX + 2.5f * absX * absX - 4 * absX + 2
            else -> 0f
        }
    }

    /**
     * シャープネスフィルター処理
     * アンシャープマスクを適用
     */
    fun sharpen(source: Bitmap, strength: Float = 1.5f): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val srcPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val dstPixels = IntArray(width * height)

        // シャープネスカーネル
        val kernel = floatArrayOf(
            0f, -strength, 0f,
            -strength, 1 + 4 * strength, -strength,
            0f, -strength, 0f
        )

        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0f
                var g = 0f
                var b = 0f

                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val px = clamp(x + kx, 0, width - 1)
                        val py = clamp(y + ky, 0, height - 1)
                        val pixel = srcPixels[py * width + px]
                        val weight = kernel[(ky + 1) * 3 + (kx + 1)]

                        r += Color.red(pixel) * weight
                        g += Color.green(pixel) * weight
                        b += Color.blue(pixel) * weight
                    }
                }

                val alpha = Color.alpha(srcPixels[y * width + x])
                dstPixels[y * width + x] = Color.argb(
                    alpha,
                    clamp(r.toInt(), 0, 255),
                    clamp(g.toInt(), 0, 255),
                    clamp(b.toInt(), 0, 255)
                )
            }
        }

        result.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * ガウシアンブラー処理
     */
    fun gaussianBlur(source: Bitmap, radius: Int = 5): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val srcPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)

        // ガウシアンカーネルを生成
        val kernelSize = radius * 2 + 1
        val kernel = FloatArray(kernelSize * kernelSize)
        val sigma = radius / 3.0f
        var sum = 0f

        for (y in -radius..radius) {
            for (x in -radius..radius) {
                val value = kotlin.math.exp(-(x * x + y * y) / (2 * sigma * sigma)).toFloat()
                kernel[(y + radius) * kernelSize + (x + radius)] = value
                sum += value
            }
        }

        // 正規化
        for (i in kernel.indices) {
            kernel[i] /= sum
        }

        val dstPixels = IntArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0f
                var g = 0f
                var b = 0f

                for (ky in -radius..radius) {
                    for (kx in -radius..radius) {
                        val px = clamp(x + kx, 0, width - 1)
                        val py = clamp(y + ky, 0, height - 1)
                        val pixel = srcPixels[py * width + px]
                        val weight = kernel[(ky + radius) * kernelSize + (kx + radius)]

                        r += Color.red(pixel) * weight
                        g += Color.green(pixel) * weight
                        b += Color.blue(pixel) * weight
                    }
                }

                val alpha = Color.alpha(srcPixels[y * width + x])
                dstPixels[y * width + x] = Color.argb(
                    alpha,
                    clamp(r.toInt(), 0, 255),
                    clamp(g.toInt(), 0, 255),
                    clamp(b.toInt(), 0, 255)
                )
            }
        }

        result.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun clamp(value: Int, min: Int, max: Int): Int {
        return max(min, min(max, value))
    }
}
