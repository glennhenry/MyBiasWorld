package mbworld.domain.lobby.activityFeed

import encore.definition.DataDefinition
import encore.definition.FileDataLoader
import encore.definition.FileDataSource
import encore.serialization.JSON
import kotlinx.serialization.Serializable

/**
 * Loader for `feed_strings.json`.
 */
class FeedStringsLoader : FileDataLoader {
    // FileDataSource should be JsonDataSource
    override fun produce(source: FileDataSource): List<DataDefinition> {
        val strings = JSON.decode<List<FeedStringsEntry>>(source.readText())
        return listOf(FeedStringsDefinition(strings))
    }
}

/**
 * Represent an entry in the `feed_strings.json`.
 * @property type The corresponding type of the activity for this string entry.
 * @property format The text format.
 */
@Serializable
data class FeedStringsEntry(
    val type: String,
    val format: String
)
