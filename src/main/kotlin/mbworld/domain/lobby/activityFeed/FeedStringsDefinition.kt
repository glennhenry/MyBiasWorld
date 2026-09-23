package mbworld.domain.lobby.activityFeed

import encore.definition.DataDefinition

/**
 * Defines the strings used for activity feed translation.
 * @param entries Entries of `feed_strings.json` produced by [FeedStringsEntry].
 */
class FeedStringsDefinition(entries: List<FeedStringsEntry>) : DataDefinition {
    // map of each activity type into its text formatting
    private val strings: Map<String, String> = entries.associateBy({ it.type }, { it.format })

    /**
     * Get text format for [type].
     * @throws IllegalStateException if type doesn't exist.
     */
    fun get(type: String): String {
        return strings[type]
            ?: error("Type '$type' doesn't exist in FeedStringsDefinition. " +
                    "Please check feed_strings.json")
    }
}
