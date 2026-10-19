package mbworld.domain.lobby.activityFeed

import encore.definition.DataDefinition
import encore.definition.FileDataLoader
import encore.definition.FileDataSource
import kotlinx.serialization.Serializable
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Loader for `feed-strings.xml`.
 */
class FeedStringsLoader : FileDataLoader {
    private val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    private val regex = "[\\n\\r\\t]".toRegex()

    // FileDataSource should be XmlDataSource
    override fun produce(source: FileDataSource): List<DataDefinition> {
        val document = builder.parse(InputSource(StringReader(source.readText())))
        val resultEntries = mutableListOf<FeedStringsEntry>()
        val entries = document.getElementsByTagName("entry")

        for (i in 0 until entries.length) {
            val entry = entries.item(i) as Element
            val type = entry.getAttribute("type")
            val format = mutableListOf<FormatDefinition>()

            for (j in 0 until entry.childNodes.length) {
                val node = entry.childNodes.item(j)
                when (node.nodeType) {
                    Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> {
                        val text = node.nodeValue ?: continue
                        if (text.isNotBlank()) {
                            format.add(
                                FormatDefinition(
                                    style = "plain",
                                    link = null,
                                    color = null,
                                    text = text.replace(regex, "")
                                )
                            )
                        }
                    }

                    Node.ELEMENT_NODE -> {
                        val element = node as Element
                        when (element.tagName) {
                            "link" -> {
                                format.add(
                                    FormatDefinition(
                                        style = "link",
                                        link = element.getAttribute("url") ?: "",
                                        color = null,
                                        text = element.textContent
                                    )
                                )
                            }

                            "b" -> {
                                format.add(
                                    FormatDefinition(
                                        style = "b",
                                        link = null,
                                        color = null,
                                        text = element.textContent
                                    )
                                )
                            }

                            "i" -> {
                                format.add(
                                    FormatDefinition(
                                        style = "i",
                                        link = null,
                                        color = null,
                                        text = element.textContent
                                    )
                                )
                            }

                            "u" -> {
                                format.add(
                                    FormatDefinition(
                                        style = "u",
                                        link = null,
                                        color = null,
                                        text = element.textContent
                                    )
                                )
                            }

                            "color" -> {
                                format.add(
                                    FormatDefinition(
                                        style = "color",
                                        link = null,
                                        color = element.getAttribute("code"),
                                        text = element.textContent
                                    )
                                )
                            }
                        }
                    }
                }
            }
            resultEntries.add(FeedStringsEntry(type, format))
        }

        return listOf(FeedStringsDefinition(resultEntries))
    }
}

/**
 * Represent a parsed entry of feed string in `feed-strings.xml`.
 * @property type The corresponding type of the activity for this string entry.
 * @property format Content of the entry as a list of format definitions.
 */
@Serializable
data class FeedStringsEntry(
    val type: String,
    val format: List<FormatDefinition>
)

/**
 * Represent a single part of string on each entry of the strings translation file.
 *
 * A single part of string has:
 * - A particular [style]: "plain", "link", "b", "i", "u", or "color"
 * - Optional [link] if [style] is "link"
 * - Optional [color] if [style] is "color"
 * - And the [text] string as the display.
 *
 * See our docs for more detail.
 */
@Serializable
data class FormatDefinition(
    val style: String,
    val link: String? = null,
    val color: String? = null,
    val text: String
)
