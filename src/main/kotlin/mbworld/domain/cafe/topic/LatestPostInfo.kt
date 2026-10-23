@file:Suppress("unused", "ConstPropertyName")

package mbworld.domain.cafe.topic

import kotlinx.serialization.Serializable

/**
 * Describes a post:
 * - Whether it's a topic, reply, or a comment by [postType].
 * - The unique [postId] reference, [authorId], and [postedDate].
 */
@Serializable
data class LatestPostInfo(
    val postType: String,
    val postId: String,
    val authorId: String,
    val postedDate: Long
)

object PostInfoType {
    const val Reply = "reply"
    const val Comment = "comment"
}
