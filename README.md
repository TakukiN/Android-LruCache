# LruCache Benchmark - Android画像処理キャッシュ効果検証

AndroidのLruCacheを使用した画像処理のパフォーマンス比較アプリです。

## 概要

このアプリは、重い画像処理（超解像、シャープネス、ガウシアンブラー）において、元画像をLruCacheにキャッシュした場合としない場合でどれだけ処理時間が変わるかを測定します。

**重要**: 画像処理自体は両方のケースで毎回実行されます。キャッシュの効果は「ファイル読み込み vs メモリ読み込み」の差を測定しています。

## ベンチマーク結果

### テスト環境
- **デバイス**: Android実機
- **画像サイズ**: 953x586 (東京夜景画像)
- **繰り返し回数**: 5回

### 測定内容
- **キャッシュなし**: 毎回ファイルから画像読み込み → 画像処理
- **キャッシュあり**: LruCacheから画像取得 → 画像処理
- ※ 画像処理は両方とも毎回実行

### 結果サマリー

| 処理 | ファイル読込+処理 | キャッシュ+処理 | 高速化倍率 | 節約時間 |
|------|------------------|----------------|-----------|---------|
| **超解像 (2x)** | 11,078ms | 10,470ms | **1.06倍** | 608ms |
| シャープネス | 510ms | 460ms | **1.11倍** | 50ms |
| ガウシアンブラー | 5,248ms | 5,101ms | **1.03倍** | 147ms |

### 総合結果
- **合計節約時間**: 805ms
- **平均高速化倍率**: 1.07倍（約6〜11%の高速化）

## 結果の分析

### LruCacheの効果

1. **ファイルI/Oのオーバーヘッド削減**
   - ファイル読み込み時間（約50〜600ms）がキャッシュで節約される
   - 特にシャープネス処理では11%の高速化を達成

2. **画像処理時間が支配的**
   - 画像処理自体（数秒〜10秒）が全体の大部分を占める
   - そのため、全体への影響は限定的（3〜11%）

3. **実用的な効果**
   - 同じ画像を繰り返し処理するシナリオでは効果あり
   - 5回の処理で合計約800msの節約

### LruCacheが効果的なユースケース

- 画像ギャラリーでのサムネイル表示（同じ画像を繰り返し読み込み）
- フォトエディターでの複数フィルター適用（元画像を何度も参照）
- スクロールリストでの画像表示（画面外→画面内の繰り返し）

## プロジェクト構成

```
app/src/main/java/com/example/lrucache/
├── MainActivity.kt          # Compose UI
├── BenchmarkViewModel.kt    # ベンチマークロジック
├── ImageProcessor.kt        # 画像処理（超解像、シャープネス、ブラー）
└── ImageCacheManager.kt     # LruCacheラッパー
```

## LruCacheの実装

### キャッシュ設定

```kotlin
class ImageCacheManager {
    // 利用可能メモリの1/8をキャッシュに使用
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024  // KB単位
        }
    }
}
```

### キャッシュの使用方法

```kotlin
// キャッシュキー
val cacheKey = "source_image"

// キャッシュに保存
cacheManager.addBitmapToCache(cacheKey, bitmap)

// キャッシュから取得
val cachedBitmap = cacheManager.getBitmapFromCache(cacheKey)
```

## 画像処理の詳細

### 超解像処理 (2x)
- バイキュービック補間（Catmull-Rom）
- 入力画像を2倍の解像度に拡大
- 最も計算量が多い処理（約10秒）

### シャープネス
- アンシャープマスクフィルター
- 3x3カーネルによるエッジ強調
- 軽量な処理（約0.5秒）

### ガウシアンブラー
- ガウス関数によるぼかし処理
- 11x11カーネル（radius=5）
- 中程度の処理（約5秒）

## ビルド方法

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 使い方

1. アプリを起動
2. 繰り返し回数をスライダーで設定（1〜10回）
3. 「ベンチマーク開始」ボタンをタップ
4. 結果が表示される

## 結論

LruCacheを活用することで、元画像のファイル読み込みをスキップし、**約6〜11%の高速化**が実現できました。

ただし、重い画像処理自体の時間が支配的なため、劇的な高速化は期待できません。LruCacheは主に**ファイルI/Oのオーバーヘッド削減**に効果があり、同じ画像を繰り返し参照するシナリオで有効です。

より大きな高速化を実現するには：
- 処理結果自体をキャッシュする
- GPUを使用した画像処理（RenderScript、Vulkan等）
- ネイティブコード（NDK）による最適化

などの手法が必要です。

## ライセンス

MIT License
