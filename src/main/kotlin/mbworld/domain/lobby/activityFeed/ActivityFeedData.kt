package mbworld.domain.lobby.activityFeed

import mbworld.domain.activity.model.Activity

/**
 * A materialized form of [Activity] in the model of lobby activity feed.
 * This model of activity is ready for display in the lobby.
 *
 * @property timestamp Epoch milliseconds of when the activity happened.
 * @property text Display text of the activity.
 */
data class ActivityFeedData(
    val timestamp: Long,
    val text: String
)
