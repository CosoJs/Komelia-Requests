@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package snd.komelia.ui.requests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import coil3.compose.AsyncImage
import io.github.snd_r.komelia.ui.komelia_ui.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import snd.komelia.ui.LocalViewModelFactory

/** Pestaña "Pedidos": buscar en AniList, pedir y, si sos admin, aprobar y elegir release.
 *  Textos en composeResources (values = inglés, values-es = español). */
class RequestsScreen : Screen {
    @Composable
    override fun Content() {
        val factory = LocalViewModelFactory.current
        val vm = rememberScreenModel { factory.getRequestsViewModel() }
        LaunchedEffect(Unit) { vm.initialize() }
        RequestsContent(vm)
    }
}

private val KINDS = listOf("manga", "novel")

@Composable
private fun kindLabel(kind: String) = when (kind) {
    "manga" -> stringResource(Res.string.requests_kind_manga)
    "novel" -> stringResource(Res.string.requests_kind_novel)
    else -> kind
}

@Composable
private fun statusLabel(status: String) = when (status) {
    "pending" -> stringResource(Res.string.requests_status_pending)
    "approved" -> stringResource(Res.string.requests_status_approved)
    "grabbed" -> stringResource(Res.string.requests_status_grabbed)
    "denied" -> stringResource(Res.string.requests_status_denied)
    else -> status
}

@Composable
private fun RequestsContent(vm: RequestsViewModel) {
    val tab by vm.tab.collectAsState()
    val loading by vm.loading.collectAsState()
    val message by vm.message.collectAsState()

    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tab.ordinal) {
            Tab(selected = tab == RequestsTab.DISCOVER, onClick = { vm.tab.value = RequestsTab.DISCOVER },
                text = { Text(stringResource(Res.string.requests_discover)) })
            Tab(selected = tab == RequestsTab.REQUESTS, onClick = { vm.tab.value = RequestsTab.REQUESTS },
                text = { Text(stringResource(Res.string.requests_tab)) })
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth()) else Spacer(Modifier.height(4.dp))

        message?.let { MessageBanner(it) { vm.message.value = null } }

        when (tab) {
            RequestsTab.DISCOVER -> DiscoverTab(vm)
            RequestsTab.REQUESTS -> RequestsTab(vm)
        }
    }

    vm.selected.collectAsState().value?.let { DetailDialog(vm, it) }
    vm.releaseSearch.collectAsState().value?.let { ReleasesDialog(vm, it) }
}

@Composable
private fun MessageBanner(text: String, onClose: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.requests_close)) }
        }
    }
}

// ---------------------------------------------------------------- Descubrir

