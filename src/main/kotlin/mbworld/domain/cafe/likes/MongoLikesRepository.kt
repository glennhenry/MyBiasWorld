package mbworld.domain.cafe.likes

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import encore.account.FieldUserId
import encore.datastore.runMongoCatching
import encore.datastore.throwIfNothingDeleted
import encore.utils.support.asUnit
import kotlinx.coroutines.flow.associateBy
import kotlinx.coroutines.flow.firstOrNull
import mbworld.mongo.collection.UserId
import org.bson.codecs.pojo.annotations.BsonId

/** `postId` */
val FieldPostId = Likes::postId.name

/** `castedAt` */
val FieldCastedAt = Likes::castedAt.name

class MongoLikesRepository(private val likes: MongoCollection<Likes>) : LikesRepository {
    override suspend fun awaitInit() {}

    override suspend fun isPostLikedBy(
        userId: UserId,
        postId: String
    ): Result<Long?> {
        return runCatching {
            likes.find(
                Filters.and(
                    Filters.eq(FieldUserId, userId),
                    Filters.eq(FieldPostId, postId)
                )
            ).firstOrNull()?.castedAt
        }
    }

    override suspend fun likedPosts(
        userId: UserId,
        postIds: List<String>
    ): Result<Map<String, Long>> {
        return runMongoCatching {
            val map = likes.aggregate<QueryPostIdAndCastedAt>(
                listOf(
                    Aggregates.match(
                        Filters.and(
                            Filters.eq(FieldUserId, userId),
                            Filters.`in`(FieldPostId, postIds)
                        )
                    ),
                    Aggregates.project(
                        Projections.fields(
                            Projections.excludeId(),
                            Projections.include(FieldPostId),
                            Projections.include(FieldCastedAt)
                        )
                    )
                )
            ).associateBy(
                keySelector = { it.postId },
                valueTransform = { it.castedAt }
            )

            return Result.success(map)
        }
    }

    override suspend fun addLike(likes: Likes): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.and(
                Filters.eq(FieldUserId, likes.userId),
                Filters.eq(FieldPostId, likes.postId)
            )

            this.likes.replaceOne(filter, likes, ReplaceOptions().upsert(true))
                .asUnit()
        }
    }

    override suspend fun removeLike(userId: UserId, postId: String): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.and(
                Filters.eq(FieldUserId, userId),
                Filters.eq(FieldPostId, postId)
            )

            this.likes.deleteOne(filter)
                .throwIfNothingDeleted("removeLike") { filter }
        }
    }
}

/**
 * Mongo projection class to query [Likes.castedAt] field.
 */
data class QueryPostIdAndCastedAt(
    @field:BsonId val id: String? = null,
    val postId: String,
    val castedAt: Long
)
