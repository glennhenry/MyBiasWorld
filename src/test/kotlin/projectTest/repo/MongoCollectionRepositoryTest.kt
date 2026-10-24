package projectTest.repo

import TestCollections
import initMongo
import kotlinx.coroutines.test.runTest
import mbworld.domain.cafe.collection.LatestSectionPostInfo
import mbworld.domain.cafe.collection.MongoCollectionRepository
import mbworld.domain.cafe.collection.Section
import mbworld.domain.cafe.collection.Space
import kotlin.test.Test
import kotlin.test.assertEquals

class MongoCollectionRepositoryTest {
    @Test
    fun `test all`() = runTest {
        val mongoDb = initMongo()
        val spaceCol = mongoDb.getCollection<Space>(TestCollections.spaces)
        val sectionCol = mongoDb.getCollection<Section>(TestCollections.sections)
        sectionCol.drop()
        mongoDb.createCollection(TestCollections.sections)

        val repo = MongoCollectionRepository(spaceCol, sectionCol)

        // setup
        sectionCol.insertOne(Section("abc", "1", "1", "1", 1, 0, LatestSectionPostInfo("asdf", "", "", "", 0)))

        // tests
        // 1. getSection
        assertEquals("abc", repo.getSection("abc").getOrThrow().id)

        // 2. incrementTopicCount
        repo.incrementTopicCount("abc").getOrThrow()
        assertEquals(1, repo.getSection("abc").getOrThrow().topicCounts)

        // 3. decrementTopicCount
        repo.decrementTopicCount("abc").getOrThrow()
        assertEquals(0, repo.getSection("abc").getOrThrow().topicCounts)

        // 4. getLatestPost
        assertEquals("asdf", repo.getLatestPost("abc").getOrThrow().postType)

        // 5. updateLatestPost
        repo.updateLatestPost("abc", LatestSectionPostInfo("defg", "", "", "", 0))
        assertEquals("defg", repo.getLatestPost("abc").getOrThrow().postType)
    }
}
