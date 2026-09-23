package encore.definition

/**
 * Loads and interprets a [FileDataSource] into one or more [DataDefinition]s.
 *
 * Implementations are responsible for reading, decoding, and transforming
 * raw data into structured, domain-specific definitions.
 */
interface FileDataLoader {
    /**
     * Produces [DataDefinition]s from the given [FileDataSource].
     */
    fun produce(source: FileDataSource): List<DataDefinition>
}
