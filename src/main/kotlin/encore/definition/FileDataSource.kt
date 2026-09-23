package encore.definition

/**
 * Provides access to static resources which stores the raw data.
 *
 * A `FileDataSource` abstracts how data is stored and retrieved,
 * regardless of its underlying format (e.g. JSON, XML, binary).
 *
 * It does not interpret the data; loaders such as [FileDataLoader]
 * are responsible for parsing it into [DataDefinition]s.
 */
interface FileDataSource {
    /**
     * Path to the resource file.
     */
    val path: String

    /**
     * Reads the content as a UTF-8 string (for text-based formats).
     */
    fun readText(): String

    /**
     * Reads the raw content of this resource as a byte array.
     */
    fun readBytes(): ByteArray
}
