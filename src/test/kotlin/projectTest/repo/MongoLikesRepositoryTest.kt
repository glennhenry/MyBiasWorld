package projectTest.repo

import TestCollections
import encore.utils.identifier.Ids
import initMongo
import kotlinx.coroutines.test.runTest
import mbworld.domain.cafe.likes.Likes
import mbworld.domain.cafe.likes.MongoLikesRepository
import testUtils.assertDoesNotFailSuspend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MongoLikesRepositoryTest {
    @Test
    fun `test all`() = runTest {
        val mongoDb = initMongo()
        val collection = mongoDb.getCollection<Likes>(TestCollections.likes)
        collection.drop()
        mongoDb.createCollection(TestCollections.likes)

        val repo = MongoLikesRepository(collection)

        // setup
        val uid = "5ab0980c-e2cb-990a-427a-5ad9b0311b7a"
        val pid = "5ab0980c-e2cb-990a-427a-5ad9b0311b7b"
        val pidNotLiked = "5ab0980c-e2cb-990a-427a-5ad9b0311b7x"
        collection.insertMany(createLike(3) + createLike(10, pid) + createLike(10, pidNotLiked) + Likes(uid, pid, 123))

        // tests
        // 1. isPostLikedBy
        assertEquals(123, repo.isPostLikedBy(uid, pid).getOrThrow())

        // 2. addLike
        val pid2 = "5ab0980c-e2cb-990a-427a-5ad9b0311b7c"
        val targetLikes = Likes(uid, pid2, 456)
        assertDoesNotFailSuspend { repo.addLike(targetLikes).getOrThrow() }
        assertEquals(456, repo.isPostLikedBy(uid, pid2).getOrThrow())

        // 3. likedPosts
        assertTrue {
            val result = repo.likedPosts(uid, listOf(pid, pid2, pidNotLiked)).getOrThrow()
            result[pid]?.equals(123L) == true &&
                    result[pid2]?.equals(456L) == true &&
                    result.keys.size == 2
        }

        // 4. removeLike
        assertDoesNotFailSuspend { repo.removeLike(uid, pid2).getOrThrow() }
        assertNull(repo.isPostLikedBy(uid, pid2).getOrThrow())
    }

    private fun createLike(amount: Int, postId: String? = null): List<Likes> {
        return List(amount) {
            Likes(Ids.uuid(), postId ?: Ids.uuid(), 0)
        }
    }
}
