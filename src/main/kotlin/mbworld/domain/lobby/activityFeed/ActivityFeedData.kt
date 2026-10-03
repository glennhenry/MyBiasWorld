package mbworld.domain.lobby.activityFeed

import kotlinx.serialization.Serializable
import mbworld.domain.activity.model.Activity

/**
 * A materialized form of [Activity] in the model of lobby activity feed.
 * This model of activity is ready to be send to client for display in the lobby.
 *
 * @property timestamp Epoch milliseconds of when the activity happened.
 * @property format Format details of the activity.
 */
@Serializable
data class ActivityFeedData(
    val timestamp: Long,
    val format: List<FormatDefinition>
)
