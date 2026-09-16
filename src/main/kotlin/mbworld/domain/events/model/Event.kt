package mbworld.domain.events.model

/**
 * Model for event, a representation of activity or actions
 * that happened in the platform.
 *
 * @property type One of the [EventType] enum.
 * @property subtype A more specific details about the type of the event.
 *                   This is a free-form string, but for convention, use a
 *                   format like "<event_type>Event.<subtype>" such as
 *                   "CafeEvent.TopicLiked" or "GameEvent.PlayersAmount".
 *                   An enum that converts to string may be used for this.
 * @property timestamp Epoch millis when the event happened.
 * @property metadata Arbitrary data entries for the event.
 */
data class Event(
    val type: EventType,
    val subtype: String,
    val timestamp: Long,
    val metadata: Map<String, Any?>
)
