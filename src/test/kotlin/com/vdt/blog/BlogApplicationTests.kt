package com.vdt.blog

import arrow.core.Either
import com.vdt.blog.post.Post
import com.vdt.blog.post.PostService
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.*

@SpringBootTest
class BlogApplicationTests {

    @Autowired
    private lateinit var postService: PostService

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @BeforeEach
    fun beforeEach() {
        jdbcTemplate.update("delete from post_tag", mapOf<String, String>())
        jdbcTemplate.update("delete from post", mapOf<String, String>())
        jdbcTemplate.update("delete from tag", mapOf<String, String>())
    }

    @Test
    fun `createPost should create post`() {
        val friendlyUrl = UUID.randomUUID().toString()

        val post = createPost(friendlyUrl = friendlyUrl)

        val newPost = post.getRightValue()
        val dbPost = postService.findById(newPost.id)!!

        dbPost.title shouldBe "title"
        dbPost.summary shouldBe "summary"
        dbPost.originalContent shouldBe "content"
        dbPost.friendlyUrl shouldBe friendlyUrl
        dbPost.tags shouldContainExactly setOf("tag1", "tag2")
        dbPost shouldBe newPost
    }

    @Test
    fun `createPost should transform markdown into html`() {
        val post = createPost(content = "con*ten*t")

        val newPost = post.getRightValue()
        val dbPost = postService.findById(newPost.id)!!

        dbPost.originalContent shouldBe "con*ten*t"
        dbPost.originalContent shouldBe newPost.originalContent
        dbPost.htmlContent shouldBe "<p>con<em>ten</em>t</p>\n"
    }

    @Test
    fun `createPost should return FriendlyUrlNotUnique`() {
        val post = createPost()
        val post2 = createPost(friendlyUrl = (post as Either.Right).value.friendlyUrl)

        post2 shouldBeLeft PostService.CreatePostError.FriendlyUrlNotUnique
    }

    @Test
    fun `createPost should return InvalidFriendlyUrl when input contains spaces`() {
        val post = createPost(friendlyUrl = "f url")

        post shouldBeLeft PostService.CreatePostError.InvalidFriendlyUrl
    }

    @Test
    fun `updatePost should return InvalidFriendlyUrl when input contains spaces`() {
        val post = createPost().getRightValue()

        val res = updatePost(friendlyUrl = "f url", postId = post.id)

        res shouldBeLeft PostService.CreatePostError.InvalidFriendlyUrl
    }

    @Test
    fun `updatePost should update all input fields`() {
        val post = createPost().getRightValue()

        updatePost(postId = post.id).getRightValue()

        val res = postService.findById(post.id)!!

        res shouldBe Post(
            id = post.id,
            title = "title2",
            summary = "summary2",
            originalContent = "content2",
            htmlContent = "<p>content2</p>\n",
            friendlyUrl = "friendly-url-2",
            createdAt = post.createdAt,
            publishedAt = null,
            tags = setOf("tag3", "tag4")
        )
    }

    @Test
    fun `updatePost should return FriendlyUrlNotUnique if there's another post with same value`() {
        val post = createPost().getRightValue()
        val post2 = createPost().getRightValue()

        val res = updatePost(postId = post.id, friendlyUrl = post2.friendlyUrl)

        res shouldBeLeft PostService.CreatePostError.FriendlyUrlNotUnique
    }

    @Test
    fun `searchPosts should return items in descendent order by created at by default`() {
        repeat(100) {
            val post = createPost().getRightValue()
            postService.publishPost(post.id)
        }

        val posts = postService.searchPosts()

        val createdAtValues = posts.map { it.createdAt }

        createdAtValues.size shouldBe 100

        createdAtValues shouldContainExactly createdAtValues.sortedDescending()
    }

    @Test
    fun `searchPosts should return published items by default`() {
        val post1 = createPost()
        createPost()

        postService.publishPost(post1.getRightValue().id)

        val postAfterPublish = postService.findById(post1.getRightValue().id)

        val posts = postService.searchPosts()

        posts shouldContainExactly listOf(postAfterPublish)
    }

    @Test
    fun `searchPosts should return published and unpublished items when fetchUnpublished = true`() {
        val post1 = createPost().getRightValue()
        val post2 = createPost().getRightValue()

        postService.publishPost(post2.id)

        val post2AfterPublish = postService.findById(post2.id)

        val posts = postService.searchPosts(fetchUnpublished = true)

        posts[0] shouldBe post2AfterPublish
        posts[1] shouldBe post1
    }

    @Test
    fun `searchPosts should return published items when fetchUnpublished = false`() {
        val post1 = createPost()
        createPost()

        postService.publishPost(post1.getRightValue().id)

        val postAfterPublish = postService.findById(post1.getRightValue().id)

        val posts = postService.searchPosts(fetchUnpublished = false)

        posts shouldContainExactly listOf(postAfterPublish)
    }

    @Test
    fun `searchPosts should return only items with tag2 when tagName = tag2`() {
        val post1 = createPost()
        createPost(tags = setOf("tag3"))

        postService.publishPost(post1.getRightValue().id)

        val postAfterPublish = postService.findById(post1.getRightValue().id)

        val posts = postService.searchPosts(tagName = "tag2")

        posts shouldContainExactly listOf(postAfterPublish)
    }

    private fun createPost(
        friendlyUrl: String = UUID.randomUUID().toString().lowercase(),
        content: String = "content",
        tags: Set<String> = setOf("tag1", "tag2")
    ): Either<PostService.CreatePostError, Post> {
        return postService.createPost(
            PostService.CreatePostInput(
                "title",
                "summary",
                content,
                friendlyUrl,
                tags,
            )
        )
    }

    private fun updatePost(
        friendlyUrl: String = "friendly-url-2",
        content: String = "content2",
        tags: Set<String> = setOf("tag3", "tag4"),
        postId: UUID,
        title: String = "title2",
        summary: String = "summary2",
    ): Either<PostService.CreatePostError, Post> {
        return postService.updatePost(
            postId,
            PostService.CreatePostInput(
                title,
                summary,
                content,
                friendlyUrl,
                tags,
            )
        )
    }

    private fun <A, B> Either<A, B>.getLeftValue(): A = (this as Either.Left).value

    private fun <A, B> Either<A, B>.getRightValue(): B = (this as Either.Right).value

    private infix fun Post.shouldBe(expected: Post) {
        this.id shouldBe expected.id
        this.title shouldBe expected.title
        this.summary shouldBe expected.summary
        this.originalContent shouldBe expected.originalContent
        this.htmlContent shouldBe expected.htmlContent
        this.createdAt.toEpochMilli() shouldBe expected.createdAt.toEpochMilli()
        this.friendlyUrl shouldBe expected.friendlyUrl
        this.publishedAt shouldBe expected.publishedAt
        this.tags shouldBe expected.tags
    }

}
