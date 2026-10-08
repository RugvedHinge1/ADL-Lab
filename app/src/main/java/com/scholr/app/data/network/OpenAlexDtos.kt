package com.scholr.app.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAlexSearchResponse(
    val results: List<OpenAlexWorkDto>? = null
)

@Serializable
data class OpenAlexWorkDto(
    val id: String? = null,
    val title: String? = null,
    @SerialName("publication_year") val publicationYear: Int? = null,
    @SerialName("cited_by_count") val citedByCount: Int? = null,
    val authorships: List<OpenAlexAuthorshipDto>? = null,
    @SerialName("primary_location") val primaryLocation: OpenAlexLocationDto? = null,
    @SerialName("open_access") val openAccess: OpenAlexOpenAccessDto? = null,
    @SerialName("abstract_inverted_index") val abstractInvertedIndex: Map<String, List<Int>>? = null,
    val concepts: List<OpenAlexConceptDto>? = null
)

@Serializable
data class OpenAlexAuthorshipDto(val author: OpenAlexAuthorDto? = null)

@Serializable
data class OpenAlexAuthorDto(@SerialName("display_name") val displayName: String? = null)

@Serializable
data class OpenAlexLocationDto(val source: OpenAlexSourceDto? = null)

@Serializable
data class OpenAlexSourceDto(@SerialName("display_name") val displayName: String? = null)

@Serializable
data class OpenAlexOpenAccessDto(@SerialName("is_oa") val isOa: Boolean? = null)

@Serializable
data class OpenAlexConceptDto(
    @SerialName("display_name") val displayName: String? = null,
    val score: Double? = null
)
