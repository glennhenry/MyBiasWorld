package mbworld.domain.cafe.collection

import kotlinx.serialization.Serializable
import mbworld.domain.cafe.topic.LatestPostInfo

/**
 * Refer to the **section** concept in cafe.
 *
 * Represent a discussion subject within a [Space].
 * A section restrict users to only discuss the relevant subject.
 *
 * @property id Unique string identifier of the section.
 *              This will also be used as URL.
 * @property spaceId Represent the space this section belongs to.
 *                   Refer to an existing [Space.id].
 * @property name Display name of the section.
 * @property order The order of how each sections are displayed within the same space.
 * @property topicCounts The amount of topic this section have.
 * @property latestPost The information about the latest post (topic or reply) this section has.
 */
@Serializable
data class Section(
    val id: String,
    val spaceId: String,
    val name: String,
    val description: String,
    val order: Int,

    val topicCounts: Int,
    val latestPost: LatestPostInfo?,
)
