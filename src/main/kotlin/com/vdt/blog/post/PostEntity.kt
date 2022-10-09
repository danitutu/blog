package com.vdt.blog.post

import java.time.Instant
import java.util.UUID

data class PostEntity(
    val id: UUID,
    val title: String,
    val summary: String,
    val originalContent: String,
    val htmlContent: String,
    val friendlyUrl: String,
    val createdAt: Instant,
    val publishedAt: Instant?,
)
