package com.vdt.blog

import com.vdt.blog.tag.TagEntity
import com.vdt.blog.tag.TagRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute


@ControllerAdvice
class ControllerAdvice(
    private val tagRepository: TagRepository,
    @Value("\${app.title}") private val appTitle: String,
    @Value("\${app.static-content.section.about}") private val appAboutDescription: String,
) {
    @ModelAttribute("tags")
    fun getTags(): Set<TagEntity> {
        return tagRepository.findAll()
    }

    @ModelAttribute("appTitle")
    fun getAppTitle(): String {
        return appTitle
    }

    @ModelAttribute("appAboutDescription")
    fun getAppAboutDescription(): String {
        return appAboutDescription
    }
}