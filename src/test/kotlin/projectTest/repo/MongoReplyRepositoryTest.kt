package projectTest.repo

import TestCollections
import initMongo
import io.ktor.util.date.*
import kotlinx.coroutines.test.runTest
import mbworld.domain.cafe.reply.Comment
import mbworld.domain.cafe.reply.MongoReplyRepository
import mbworld.domain.cafe.reply.Reply
import testUtils.assertDoesNotFailSuspend
import testUtils.randomString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test operations of [MongoReplyRepository].
 */
class MongoReplyRepositoryTest {
    @Test
    fun `test all`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<Reply>(TestCollections.reply)
        collection.drop()
        mongoDb.createCollection(TestCollections.reply)

        val repo = MongoReplyRepository(collection)

        // setup
        val id = "5ab0980c-e2cb-990a-427a-5ad9b0311b7f"
        val targetReply = Reply(
            replyId = id,
            topicId = "fixedTopicId",
            authorId = "author123",
            content = "content123",
            likes = 0,
            postedDate = 0,
            comments = listOf(
                Comment("comment1", "author1", "hello world 1", 0),
                Comment("comment2", "author2", "hello world 2", 0),
                Comment("comment3", "author3", "hello world 3", 0),
                Comment("comment4", "author4", "hello world 4", 0),
                Comment("comment5", "author5", "hello world 5", 0),
                Comment("comment6", "author6", "hello world 6", 0),
            )
        )
        collection.insertMany(createReply(10, "yesyes") + createReply(10, "fixedTopicId") + targetReply)

        // tests
        // 1. getReply
        assertDoesNotFailSuspend { repo.getReply(id).getOrThrow() }

        // 2. getRepliesUnder
        assertEquals(11, repo.getRepliesUnder("fixedTopicId").getOrThrow().size)

        // 3. getReplyCount
        assertEquals(11, repo.getReplyCount(topicId = "fixedTopicId").getOrThrow())

        // 4. getReplyCounts
        assertTrue {
            val x = repo.getReplyCounts(listOf("fixedTopicId", "yesyes")).getOrThrow()
            x["fixedTopicId"]!! == 11 && x["yesyes"]!! == 10
        }

        // 5. addReply
        assertDoesNotFailSuspend {
            repo.addReply(Reply("asdf", "asdf", "asdf", "asdf", 0, 0, emptyList())).getOrThrow()
        }
        assertDoesNotFailSuspend { repo.getReply("asdf").getOrThrow() }

        // 6. getComments
        assertTrue {
            val x = repo.getComments(id, 3).getOrThrow()
            x.find { it.commentId == "comment1" } != null &&
                    x.find { it.commentId == "comment2" } != null &&
                    x.find { it.commentId == "comment3" } != null
        }

        // 6. addComment
        assertDoesNotFailSuspend {
            repo.addComment(id, comment = Comment("comment7", "author7", "hello world 7", 0)).getOrThrow()
        }
        assertNotNull(repo.getComments(id, 7).getOrThrow().find { it.commentId == "comment7" })

        // 7. incrementLike
        assertDoesNotFailSuspend { repo.incrementLike(id).getOrThrow() }
        assertEquals(1, repo.getReply(id).getOrThrow().likes)

        // 8. decrementLike
        assertDoesNotFailSuspend { repo.decrementLike(id).getOrThrow() }
        assertEquals(0, repo.getReply(id).getOrThrow().likes)
    }

    private fun createReply(amount: Int, topicId: String = randstr()): List<Reply> {
        return List(amount) {
            Reply(randstr(), topicId, randstr(), randstr(), 0, getTimeMillis(), emptyList())
        }
    }

    private val charpool = ('a'..'z').toList()
    private fun randstr(): String {
        return randomString(8, charpool)
    }
}
