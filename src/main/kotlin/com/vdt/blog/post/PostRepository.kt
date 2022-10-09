package com.vdt.blog.post

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

@Repository
class PostRepository(
    private val template: NamedParameterJdbcTemplate,
) {
    fun findByFriendlyUrl(friendlyUrl: String): Collection<PostEntity> {
        return template.query(
            "select * from post where friendly_url=:friendlyUrl",
            MapSqlParameterSource("friendlyUrl", friendlyUrl),
            ::rowMapper
        )
    }

    fun save(post: PostEntity) {
        template.update(
            "insert into post (id, title, summary, original_content, html_content, friendly_url, created_at) " +
                    "values (:id, :title, :summary,:originalContent,:htmlContent,:friendlyUrl,:createdAt)",
            MapSqlParameterSource().addValues(
                mapOf(
                    "id" to post.id,
                    "title" to post.title,
                    "summary" to post.summary,
                    "originalContent" to post.originalContent,
                    "htmlContent" to post.htmlContent,
                    "friendlyUrl" to post.friendlyUrl,
                    "createdAt" to Timestamp.from(post.createdAt),
                )
            )
        )
    }

    fun search(fetchUnpublished: Boolean = false, tags: Collection<PostTagEntity>? = null): Collection<PostEntity> {
        val queryParams = MapSqlParameterSource()

        val publishedAtCondition = if (fetchUnpublished) "" else "published_at is not null"
        val postIdsCondition = if (tags == null) "" else {
            if (tags.isEmpty()) {
                return emptyList()
            }
            queryParams.addValue("postIds", tags.map(PostTagEntity::postId))
            "id in (:postIds)"
        }
        val conditions = listOf(publishedAtCondition, postIdsCondition)
            .filter { it.isNotEmpty() }
            .joinToString(" AND ")
        val whereCondition = if (conditions.isEmpty()) "" else "where $conditions"

        return template.query(
            "select * from post $whereCondition order by created_at desc",
            queryParams,
            ::rowMapper
        )
    }

    fun findById(postId: UUID): PostEntity? {
        return template.query("select * from post where id = :id", MapSqlParameterSource("id", postId), ::rowMapper)
            .firstOrNull()
    }

    fun update(post: PostEntity) {
        template.update(
            "update post set published_at = :publishedAt, title = :title, summary = :summary, original_content = :originalContent, html_content = :htmlContent, friendly_url = :friendlyUrl where id = :id",
            MapSqlParameterSource(
                mapOf(
                    "id" to post.id,
                    "publishedAt" to post.publishedAt?.let(Timestamp::from),
                    "title" to post.title,
                    "summary" to post.summary,
                    "originalContent" to post.originalContent,
                    "htmlContent" to post.htmlContent,
                    "friendlyUrl" to post.friendlyUrl,
                )
            )
        )
    }

    private fun rowMapper(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rowNum: Int) = PostEntity(
        id = UUID.fromString(rs.getString("id")),
        title = rs.getString("title"),
        summary = rs.getString("summary"),
        originalContent = rs.getString("original_content"),
        htmlContent = rs.getString("html_content"),
        friendlyUrl = rs.getString("friendly_url"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        publishedAt = rs.getTimestamp("published_at")?.toInstant(),
    )
}