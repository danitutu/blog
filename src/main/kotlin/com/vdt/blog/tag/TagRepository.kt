package com.vdt.blog.tag

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class TagRepository(
    private val template: NamedParameterJdbcTemplate,
) {
    @Transactional
    fun saveAll(tagsToInsert: Collection<TagEntity>) {
        tagsToInsert.forEach {
            template.update(
                "insert into tag (name) values (:name)",
                MapSqlParameterSource("name", it.name),
            )
        }
    }

    fun findAll(): Set<TagEntity> {
        return template.query("select * from tag", MapSqlParameterSource(), RowMapper { rs, _ ->
            TagEntity(rs.getString("name"))
        }).toSet()
    }
}