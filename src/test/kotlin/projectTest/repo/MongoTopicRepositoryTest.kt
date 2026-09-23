package projectTest.repo

import TestCollections
import encore.datastore.DocumentNotFoundException
import encore.utils.identifier.shortUuid
import initMongo
import io.ktor.util.date.*
import kotlinx.coroutines.test.runTest
import mbworld.domain.cafe.topic.MongoTopicRepository
import mbworld.domain.cafe.topic.Topic
import testUtils.assertDoesNotFailSuspend
import testUtils.randomString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Test operations of [MongoTopicRepository].
 */
class MongoTopicRepositoryTest {
    @Test
    fun `test all`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<Topic>(TestCollections.topic)
        collection.drop()
        mongoDb.createCollection(TestCollections.topic)

        val repo = MongoTopicRepository(collection)

        // setup
        val id = "5ab0980c-e2cb-990a-427a-5ad9b0311b7f"
        val targetTopic = Topic(id, "sectionId123", "title123", "author123", "content123", 0, 0)
        collection.insertMany(createTopic(20) + targetTopic)

        // tests
        // 1. getTopic
        assertDoesNotFailSuspend { repo.getTopic(id).getOrThrow() }

        // 2. getTopicByShortId
        assertEquals(id, repo.getTopicByShortId(id.shortUuid()).getOrThrow().topicId)

        // 3. getFullTopicId
        assertEquals(id, repo.getFullTopicId(id.shortUuid()).getOrThrow())

        // 4. getTopics
        assertNotNull(
            repo.getTopics().getOrThrow().find { it.topicId == targetTopic.topicId }
        )

        // 5. getTopicsOfSection
        assertNotNull(
            repo.getTopicsOfSection("sectionId123").getOrThrow().find { it.sectionId == targetTopic.sectionId }
        )

        // 6. getTopicsCountForEachSection
        assertEquals(1, repo.getTopicsCountForEachSection().getOrThrow()["sectionId123"])

        // 7. addTopic
        val t = Topic("asdf", "asdf", "asdf", "asdf", "asdf", 0, 0)
        assertDoesNotFailSuspend { repo.addTopic(t).getOrThrow() }
        assertDoesNotFailSuspend { repo.getTopic("asdf").getOrThrow() }

        // 8. deleteTopic
        assertDoesNotFailSuspend { repo.deleteTopic("asdf").getOrThrow() }
        assertFailsWith<DocumentNotFoundException> { repo.getTopic("asdf").getOrThrow() }

        // 9. getTopicLikes
        assertEquals(0, repo.getTopicLikes(id).getOrThrow())

        // 9. incrementLike
        assertDoesNotFailSuspend { repo.incrementLike(id).getOrThrow() }
        assertEquals(1, repo.getTopic(id).getOrThrow().likes)

        // 10. decrementLike
        assertDoesNotFailSuspend { repo.decrementLike(id).getOrNull() }
        assertEquals(0, repo.getTopic(id).getOrThrow().likes)
    }

    private fun createTopic(amount: Int): List<Topic> {
        return List(amount) {
            Topic(randstr(), randstr(), randstr(), randstr(), randstr(), 0, getTimeMillis())
        }
    }

    private val charpool = ('a'..'z').toList()
    private fun randstr(): String {
        return randomString(8, charpool)
    }
}
