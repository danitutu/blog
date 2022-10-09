package com.vdt.blog.post

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vdt.blog.tag.TagEntity
import com.vdt.blog.tag.TagRepository
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class PostService(
    private val postRepository: PostRepository,
    private val tagRepository: TagRepository,
    private val postTagRepository: PostTagRepository,
) {
    private val parser: Parser = Parser.builder().build()
    private val renderer = HtmlRenderer.builder().build()

    @Transactional
    fun createPost(input: CreatePostInput): Either<CreatePostError, Post> {
        val error = validateCreateOrUpdateInput(input)

        if (error != null) return error.left()

        val existsByFriendlyUrl = postRepository.findByFriendlyUrl(input.friendlyUrl).isNotEmpty()

        if (existsByFriendlyUrl) return CreatePostError.FriendlyUrlNotUnique.left()

        val tags = storeNewTagsAndGetAllTags(input.tags)

        val post = PostEntity(
            id = UUID.randomUUID(),
            title = input.title,
            summary = input.summary,
            originalContent = input.content,
            htmlContent = renderHtmlFromMarkdown(input.content),
            friendlyUrl = input.friendlyUrl,
            createdAt = Instant.now(),
            publishedAt = null,
        )

        postRepository.save(post)

        storePostAssociations(tags, post)

        return Post(post, tags).right()
    }

    private fun storePostAssociations(tagsToInsert: Set<TagEntity>, post: PostEntity) {
        val postTags = tagsToInsert.map { PostTagEntity(post.id, it.name) }

        postTagRepository.saveAll(postTags)
    }

    private fun storeNewTagsAndGetAllTags(tagsInput: Set<String>): Set<TagEntity> {
        val tags = tagsInput.map(String::lowercase).map(::TagEntity).toSet()

        val dbTags = tagRepository.findAll().toSet()

        val tagsToInsert = tags - dbTags

        tagRepository.saveAll(tagsToInsert)
        return tags
    }

    private fun validateCreateOrUpdateInput(input: CreatePostInput): CreatePostError? {
        val error = with(input) {
            when {
                title.isBlank() || title.length !in 1..500 -> CreatePostError.InvalidTitle
                summary.isBlank() || summary.length !in 1..1000 -> CreatePostError.InvalidSummary
                content.isBlank() || content.length !in 1..100000 -> CreatePostError.InvalidContent
                friendlyUrl.isBlank() || !friendlyUrl.matches(friendlyUrlRegex) -> CreatePostError.InvalidFriendlyUrl
                tags.isEmpty() || tags.any { it.isBlank() } -> CreatePostError.InvalidTags
                else -> null
            }
        }
        return error
    }

    fun searchPosts(fetchUnpublished: Boolean = false, tagName: String? = null): List<Post> {
        val tags = tagName ?. let { postTagRepository.findAllByTagName(tagName) }
        val posts = postRepository.search(fetchUnpublished = fetchUnpublished, tags = tags)
        return createPostsFromEntities(posts)
    }

    private fun createPostsFromEntities(posts: Collection<PostEntity>): List<Post> {
        val postIdTagIds = if (posts.isNotEmpty()) {
            val postIds = posts.map { it.id }.toSet()
            val postTags = postTagRepository.findAllByPostIdIn(postIds)
            postTags.groupBy { it.postId }
        } else emptyMap()
        return posts.map { post ->
            Post(
                post,
                postIdTagIds[post.id]?.map { TagEntity(it.tagName) }?.toSet() ?: emptySet()
            )
        }
    }

    private fun renderHtmlFromMarkdown(value: String): String {
        val document = parser.parse(value)
        return renderer.render(document)
    }

    fun publishPost(postId: UUID): Either<PublishPostError, Unit> {
        val post = postRepository.findById(postId)
        return when {
            post == null -> PublishPostError.PostNotFound.left()
            post.publishedAt != null -> PublishPostError.PostAlreadyPublished.left()
            else -> postRepository.update(post.copy(publishedAt = Instant.now())).right()
        }
    }

    fun findById(postId: UUID): Post? {
        val postEntity = postRepository.findById(postId) ?: return null
        return createPostsFromEntities(listOf(postEntity)).singleOrNull()
    }

    fun unpublishPost(postId: UUID): Either<UnpublishPostError, Unit> {
        val post = postRepository.findById(postId)
        return when {
            post == null -> UnpublishPostError.PostNotFound.left()
            post.publishedAt == null -> UnpublishPostError.PostAlreadyUnpublished.left()
            else -> postRepository.update(post.copy(publishedAt = null)).right()
        }
    }

    @Transactional
    fun updatePost(postId: UUID, input: CreatePostInput): Either<CreatePostError, Post> {
        val error = validateCreateOrUpdateInput(input)

        if (error != null) {
            return error.left()
        }

        val tags = storeNewTagsAndGetAllTags(input.tags)

        val post = postRepository.findById(postId)!! // todo not found

        val existsByFriendlyUrl = postRepository.findByFriendlyUrl(input.friendlyUrl).any { it.id != post.id }

        if (existsByFriendlyUrl) return CreatePostError.FriendlyUrlNotUnique.left()

        val newEntity = post.copy(
            title = input.title,
            summary = input.summary,
            originalContent = input.content,
            htmlContent = renderHtmlFromMarkdown(input.content),
            friendlyUrl = input.friendlyUrl,
        )
        postRepository.update(newEntity)

        postTagRepository.deleteAllPostTags(postId)

        storePostAssociations(tags, newEntity)

        return Post(newEntity, tags).right()
    }

    sealed interface PublishPostError {
        object PostNotFound : PublishPostError
        object PostAlreadyPublished : PublishPostError
    }

    sealed interface UnpublishPostError {
        object PostNotFound : UnpublishPostError
        object PostAlreadyUnpublished : UnpublishPostError
    }

    data class CreatePostInput(
        val title: String,
        val summary: String,
        val content: String,
        val friendlyUrl: String,
        val tags: Set<String>,
    )

    sealed interface CreatePostError {
        object InvalidContent : CreatePostError
        object InvalidTitle : CreatePostError
        object InvalidSummary : CreatePostError
        object InvalidFriendlyUrl : CreatePostError
        object InvalidTags : CreatePostError
        object FriendlyUrlNotUnique : CreatePostError
    }

    companion object {
        private val friendlyUrlRegex = "^[a-z0-9-]{1,100}$".toRegex()
    }
}
