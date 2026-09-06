package mbworld.domain.cafe.likes

import mbworld.mongo.collection.UserId

/**
 * Repository for [Likes] collection.
 */
interface LikesRepository {
    /**
     * Ensures the repository is fully initialized.
     *
     * Use this for repository initialization that may utilize suspendable code.
     */
    suspend fun awaitInit()

    /**
     * Find out whether [userId] liked [postId].
     *
     * Returns:
     * - [Result.success] with the [Likes.castedAt] value or `null` if no relationship
     *   between [userId] and [postId] is found.
     * - [Result.failure] if other error occurs while retrieving the data.
     */
    suspend fun isPostLikedBy(userId: UserId, postId: String): Result<Long?>

    /**
     * Find out the like state of all posts in [postIds] for [userId].
     *
     * Returns:
     * - [Result.success] with a map of each `postId` to the value of [Likes.castedAt].
     *   If any provided `postId` is not available in the map, it means the `postId` is not found.
     * - [Result.failure] if other error occurs while retrieving the data.
     */
    suspend fun likedPosts(userId: UserId, postIds: List<String>): Result<Map<String, Long>>

    /**
     * Add a like to the post identified by [Likes.postId] for [Likes.userId].
     *
     * Returns:
     * - [Result.success] if the operation succeeded.
     * - [Result.failure] if other error occurs while retrieving the data.
     */
    suspend fun addLike(likes: Likes): Result<Unit>

    /**
     * Remove a like to the post identified by [Likes.postId] for [Likes.userId].
     *
     * Returns:
     * - [Result.success] if the operation succeeded.
     * - [Result.failure] if other error occurs while retrieving the data.
     */
    suspend fun removeLike(userId: UserId, postId: String): Result<Unit>
}
