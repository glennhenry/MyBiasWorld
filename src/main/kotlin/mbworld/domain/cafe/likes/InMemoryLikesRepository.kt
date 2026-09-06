package mbworld.domain.cafe.likes

import encore.datastore.DocumentNotFoundException
import mbworld.mongo.collection.UserId

class InMemoryLikesRepository(
    private val likes: MutableList<Likes> = mutableListOf()
) : LikesRepository {
    override suspend fun awaitInit() {}

    override suspend fun isPostLikedBy(
        userId: UserId,
        postId: String
    ): Result<Long> {
        val likes = likes.find { it.userId == userId && it.postId == postId }
            ?: return Result.failure(DocumentNotFoundException("Relationship of userId=$userId with postId=$postId is not found."))
        return Result.success(likes.castedAt)
    }

    override suspend fun likedPosts(
        userId: UserId,
        postIds: List<String>
    ): Result<Map<String, Long>> {
        val map = mutableMapOf<String, Long>()
        for ((userId1, postId) in likes) {
            if (userId1 == userId && postId in postIds) {
                map[postId] = map.getOrDefault(postId, 0) + 1
            }
        }

        return Result.success(map)
    }

    override suspend fun addLike(likes: Likes): Result<Unit> {
        if (this.likes.find {
                it.userId == likes.userId && it.postId == likes.postId
            } != null) {
            this.likes.add(likes)
        }
        return Result.success(Unit)
    }

    override suspend fun removeLike(userId: UserId, postId: String): Result<Unit> {
        this.likes.removeIf { it.userId == userId && it.postId == postId }
        return Result.success(Unit)
    }
}
