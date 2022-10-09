package com.vdt.blog

import arrow.core.Either
import com.vdt.blog.post.PostService
import com.vdt.blog.post.PostService.CreatePostError
import org.commonmark.node.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.util.*
import javax.servlet.http.HttpServletResponse


@Controller
class AppController(
    private val postService: PostService,
) {
    private val logger = LoggerFactory.getLogger(AppController::class.java)

    @GetMapping("", "/", "index.html", "/home", "/posts")
    fun home(model: Model): String {
        val posts = postService.searchPosts()
        model.addAttribute("posts", posts)
        return "home"
    }

    @GetMapping("/tags/{tagName}")
    fun viewTagPosts(
        model: Model,
        @PathVariable tagName: String,
    ): String {
        val posts = postService.searchPosts(tagName = tagName)
        model.addAttribute("posts", posts)
        model.addAttribute("tagName", tagName)
        return "home"
    }

    @GetMapping("/posts/{friendlyUrl}")
    fun postDetails(model: Model, @PathVariable friendlyUrl: String): String {
        val post = postService.searchPosts().firstOrNull { it.friendlyUrl == friendlyUrl }
            ?: return "not-found"
        model.addAttribute("post", post)
        return "post"
    }

    @GetMapping("/admin")
    fun admin(model: Model): String {
        return "admin/admin"
    }

    @GetMapping("/admin/backup")
    fun getDatabaseBackup(
        model: Model,
        response: HttpServletResponse,
        @Value("\${app.database.storage.path}") databaseFilePath: String,
    ) {
        val file = File(databaseFilePath)
        response.contentType = "application/octet-stream"
        val headerKey = "Content-Disposition"
        val headerValue =
            "attachment; filename = " + Instant.now().atOffset(ZoneOffset.UTC).toLocalDateTime().toString()
        response.setHeader(headerKey, headerValue)
        response.outputStream.use { outputStream -> outputStream.write(file.readBytes()) }
    }

    @GetMapping("/admin/posts")
    fun adminPosts(model: Model): String {
        model.addAttribute("posts", postService.searchPosts(fetchUnpublished = true))
        return "admin/posts/list"
    }

    @GetMapping("/admin/posts/create")
    fun adminPostsCreate(model: Model): String {
        model.addAttribute("input", PostService.CreatePostInput("", "", "", "", emptySet()))
        model.addAttribute("action", "/admin/posts/create")
        return "/admin/posts/create-edit"
    }

    @PostMapping("/admin/posts/create")
    fun adminPostsCreateDo(
        @ModelAttribute("input") input: PostService.CreatePostInput,
        model: Model,
        bindingResult: BindingResult
    ): String {
        model.addAttribute("action", "/admin/posts/create")
        return when (val result = postService.createPost(input)) {
            is Either.Left -> handleCreateOrUpdatePostError(result, bindingResult)
            is Either.Right -> "redirect:/admin/posts"
        }
    }

    @GetMapping("/admin/posts/{postId}/edit")
    fun adminPostEdit(
        @PathVariable postId: UUID,
        model: Model,
    ): String {
        postService.findById(postId)?.let { post ->
            model.addAttribute(
                "input", PostService.CreatePostInput(
                    post.title,
                    post.summary,
                    post.originalContent,
                    post.friendlyUrl,
                    post.tags,
                )
            )
        }
        model.addAttribute("action", "/admin/posts/$postId/edit")
        return "/admin/posts/create-edit"
    }

    @PostMapping("/admin/posts/{postId}/edit")
    fun adminPostEditDo(
        @PathVariable postId: UUID,
        @ModelAttribute("input") input: PostService.CreatePostInput,
        model: Model,
        bindingResult: BindingResult,
    ): String {
        model.addAttribute("action", "/admin/posts/$postId/edit")
        return when (val result = postService.updatePost(postId, input)) {
            is Either.Left -> handleCreateOrUpdatePostError(result, bindingResult)
            is Either.Right -> "redirect:/admin/posts"
        }
    }

    @PostMapping("/admin/posts/{postId}/publish")
    fun adminPostPublish(
        @PathVariable postId: UUID,
    ): String {
        return when (val result = postService.publishPost(postId)) {
            is Either.Left -> when (result.value) {
                PostService.PublishPostError.PostNotFound -> "not-found"
                PostService.PublishPostError.PostAlreadyPublished -> throw IllegalStateException("Post '$postId' is already published.")
            }

            is Either.Right -> "redirect:/admin/posts"
        }
    }

    @PostMapping("/admin/posts/{postId}/unpublish")
    fun adminPostUnpublish(
        @PathVariable postId: UUID,
    ): String {
        return when (val result = postService.unpublishPost(postId)) {
            is Either.Left -> when (result.value) {
                PostService.UnpublishPostError.PostNotFound -> "not-found"
                PostService.UnpublishPostError.PostAlreadyUnpublished -> throw IllegalStateException("Post '$postId' is already unpublished.")
            }

            is Either.Right -> "redirect:/admin/posts"
        }
    }

    private fun handleCreateOrUpdatePostError(
        result: Either.Left<CreatePostError>,
        bindingResult: BindingResult
    ): String {
        logger.warn("Validation failed. Reason: $result.value")
        when (result.value) {
            CreatePostError.InvalidContent ->
                createError(bindingResult, "content", "Invalid content.")

            CreatePostError.InvalidFriendlyUrl ->
                createError(bindingResult, "friendlyUrl", "Invalid friendly URL.")

            CreatePostError.InvalidTags ->
                createError(bindingResult, "tags", "Invalid tags.")

            CreatePostError.InvalidTitle ->
                createError(bindingResult, "title", "Invalid title.")

            CreatePostError.InvalidSummary ->
                createError(bindingResult, "summary", "Invalid summary.")

            CreatePostError.FriendlyUrlNotUnique ->
                createError(bindingResult, "friendlyUrl", "Friendly URL is not unique.")
        }
        return "admin/posts/create-edit"
    }

    private fun createError(bindingResult: BindingResult, field: String, message: String) {
        bindingResult.addError(FieldError("input", field, message))
    }
}