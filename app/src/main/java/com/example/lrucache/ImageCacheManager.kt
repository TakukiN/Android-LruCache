package com.example.lrucache

import android.graphics.Bitmap
import android.util.LruCache

/**
 * LruCacheを使用した画像キャッシュマネージャー
 */
class ImageCacheManager private constructor() {

    // 利用可能なメモリの1/8をキャッシュに使用
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            // キャッシュサイズをKBで計測
            return bitmap.byteCount / 1024
        }
    }

    /**
     * キャッシュから画像を取得
     */
    fun getBitmapFromCache(key: String): Bitmap? {
        return memoryCache.get(key)
    }

    /**
     * キャッシュに画像を保存
     */
    fun addBitmapToCache(key: String, bitmap: Bitmap) {
        if (getBitmapFromCache(key) == null) {
            memoryCache.put(key, bitmap)
        }
    }

    /**
     * キャッシュをクリア
     */
    fun clearCache() {
        memoryCache.evictAll()
    }

    /**
     * キャッシュの統計情報を取得
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            hitCount = memoryCache.hitCount(),
            missCount = memoryCache.missCount(),
            size = memoryCache.size(),
            maxSize = memoryCache.maxSize(),
            putCount = memoryCache.putCount(),
            evictionCount = memoryCache.evictionCount()
        )
    }

    data class CacheStats(
        val hitCount: Int,
        val missCount: Int,
        val size: Int,
        val maxSize: Int,
        val putCount: Int,
        val evictionCount: Int
    ) {
        val hitRate: Float
            get() = if (hitCount + missCount > 0) {
                hitCount.toFloat() / (hitCount + missCount)
            } else 0f
    }

    companion object {
        @Volatile
        private var instance: ImageCacheManager? = null

        fun getInstance(): ImageCacheManager {
            return instance ?: synchronized(this) {
                instance ?: ImageCacheManager().also { instance = it }
            }
        }
    }
}
