package com.scholr.app.data.network

import kotlinx.serialization.Serializable

@Serializable
data class S2SearchResponse(val data: List<S2PaperDto>? = null)

@Serializable
data class S2PaperDto(
    val paperId: String? = null,
    val title: String? = null,
    val abstract: String? = null,
    val venue: String? = null,
    val year: Int? = null,
    val citationCount: Int? = null,
    val isOpenAccess: Boolean? = null,
    val fieldsOfStudy: List<String>? = null,
    val authors: List<S2AuthorDto>? = null,
    val embedding: S2EmbeddingDto? = null,
    val openAccessPdf: S2OpenAccessPdfDto? = null,
    val url: String? = null
)

@Serializable
data class S2OpenAccessPdfDto(val url: String? = null)

@Serializable
data class S2AuthorDto(val name: String? = null)

@Serializable
data class S2EmbeddingDto(val model: String? = null, val vector: List<Float>? = null)

@Serializable
data class S2BatchRequest(val ids: List<String>)

@Serializable
data class S2RecommendRequest(
    val positivePaperIds: List<String>,
    val negativePaperIds: List<String> = emptyList()
)

@Serializable
data class S2RecommendResponse(val recommendedPapers: List<S2PaperDto>? = null)
