package mbworld.domain.cafe.likes

import kotlinx.serialization.Serializable
import mbworld.domain.cafe.reply.Reply
import mbworld.domain.cafe.topic.Topic
import mbworld.mongo.collection.UserId

/**
 * Represent a like relationship between a user with a cafe post
 * which could either be a topic or reply.
 *
 * @property userId The [UserId] liking the post.
 * @property postId Unique identifier of the liked post.
 *                  References [Topic.topicId] or [Reply.replyId].
 * @property castedAt Timestamp of when the like was casted.
 */
@Serializable
data class Likes(
    val userId: UserId,
    val postId: String,
    val castedAt: Long
)
