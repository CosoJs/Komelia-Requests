package snd.komelia.ui.requests

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.parseServerSetCookieHeader
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import snd.komelia.settings.CommonSettingsRepository
import snd.komelia.settings.SecretsRepository

/** Error que muestra la pantalla tal cual (viene del servidor de pedidos). */
class RequestsException(message: String) : Exception(message)

/**
 * Cliente del servidor de pedidos (Taberna Requests, https://github.com/CosoJs/taberna-requests).
 *
 * Entra con la cookie "recordarme" que Komelia ya guardó al iniciar sesión en Komga:
 * el servidor de pedidos le pregunta a Komga de quién es, así que no hace falta contraseña.
 * El servidor se busca en el mismo dominio que Komga, en /pedidos/.
 */
class RequestsClient(
    private val settings: CommonSettingsRepository,
    private val secrets: SecretsRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val http = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) { requestTimeoutMillis = 120_000 }  // buscar en los trackers puede tardar
    }
    private val loginMutex = Mutex()
    private var token: String? = null

    /** https://host/komga → https://host/pedidos */
    suspend fun baseUrl(): String {
        val komga = Url(settings.getServerUrl().first())
        val port = if (komga.port == komga.protocol.defaultPort) "" else ":${komga.port}"
        return "${komga.protocol.name}://${komga.host}$port/pedidos"
    }

    private suspend fun login(): String {
        val komgaUrl = settings.getServerUrl().first()
        val cookie = secrets.getCookie(komgaUrl)?.let { parseServerSetCookieHeader(it).value }
            ?: throw RequestsException("No hay sesión de Komga guardada: cerrá sesión en Komelia y volvé a entrar con \"recordarme\".")
        val response: JsonObject = try {
            http.post("${baseUrl()}/api/login") {
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject { put("komga_remember_me", cookie) })
            }.body()
        } catch (e: ResponseException) {
            throw RequestsException("El servidor de pedidos no aceptó tu sesión de Komga (${e.response.status.value}).")
        }
        return response["token"]?.jsonPrimitive?.content
            ?: throw RequestsException("El servidor de pedidos no devolvió una sesión. ¿Está actualizado?")
    }

    /** Hace la llamada con la sesión; si venció, vuelve a entrar una vez. */
    private suspend fun <T> call(block: suspend (HttpRequestBuilder.() -> Unit) -> T): T {
        repeat(2) { attempt ->
            val current = loginMutex.withLock { token ?: login().also { token = it } }
            try {
                return block { header("X-Session", current) }
            } catch (e: ResponseException) {
                if (e.response.status == HttpStatusCode.Unauthorized && attempt == 0) {
                    loginMutex.withLock { if (token == current) token = null }
                } else {
                    val msg = runCatching {
                        json.decodeFromString<JsonObject>(e.response.bodyAsText())["error"]?.jsonPrimitive?.content
                    }.getOrNull()
                    throw RequestsException(msg ?: "Error del servidor de pedidos (${e.response.status.value})")
                }
            }
        }
        throw RequestsException("No se pudo iniciar sesión en el servidor de pedidos.")
    }

    suspend fun me(): RqMe = call { auth -> http.get("${baseUrl()}/api/me") { auth() }.body() }

    suspend fun discover(query: String, kind: String): List<RqItem> = call { auth ->
        http.get("${baseUrl()}/api/anilist") { auth(); parameter("q", query); parameter("kind", kind) }.body()
    }

    suspend fun createRequest(item: RqItem): RqCreated = call { auth ->
        http.post("${baseUrl()}/api/requests") {
            auth(); contentType(ContentType.Application.Json); setBody(item)
        }.body()
    }

    suspend fun requests(): List<RqRequest> = call { auth -> http.get("${baseUrl()}/api/requests") { auth() }.body() }

    /** approve, deny, delete, follow */
    suspend fun action(id: Long, action: String) {
        call { auth ->
            http.post("${baseUrl()}/api/requests/$id/$action") {
                auth(); contentType(ContentType.Application.Json); setBody(JsonObject(emptyMap()))
            }
        }
    }

    suspend fun searchReleases(id: Long): RqSearch = call { auth ->
        http.post("${baseUrl()}/api/requests/$id/search") {
            auth(); contentType(ContentType.Application.Json); setBody(JsonObject(emptyMap()))
        }.body()
    }

    suspend fun grab(id: Long, guid: String) {
        call { auth ->
            http.post("${baseUrl()}/api/requests/$id/grab") {
                auth(); contentType(ContentType.Application.Json); setBody(buildJsonObject { put("guid", guid) })
            }
        }
    }
}
