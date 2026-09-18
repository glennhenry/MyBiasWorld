package mbworld.domain.activity.model

/**
 * `Activity` describes the occurence of an event in the platform.
 * They are used to record and document over something that happened.
 *
 * An activity has a [source], telling where it's coming from;
 * while the [type] describes the detail about the activity.
 *
 * @property source One of the [ActivitySource] enum.
 * @property type Detail and context about the activity.
 * @property timestamp Epoch millis when the activity happened.
 * @property metadata Arbitrary data entries for the activity.
 */
data class Activity(
    val source: ActivitySource,
    val type: String,
    val timestamp: Long,
    val metadata: Map<String, Any?>
)
