package mbworld.domain.cafe.view.model

import mbworld.routes.common.AccountData

/**
 * Model for the cafe landing page: `/cafe`.
 *
 * @property account Optional [AccountData] of the user.
 * @property spaces A unit that groups together related sections in the cafe.
 */
data class CafeModel(
    val account: AccountData?,
    val spaces: List<SpaceItem>,
)

/**
 * Describe a single space.
 *
 * @property name The name of the space (e.g., "Lounge", "Bias Corner", "Terrace")
 * @property sections The sections that this space contains.
 */
data class SpaceItem(
    val name: String,
    val sections: List<SectionItem>
)

/**
 * Describe a single cafe section.
 *
 * @property id Unique identifier of the section. This is used for page's slug.
 * @property name Display name of the section (e.g., "Kep1er Discussion")
 * @property description Text describing the purpose of the section.
 * @property topicCounts The amount of topic that exists within this section.
 * @property latestPostData The data of the latest post this section has, if any.
 */
data class SectionItem(
    val id: String,
    val name: String,
    val description: String,
    val topicCounts: Int,
    val latestPostData: LatestSectionPostData?
)

/**
 * @property postId
 * @property postType
 * @property topicTitle
 * @property topicUrl
 * @property authorDisplayName
 * @property authorAvatarUrl
 * @property authorProfileUrl
 * @property postedDate
 */
data class LatestSectionPostData(
    val postId: String,
    val postType: String,
    val topicTitle: String,
    val topicUrl: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String,
    val authorProfileUrl: String,
    val postedDate: Long
)
