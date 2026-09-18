package mbworld.domain.activity.model

/**
 * `ActivitySource` classifies the domain that produces an activity.
 *
 * While source separates between the various domain of the platform,
 * [Activity.type] is used to describe the context of the activity.
 *
 * For example:
 * - `Cafe`: activities that happened around the cafe.
 * - `Games`: activities that happened inside the game environment.
 * - `Others`: includes any generic or uncategorized activities.
 */
enum class ActivitySource {
    Cafe, Others
}
