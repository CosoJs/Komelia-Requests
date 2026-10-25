package snd.komelia.ui.requests

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.github.snd_r.komelia.ui.komelia_ui.generated.resources.*
import org.jetbrains.compose.resources.getString

enum class RequestsTab { DISCOVER, REQUESTS }

/** Resultados de "Buscar release" de un pedido (null = cerrado). */
data class ReleaseSearch(val request: RqRequest, val loading: Boolean = true, val result: RqSearch? = null)

class RequestsViewModel(private val client: RequestsClient) : ScreenModel {
    private val _me = MutableStateFlow<RqMe?>(null)
    val me: StateFlow<RqMe?> = _me.asStateFlow()

    val tab = MutableStateFlow(RequestsTab.DISCOVER)
    val kind = MutableStateFlow("manga")
    val query = MutableStateFlow("")

    private val _items = MutableStateFlow<List<RqItem>>(emptyList())
    val items: StateFlow<List<RqItem>> = _items.asStateFlow()

    private val _requests = MutableStateFlow<List<RqRequest>>(emptyList())
    val requests: StateFlow<List<RqRequest>> = _requests.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Mensaje para mostrar (error o confirmación); la pantalla lo limpia al mostrarlo. */
    val message = MutableStateFlow<String?>(null)

    val selected = MutableStateFlow<RqItem?>(null)
    val requesting = MutableStateFlow(false)
    val releaseSearch = MutableStateFlow<ReleaseSearch?>(null)

    fun initialize() {
        if (_me.value != null) return
        launchLoading { _me.value = client.me() }
    }

    private fun launchLoading(block: suspend () -> Unit) {
        screenModelScope.launch {
            _loading.value = true
            try {
                block()
            } catch (e: RequestsException) {
                message.value = e.message
            } catch (e: Exception) {
                message.value = getString(Res.string.requests_err_connect, e.message ?: "")
            } finally {
                _loading.value = false
            }
        }
    }

    fun search() {
        val q = query.value.trim()
        if (q.isEmpty()) return
        launchLoading { _items.value = client.discover(q, kind.value) }
    }

    fun setKind(value: String) {
        if (kind.value == value) return
        kind.value = value
        _items.value = emptyList()
    }

    fun request(item: RqItem) {
        requesting.value = true
        screenModelScope.launch {
            try {
                val created = client.createRequest(item)
                _items.value = _items.value.map {
                    if (it.anilistId == item.anilistId) it.copy(requestStatus = created.status) else it
                }
                message.value = if (created.status == "approved") getString(Res.string.requests_msg_approved)
                else getString(Res.string.requests_msg_pending)
                selected.value = null
            } catch (e: Exception) {
                message.value = e.message ?: getString(Res.string.requests_err_request)
            } finally {
                requesting.value = false
            }
        }
    }

    fun loadRequests() = launchLoading { _requests.value = client.requests() }

    /** approve, deny, delete, follow */
    fun action(request: RqRequest, action: String) = launchLoading {
        client.action(request.id, action)
        _requests.value = client.requests()
    }

    fun searchReleases(request: RqRequest) {
        releaseSearch.value = ReleaseSearch(request)
        screenModelScope.launch {
            try {
                releaseSearch.value = ReleaseSearch(request, loading = false, result = client.searchReleases(request.id))
            } catch (e: Exception) {
                releaseSearch.value = null
                message.value = e.message ?: getString(Res.string.requests_err_search)
            }
        }
    }

    fun grab(request: RqRequest, release: RqRelease) = launchLoading {
        client.grab(request.id, release.guid)
        releaseSearch.value = null
        message.value = getString(Res.string.requests_msg_sent_to_client)
        _requests.value = client.requests()
    }
}