@Composable
private fun DiscoverTab(vm: RequestsViewModel) {
    val kind by vm.kind.collectAsState()
    val query by vm.query.collectAsState()
    val items by vm.items.collectAsState()

    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KINDS.forEach { value ->
                FilterChip(selected = kind == value, onClick = { vm.setKind(value) }, label = { Text(kindLabel(value)) })
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { vm.query.value = it },
            placeholder = { Text(stringResource(Res.string.requests_search_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.search() }),
            trailingIcon = { IconButton(onClick = vm::search) { Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.requests_search_action)) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            Text(stringResource(Res.string.requests_discover_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items, key = { it.anilistId }) { item ->
                Card(Modifier.clickable { vm.selected.value = item }) {
                    Cover(item.cover, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                    Column(Modifier.padding(6.dp)) {
                        Text(item.title ?: "", maxLines = 2, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge)
                        listOfNotNull(item.year?.toString(), item.volumes?.let { stringResource(Res.string.requests_volumes_short, it) })
                            .takeIf { it.isNotEmpty() }
                            ?.let { Text(it.joinToString(" · "), style = MaterialTheme.typography.labelSmall) }
                        StatusBadge(item.requestStatus, item.available)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailDialog(vm: RequestsViewModel, item: RqItem) {
    val requesting by vm.requesting.collectAsState()
    val complete = item.available?.complete == true
    AlertDialog(
        onDismissRequest = { vm.selected.value = null },
        title = { Text(item.title ?: "") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Cover(item.cover, Modifier.width(90.dp).aspectRatio(2f / 3f))
                    Column {
                        Text(listOfNotNull(kindLabel(item.kind), item.year?.toString(),
                            item.volumes?.let { stringResource(Res.string.requests_volumes, it) }, item.status).joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium)
                        StatusBadge(item.requestStatus, item.available)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(plain(item.description), style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            when {
                item.requestStatus != null -> {}
                complete -> Text(stringResource(Res.string.requests_already_complete))
                else -> Button(enabled = !requesting, onClick = { vm.request(item) }) {
                    Text(
                        if (requesting) stringResource(Res.string.requests_requesting)
                        else if (item.available != null) stringResource(Res.string.requests_request_missing)
                        else stringResource(Res.string.requests_request)
                    )
                }
            }
        },
        dismissButton = { TextButton(onClick = { vm.selected.value = null }) { Text(stringResource(Res.string.requests_close)) } }
    )
}

// ---------------------------------------------------------------- Pedidos

@Composable
private fun RequestsTab(vm: RequestsViewModel) {
    val requests by vm.requests.collectAsState()
    val me by vm.me.collectAsState()
    LaunchedEffect(Unit) { vm.loadRequests() }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(stringResource(Res.string.requests_count, requests.size), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = vm::loadRequests) { Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.requests_refresh)) }
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
        ) {
            items(requests, key = { it.id }) { request -> RequestCard(vm, request, me) }
        }
    }
}

@Composable
private fun RequestCard(vm: RequestsViewModel, r: RqRequest, me: RqMe?) {
    val admin = me?.isAdmin == 1
    val owner = me != null && r.requestedBy == me.email
    val chooser = admin || (owner && me?.canChoose == 1)

    Card {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Cover(r.cover, Modifier.width(60.dp).aspectRatio(2f / 3f))
            Column(Modifier.weight(1f)) {
                Text(r.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                StatusBadge(r.status, r.available)
                Text(stringResource(Res.string.requests_requested_by, kindLabel(r.kind), r.requestedBy), style = MaterialTheme.typography.labelSmall)
                r.releaseTitle?.let {
                    Text(stringResource(Res.string.requests_release, it, r.releaseIndexer ?: "?"), style = MaterialTheme.typography.labelSmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (r.releasing == 1) {
                    Text(stringResource(Res.string.requests_following), style = MaterialTheme.typography.labelSmall)
                }
                r.note?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary) }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (admin && r.status == "pending") {
                        Button(onClick = { vm.action(r, "approve") }) { Text(stringResource(Res.string.requests_approve)) }
                        OutlinedButton(onClick = { vm.action(r, "deny") }) { Text(stringResource(Res.string.requests_deny)) }
                    }
                    if (chooser && r.status != "denied") {
                        OutlinedButton(onClick = { vm.searchReleases(r) }) { Text(stringResource(Res.string.requests_find_release)) }
                    }
                    if (admin && r.status != "denied") {
                        OutlinedButton(onClick = { vm.action(r, "follow") }) {
                            Text(if (r.releasing == 1) stringResource(Res.string.requests_unfollow) else stringResource(Res.string.requests_follow))
                        }
                    }
                    if (admin || (owner && r.status == "pending")) {
                        TextButton(onClick = { vm.action(r, "delete") }) {
                            Text(stringResource(Res.string.requests_delete), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleasesDialog(vm: RequestsViewModel, search: ReleaseSearch) {
    AlertDialog(
        onDismissRequest = { vm.releaseSearch.value = null },
        title = { Text(stringResource(Res.string.requests_releases_title, search.request.title)) },
        text = {
            val result = search.result
            if (search.loading || result == null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (result.have > 0) {
                        Text(stringResource(Res.string.requests_have_in_komga, result.have),
                            style = MaterialTheme.typography.labelMedium)
                    }
                    if (result.results.isEmpty()) Text(stringResource(Res.string.requests_no_results))
                    result.results.take(50).forEach { rel -> ReleaseRow(rel) { vm.grab(search.request, rel) } }
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.releaseSearch.value = null }) { Text(stringResource(Res.string.requests_close)) } }
    )
}

@Composable
private fun ReleaseRow(rel: RqRelease, onGrab: () -> Unit) {
    val dim = !rel.match || rel.avoid
    Column(Modifier.alpha(if (dim) 0.5f else 1f)) {
        Text(rel.title, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        val vols = when {
            rel.volFrom == null -> stringResource(Res.string.requests_vol_unknown)
            rel.volFrom == rel.volTo -> stringResource(Res.string.requests_vol_single, rel.volFrom)
            else -> stringResource(Res.string.requests_vol_range, rel.volFrom, rel.volTo ?: "?")
        }
        val langs = rel.languages.ifEmpty { listOf("?") }.joinToString(", ")
        Text("${rel.indexer ?: "?"} · $vols · $langs · ${size(rel.size)} · ${rel.seeders ?: "?"}/${rel.leechers ?: "?"}",
            style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                rel.onlyNew -> Badge(stringResource(Res.string.requests_badge_new), MaterialTheme.colorScheme.primaryContainer)
                rel.new -> Badge(stringResource(Res.string.requests_badge_includes_new), MaterialTheme.colorScheme.secondaryContainer)
            }
            if (rel.complete) Badge(stringResource(Res.string.requests_badge_complete), MaterialTheme.colorScheme.primaryContainer)
            if (!rel.match) Text(stringResource(Res.string.requests_no_title_match), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Button(onClick = onGrab) { Text(stringResource(Res.string.requests_download)) }
        }
    }
}

// ---------------------------------------------------------------- comunes

/** Si ya está en Komga, eso manda (Disponible / Parcial); si no, el estado del pedido. */
@Composable
private fun StatusBadge(status: String?, available: RqAvailability?) {
    val showAvailability = available != null && status !in setOf("pending", "denied")
    when {
        showAvailability && available != null -> Badge(
            when {
                available.complete -> stringResource(Res.string.requests_available)
                available.volumes != null -> stringResource(Res.string.requests_partial, available.books, available.volumes)
                else -> stringResource(Res.string.requests_in_library, available.books)
            },
            if (available.complete) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        )

        status != null -> Badge(statusLabel(status), MaterialTheme.colorScheme.surfaceVariant)
        available != null -> Badge(stringResource(Res.string.requests_in_library, available.books), MaterialTheme.colorScheme.secondaryContainer)
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Surface(color = color, shape = RoundedCornerShape(50), modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
private fun Cover(url: String?, modifier: Modifier) {
    val shaped = modifier.clip(RoundedCornerShape(6.dp))
    if (url == null) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = shaped) {}
    } else {
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = shaped)
    }
}

private fun plain(html: String?): String =
    (html ?: "").replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n").replace(Regex("<[^>]+>"), "").trim()

private fun size(bytes: Long?): String = when {
    bytes == null -> "?"
    bytes >= 1_000_000_000 -> "${bytes / 100_000_000 / 10.0} GB"
    else -> "${bytes / 1_000_000} MB"
}
