package mbworld.domain.lobby.view.response

import kotlinx.serialization.Serializable
import mbworld.domain.lobby.activityFeed.ActivityFeedData

/**
 * Response model for the '/activity' route.
 * @property activities A list of activities data.
 */
@Serializable
data class ActivityResponse(
    val activities: List<ActivityFeedData>
)
