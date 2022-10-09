package com.vdt.blog.post

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class PostTagRepository(
    private val template: NamedParameterJdbcTemplate,
) {
    fun saveAll(postTags: List<PostTagEntity>) {
        postTags.forEach {
            template.update(
                "insert into post_tag (post_id, tag_name) values (:postId, :tagName)", MapSqlParameterSource(
                    mapOf(
                        "postId" to it.postId,
                        "tagName" to it.tagName,
                    )
                )
            )
        }
    }

    fun findAllByPostIdIn(postIds: Set<UUID>): Collection<PostTagEntity> {
        return template.query(
            "select * from post_tag where post_id in (:postIds)",
            MapSqlParameterSource("postIds", postIds),
            ::rowMapper
        )
    }

    fun deleteAllPostTags(postId: UUID) {
        template.update("delete from post_tag where post_id = :postId", MapSqlParameterSource("postId", postId))
    }

    fun findAllByTagName(tagName: String): Collection<PostTagEntity> {
        return template.query(
            "select * from post_tag where tag_name = :tagName",
            MapSqlParameterSource("tagName", tagName),
            ::rowMapper
        )
    }

    private fun rowMapper(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rowNum: Int) = PostTagEntity(
        postId = UUID.fromString(rs.getString("post_id")),
        tagName = rs.getString("tag_name"),
    )


}
