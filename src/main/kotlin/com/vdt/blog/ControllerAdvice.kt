package com.vdt.blog

import com.vdt.blog.tag.TagEntity
import com.vdt.blog.tag.TagRepository
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute


@ControllerAdvice
class ControllerAdvice(
    private val tagRepository: TagRepository,
) {
    @ModelAttribute("tags")
    fun getTags(): Set<TagEntity> {
        return tagRepository.findAll()
    }
}