package com.darkjade.streamlib.ui.screens.comics

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.darkjade.streamlib.ui.theme.VaultColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComicReaderScreen(
    viewModel: ComicReaderViewModel,
    onBack: () -> Unit,
    onOpenExternally: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var controlsVisible by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            state.isLoading -> CircularProgressIndicator(
                color = VaultColors.Orange,
                modifier = Modifier.align(Alignment.Center)
            )
            state.unsupportedFormat -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "This comic format isn't supported by the built-in reader yet.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        "Open with another app",
                        color = VaultColors.Orange,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.clickable(onClick = onOpenExternally)
                    )
                }
            }
            state.errorMessage != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Couldn't open this comic", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.errorMessage.orEmpty(),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "Go back",
                        color = VaultColors.Orange,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp).clickable(onClick = onBack)
                    )
                }
            }
            state.pages.isNotEmpty() -> {
                val pagerState = rememberPagerState(pageCount = { state.pages.size })

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { controlsVisible = !controlsVisible }
                        }
                ) { page ->
                    AsyncImage(
                        model = state.pages[page],
                        contentDescription = "Page ${page + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (controlsVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Text(state.title, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                        Text(
                            "${pagerState.currentPage + 1} / ${state.pages.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
