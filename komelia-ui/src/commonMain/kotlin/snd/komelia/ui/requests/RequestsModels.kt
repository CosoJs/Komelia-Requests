package snd.komelia.ui.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Lo que responde el servidor de pedidos. Los campos que no se usan acá se ignoran.

@Serializable
data class RqMe(
    val email: String,
    @SerialName("is_admin") val isAdmin: Int = 0,
    @SerialName("auto_approve") val autoApprove: Int = 0,
    @SerialName("can_choose") val canChoose: Int = 0,
)

/** Cuánto hay en Komga: completo, parcial (books/volumes) o en biblioteca si no se sabe el total. */
@Serializable
data class RqAvailability(
    val books: Int,
    val volumes: Int? = null,
    val complete: Boolean = false,
)

/** Una obra de AniList. Se manda tal cual para pedirla. */
@Serializable
data class RqItem(
    @SerialName("anilist_id") val anilistId: Long,
    @SerialName("mal_id") val malId: Long? = null,
    val kind: String,
    val title: String? = null,
    val titles: List<String> = emptyList(),
    val cover: String? = null,
    val volumes: Int? = null,
    val chapters: Int? = null,
    val status: String? = null,
    val year: Int? = null,
    val description: String? = null,
    val url: String? = null,
    @SerialName("request_status") val requestStatus: String? = null,
    val available: RqAvailability? = null,
)

@Serializable
data class RqCreated(val id: Long, val status: String)

@Serializable
data class RqRequest(
    val id: Long,
    val kind: String,
    val title: String,
    val cover: String? = null,
    val status: String,
    @SerialName("requested_by") val requestedBy: String,
    @SerialName("release_title") val releaseTitle: String? = null,
    @SerialName("release_indexer") val releaseIndexer: String? = null,
    val note: String? = null,
    val releasing: Int? = 0,
    val available: RqAvailability? = null,
)

@Serializable
data class RqRelease(
    val guid: String,
    val title: String,
    val size: Long? = null,
    val seeders: Int? = null,
    val leechers: Int? = null,
    val indexer: String? = null,
    val match: Boolean = false,
    val avoid: Boolean = false,
    val languages: List<String> = emptyList(),
    @SerialName("vol_from") val volFrom: Int? = null,
    @SerialName("vol_to") val volTo: Int? = null,
    val new: Boolean = false,
    @SerialName("only_new") val onlyNew: Boolean = false,
    val complete: Boolean = false,
)

@Serializable
data class RqSearch(val have: Int = 0, val results: List<RqRelease> = emptyList())
