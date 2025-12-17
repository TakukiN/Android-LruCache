package com.example.lrucache

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lrucache.ui.theme.LruCacheTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LruCacheTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BenchmarkScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(viewModel: BenchmarkViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var iterations by remember { mutableIntStateOf(5) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LruCache ベンチマーク") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // コントロールセクション
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ベンチマーク設定",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("繰り返し回数:")
                            Slider(
                                value = iterations.toFloat(),
                                onValueChange = { iterations = it.toInt() },
                                valueRange = 1f..10f,
                                steps = 8,
                                modifier = Modifier.weight(1f)
                            )
                            Text("$iterations 回")
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.runBenchmark(context, iterations) },
                                enabled = !state.isRunning,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (state.isRunning) "実行中..." else "ベンチマーク開始")
                            }

                            OutlinedButton(
                                onClick = { viewModel.clearResults() },
                                enabled = !state.isRunning
                            ) {
                                Text("リセット")
                            }
                        }
                    }
                }
            }

            // 進捗表示
            if (state.isRunning) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "処理中: ${state.currentProcess}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "${(state.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // エラー表示
            state.errorMessage?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // 画像プレビュー
            if (state.originalBitmap != null || state.processedBitmap != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "画像プレビュー",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                state.originalBitmap?.let { bitmap ->
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("元画像", style = MaterialTheme.typography.labelMedium)
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "元画像",
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Text(
                                            "${bitmap.width}x${bitmap.height}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                state.processedBitmap?.let { bitmap ->
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("処理後", style = MaterialTheme.typography.labelMedium)
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "処理後画像",
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Text(
                                            "${bitmap.width}x${bitmap.height}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 結果表示
            if (state.results.isNotEmpty()) {
                item {
                    Text(
                        text = "ベンチマーク結果",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(state.results) { result ->
                    ResultCard(result = result)
                }

                // サマリー
                item {
                    val totalSaved = state.results.sumOf { it.savedTime }
                    val avgSpeedup = state.results.map { it.speedupRatio }.average()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "総合結果",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("合計節約時間: ${totalSaved}ms")
                            Text("平均高速化倍率: ${String.format("%.2f", avgSpeedup)}倍")
                            Text(
                                text = "※ キャッシュヒット時は処理をスキップするため大幅に高速化",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultCard(result: BenchmarkResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.processType,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format("%.1f", result.speedupRatio)}倍高速",
                    color = if (result.speedupRatio > 1) Color(0xFF4CAF50) else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ファイル読込+処理",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "${result.withoutCacheTime}ms",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFE57373)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "キャッシュ+処理",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "${result.withCacheTime}ms",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF81C784)
                    )
                }
            }

            // 視覚的な比較バー
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "処理時間比較",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                // キャッシュなし
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFFCDD2))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE57373))
                    )
                }

                // キャッシュあり
                val cacheRatio = if (result.withoutCacheTime > 0) {
                    result.withCacheTime.toFloat() / result.withoutCacheTime
                } else 0f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFC8E6C9))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(cacheRatio.coerceIn(0f, 1f))
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF81C784))
                    )
                }
            }

            Text(
                text = "節約時間: ${result.savedTime}ms (${result.iterations}回の平均)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Text(
                text = "※ 画像処理は両方とも毎回実行",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
