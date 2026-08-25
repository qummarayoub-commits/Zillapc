package com.darkjade.streamlib.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkjade.streamlib.data.db.entity.MediaItemEntity
import com.darkjade.streamlib.ui.theme.VaultColors
import com.darkjade.streamlib.ui.theme.VaultSpacing

@Composable
fun MediaRail(
    title: String,
    items: List<MediaItemEntity>,
    modifier: Modifier = Modifier,
    onItemClick: (MediaItemEntity) -> Unit,
    onAddToList: ((MediaItemEntity) -> Unit)? = null,
    onRemoveFromLibrary: ((MediaItemEntity) -> Unit)? = null,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.padding(top = 40.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = VaultColors.TextPrimary,
            modifier = Modifier.padding(horizontal = VaultSpacing.md, vertical = VaultSpacing.sm)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(VaultSpacing.sm),
            contentPadding = PaddingValues(horizontal = VaultSpacing.md)
        ) {
            items(items, key = { it.id }) { item ->
                PosterCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    showMenu = onAddToList != null || onRemoveFromLibrary != null,
                    onAddToList = onAddToList?.let { { it(item) } },
                    onRemoveFromLibrary = onRemoveFromLibrary?.let { { it(item) } },
                )
            }
        }
    }
}
