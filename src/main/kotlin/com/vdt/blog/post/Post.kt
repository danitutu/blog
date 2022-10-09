package com.vdt.blog.post

import com.vdt.blog.tag.TagEntity
import java.time.Instant
import java.util.UUID

data class Post(
    val id: UUID,
    val title: String,
    val summary: String,
    val originalContent: String,
    val htmlContent: String,
    val friendlyUrl: String,
    val createdAt: Instant,
    val publishedAt: Instant?,
    val tags: Set<String>,
) {
    constructor(post: PostEntity, tags: Collection<TagEntity>) : this(
        id = post.id,
        title = post.title,
        summary = post.summary,
        originalContent = post.originalContent,
        htmlContent = post.htmlContent,
        friendlyUrl = post.friendlyUrl,
        createdAt = post.createdAt,
        publishedAt = post.publishedAt,
        tags = tags.map { it.name }.toSet()
    )
}