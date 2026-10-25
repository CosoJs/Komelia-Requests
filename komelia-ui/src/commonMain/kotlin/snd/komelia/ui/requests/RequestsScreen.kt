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
import snd.komelia.ui.LocalViewModelFactory

/** Pestaña "Pedidos": buscar en AniList, pedir y, si sos admin, aprobar y elegir release. */
class RequestsScreen : Screen {
    @Composable
    override fun Content() {
        val factory = LocalViewModelFactory.current
        val vm = rememberScreenModel { factory.getRequestsViewModel() }
        LaunchedEffect(Unit) { vm.initialize() }
        RequestsContent(vm)
    }
}

private val KIND_LABEL = mapOf("manga" to "Manga", "novel" to "Novela ligera")
private val STATUS_LABEL = mapOf(
    "pending" to "Pendiente", "approved" to "Aprobado", "grabbed" to "Descargando", "denied" to "Rechazado"
)

@Composable
private fun RequestsContent(vm: RequestsViewModel) {
    val tab by vm.tab.collectAsState()
    val loading by vm.loading.collectAsState()
    val message by vm.message.collectAsState()

    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tab.ordinal) {
            Tab(selected = tab == RequestsTab.DISCOVER, onClick = { vm.tab.value = RequestsTab.DISCOVER },
                text = { Text("Descubrir") })
            Tab(selected = tab == RequestsTab.REQUESTS, onClick = { vm.tab.value = RequestsTab.REQUESTS },
                text = { Text("Pedidos") })
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
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Cerrar") }
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
            KIND_LABEL.forEach { (value, label) ->
                FilterChip(selected = kind == value, onClick = { vm.setKind(value) }, label = { Text(label) })
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { vm.query.value = it },
            placeholder = { Text("Buscar en AniList…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.search() }),
            trailingIcon = { IconButton(onClick = vm::search) { Icon(Icons.Default.Search, contentDescription = "Buscar") } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            Text("Buscá un manga o una novela ligera para pedirla.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        listOfNotNull(item.year?.toString(), item.volumes?.let { "$it vol." })
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
                        Text(listOfNotNull(KIND_LABEL[item.kind], item.year?.toString(),
                            item.volumes?.let { "$it volúmenes" }, item.status).joinToString(" · "),
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
                complete -> Text("Ya está completa en la biblioteca.")
                else -> Button(enabled = !requesting, onClick = { vm.request(item) }) {
                    Text(if (requesting) "Pidiendo…" else if (item.available != null) "Pedir lo que falta" else "Pedir")
                }
            }
        },
        dismissButton = { TextButton(onClick = { vm.selected.value = null }) { Text("Cerrar") } }
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
            Text("${requests.size} pedidos", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = vm::loadRequests) { Icon(Icons.Default.Refresh, contentDescription = "Actualizar") }
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
                Text("${KIND_LABEL[r.kind] ?: r.kind} · pedido por ${r.requestedBy}", style = MaterialTheme.typography.labelSmall)
                r.releaseTitle?.let {
                    Text("Release: $it (${r.releaseIndexer ?: "?"})", style = MaterialTheme.typography.labelSmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (r.releasing == 1) {
                    Text("📡 En seguimiento: se buscan volúmenes nuevos cada día.", style = MaterialTheme.typography.labelSmall)
                }
                r.note?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary) }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (admin && r.status == "pending") {
                        Button(onClick = { vm.action(r, "approve") }) { Text("Aprobar") }
                        OutlinedButton(onClick = { vm.action(r, "deny") }) { Text("Rechazar") }
                    }
                    if (chooser && r.status != "denied") {
                        OutlinedButton(onClick = { vm.searchReleases(r) }) { Text("Buscar release") }
                    }
                    if (admin && r.status != "denied") {
                        OutlinedButton(onClick = { vm.action(r, "follow") }) {
                            Text(if (r.releasing == 1) "Dejar de seguir" else "Seguir nuevos")
                        }
                    }
                    if (admin || (owner && r.status == "pending")) {
                        TextButton(onClick = { vm.action(r, "delete") }) {
                            Text("Borrar", color = MaterialTheme.colorScheme.error)
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
        title = { Text("Releases: ${search.request.title}") },
        text = {
            val result = search.result
            if (search.loading || result == null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (result.have > 0) {
                        Text("Ya tenés ${result.have} en Komga. \"nuevos\" = trae solo lo que falta.",
                            style = MaterialTheme.typography.labelMedium)
                    }
                    if (result.results.isEmpty()) Text("Sin resultados.")
                    result.results.take(50).forEach { rel -> ReleaseRow(rel) { vm.grab(search.request, rel) } }
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.releaseSearch.value = null }) { Text("Cerrar") } }
    )
}

@Composable
private fun ReleaseRow(rel: RqRelease, onGrab: () -> Unit) {
    val dim = !rel.match || rel.avoid
    Column(Modifier.alpha(if (dim) 0.5f else 1f)) {
        Text(rel.title, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
        val vols = when {
            rel.volFrom == null -> "vol. ?"
            rel.volFrom == rel.volTo -> "vol. ${rel.volFrom}"
            else -> "vol. ${rel.volFrom}–${rel.volTo}"
        }
        val langs = rel.languages.ifEmpty { listOf("?") }.joinToString(", ")
        Text("${rel.indexer ?: "?"} · $vols · $langs · ${size(rel.size)} · ${rel.seeders ?: "?"}/${rel.leechers ?: "?"}",
            style = MaterialTheme.typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                rel.onlyNew -> Badge("nuevos", MaterialTheme.colorScheme.primaryContainer)
                rel.new -> Badge("incluye nuevos", MaterialTheme.colorScheme.secondaryContainer)
            }
            if (rel.complete) Badge("completo", MaterialTheme.colorScheme.primaryContainer)
            if (!rel.match) Text("no coincide con el título", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Button(onClick = onGrab) { Text("Descargar") }
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
                available.complete -> "Disponible"
                available.volumes != null -> "Parcial ${available.books}/${available.volumes}"
                else -> "En biblioteca · ${available.books}"
            },
            if (available.complete) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        )

        status != null -> Badge(STATUS_LABEL[status] ?: status, MaterialTheme.colorScheme.surfaceVariant)
        available != null -> Badge("En biblioteca · ${available.books}", MaterialTheme.colorScheme.secondaryContainer)
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
