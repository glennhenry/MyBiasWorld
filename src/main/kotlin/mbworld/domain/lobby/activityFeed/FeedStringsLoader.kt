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
 * @property format List of format definitions.
 */
@Serializable
data class FeedStringsEntry(
    val type: String,
    val format: List<FormatDefinition>
)

/**
 * A definition of format for the strings translation file.
 *
 * This describes the format of a single part of string with:
 * - A particular [style]
 * - Optional [link] if [style] is "hyperlink"
 * - Optional [color] if [style] is "color"
 * - And the [text] string as the display
 */
@Serializable
data class FormatDefinition(
    val style: String,
    val link: String? = null,
    val color: String? = null,
    val text: String
)
