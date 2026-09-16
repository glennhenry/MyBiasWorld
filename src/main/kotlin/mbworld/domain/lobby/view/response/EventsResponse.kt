package mbworld.domain.lobby.view.response

import kotlinx.serialization.Serializable

/**
 * Response model for the '/events' route.
 * @property events A list of events data.
 */
@Serializable
data class EventsResponse(
    val events: List<EventData>
)

/**
 * Data of event sent to client.
 *
 * @property text The text data of the event.
 * @property timestamp Epoch millis when the event happened.
 */
@Serializable
data class EventData(
    val text: String,
    val timestamp: Long
)
