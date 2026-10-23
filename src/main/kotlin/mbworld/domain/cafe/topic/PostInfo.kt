@file:Suppress("unused", "ConstPropertyName")

package mbworld.domain.cafe.topic

import kotlinx.serialization.Serializable

/**
 * Describes a post:
 * - Whether it's a topic, reply, or a comment by [postType].
 * - The unique [postId] reference.
 */
@Serializable
data class PostInfo(
    val postType: String,
    val postId: String
)

object PostInfoType {
    const val Topic = "topic"
    const val Reply = "reply"
    const val Comment = "comment"
}
