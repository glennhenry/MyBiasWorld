package mbworld.domain.events.model

/**
 * The type for [Event], classifying the source of where event was produced.
 *
 * `EventType` separates between the high-level domain of the platform.
 * Use [Event.subtype] instead for the more detailed source of the event.
 *
 * For example:
 * - `Cafe`: activities that happened around the cafe.
 * - `Games`: activities that happened inside the game environment.
 * - `System`: internal system activities.
 * - `Others`: includes any generic or uncategorized activities.
 */
enum class EventType {
    Cafe, Others
}
