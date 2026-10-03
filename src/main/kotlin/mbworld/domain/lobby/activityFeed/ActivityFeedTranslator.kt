package mbworld.domain.lobby.activityFeed

import encore.definition.DataReference
import encore.fancam.Fancam
import mbworld.domain.activity.model.Activity

/**
 * The translator for activity feed.
 *
 * It translates raw [Activity] model into textual data for
 * display in the lobby's activity feed.
 *
 * It depends on [FeedStringsDefinition].
 */
class ActivityFeedTranslator {
    // warning, don't use definition until dataReference is initialized on Application.kt
    private val definition by lazy { DataReference.get<FeedStringsDefinition>() }

    fun translate(activity: Activity): ActivityFeedData {

        return ActivityFeedData(
            timestamp = activity.timestamp,
            format = definition.get(activity.type).format(
                activity.type, activity.metadata
            )
        ).also { Fancam.debug { it.toString() } }
    }

    private val regex = Regex("\\{(.*?)}")

    // parse FormatDefinition, update string format, return updated format
    private fun List<FormatDefinition>.format(
        type: String, metadata: Map<String, Any?>
    ): List<FormatDefinition> {
        val result = mutableListOf<FormatDefinition>()
        for ((style, link, color, text) in this) {
            result.add(
                FormatDefinition(
                    style = style,
                    link = link?.format(type, metadata),
                    color = color,
                    text = text.format(type, metadata)
                )
            )
        }
        return result
    }

    private fun String.format(type: String, metadata: Map<String, Any?>): String {
        // find all formattable text inside {...}
        val toBeFormatted = regex.findAll(this)
            .map { it.groupValues[1] }
            .toList()

        // for each format, find its value on the metadata and replace it
        var textResult = this
        for (stringFormat in toBeFormatted) {
            val value = metadata[stringFormat]
            if (value == null) {
                Fancam.warn { "'$stringFormat' is missing on activity '$type' metadata." }
            }
            textResult = textResult.replace("{$stringFormat}", value.toString())
        }
        return textResult
    }
}
