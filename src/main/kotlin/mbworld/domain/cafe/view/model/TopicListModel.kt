package mbworld.domain.cafe.view.model

import mbworld.domain.cafe.topic.Topic
import mbworld.routes.common.AccountData

/**
 * Model to display the list of topics within a section in the cafe.
 * This uses the `topiclist.html`; URLs examples: `/cafe/kep1er`, `/cafe/kpop`.
 *
 * @property account Optional [AccountData] of the user.
 * @property sectionId The unique identifier of the section. Uses [SectionItem.id].
 * @property sectionName Display name of the section. Uses [SectionItem.name].
 * @property topics The list of topics that this section contains.
 */
data class TopicListModel(
    val account: AccountData?,
    val sectionId: String,
    val sectionName: String,
    val topics: List<TopicListItemData> = emptyList()
)

/**
 * A single topic item within the list of topics in a section.
 *
 * @property topicId Unique identifier of the topic. Uses [Topic.topicId].
 * @property link The link that direct to the topic's page.
 * @property title Title of the topic. Uses [Topic.title].
 * @property authorName The name of author that posted the topic.
 * @property authorAvatarUrl The URL that leads to the author's avatar.
 * @property authorProfileUrl The URL that leads to the author's profile.
 * @property replyCount The amount of reply posted within this topic.
 * @property likesCount The amount of likes this topic has.
 * @property postedDate The date of when this topic was posted. Uses [Topic.postedDate].
 * @property latestPostData The data of the latest post this topic has, if any.
 */
data class TopicListItemData(
    val topicId: String,
    val link: String,
    val title: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val authorProfileUrl: String,
    val replyCount: Int,
    val likesCount: Int,
    val postedDate: Long,
    val latestPostData: LatestPostData?
)

/**
 * @property postId
 * @property postType
 * @property authorDisplayName
 * @property authorAvatarUrl
 * @property authorProfileUrl
 * @property postedDate
 */
data class LatestPostData(
    val postId: String,
    val postType: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String,
    val authorProfileUrl: String,
    val postedDate: Long
)
