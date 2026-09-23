package mbworld.domain.lobby.view.response

import kotlinx.serialization.Serializable

/**
 * Response model for the '/activity' route.
 * @property activities A list of activities data.
 */
@Serializable
data class ActivityResponse(
    val activities: List<LobbyActivityData>
)

/**
 * Data of activity sent to client.
 *
 * @property text The text data of the activity.
 * @property timestamp Epoch millis when the activity happened.
 */
@Serializable
data class LobbyActivityData(
    val text: String,
    val timestamp: Long
)
