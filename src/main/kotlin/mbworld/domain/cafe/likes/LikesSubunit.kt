package mbworld.domain.cafe.likes

import encore.fancam.Fancam
import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import encore.utils.types.Outcome
import encore.utils.types.Report
import encore.utils.types.toOutcome
import encore.utils.types.toReport
import mbworld.mongo.collection.UserId
import mbworld.utils.peek

/**
 * Server subunits that handles [Likes] concerns from [LikesRepository].
 */
class LikesSubunit(private val likesRepository: LikesRepository) : Subunit<ServerScope> {

    /**
     * Returns an [Outcome] containing a timestamp of when the post
     * was liked at.
     *
     * - [Outcome.Fail] when there is internal repository error.
     * - [Outcome.Ok] with the timestamp, or `null` if the post is not liked.
     */
    suspend fun isPostLikedBy(userId: UserId, postId: String): Outcome<Long?> {
        return likesRepository.isPostLikedBy(userId, postId)
            .onFailure {
                Fancam.error(it, "likes") {
                    "isPostLikedBy query failed for userId=$userId and postId=$postId"
                }
            }
            .toOutcome { castedAt -> return Outcome.Ok(castedAt) }
    }


    suspend fun likedPosts(userId: UserId, postIds: List<String>): Outcome<Map<String, Long>> {
        return likesRepository.likedPosts(userId, postIds)
            .onFailure {
                Fancam.error(it, "likes") {
                    "likedPosts query failed for userId=$userId and postIds(3)=${postIds.peek(3)}"
                }
            }
            .toOutcome { map -> return Outcome.Ok(map) }
    }


    suspend fun addLike(likes: Likes): Report {
        return likesRepository.addLike(likes)
            .onFailure {
                Fancam.error(it, "likes") {
                    "addLike failed for likes=$likes"
                }
            }
            .toReport()
    }

    suspend fun removeLike(userId: UserId, postId: String): Report {
        return likesRepository.removeLike(userId, postId)
            .onFailure {
                Fancam.error(it, "likes") {
                    "removeLike failed for userId=$userId and postId=$postId"
                }
            }
            .toReport()
    }

    override suspend fun debut(scope: ServerScope): Result<Unit> {
        return runCatching { }
    }

    override suspend fun disband(scope: ServerScope): Result<Unit> {
        return runCatching { }
    }
}
