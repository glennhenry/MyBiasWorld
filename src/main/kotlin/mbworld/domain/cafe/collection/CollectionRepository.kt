package mbworld.domain.cafe.collection

/**
 * Repository for [Space] and [Section] data collection.
 *
 * Implementation solely provide way to access them.
 * Since space and section is rarely changed, administrator
 * may add collection directly to the underlying database editor.
 */
interface CollectionRepository {
    /**
     * Get every available spaces.
     */
    suspend fun getSpaces(): Result<List<Space>>

    /**
     * Get every available sections.
     */
    suspend fun getSections(): Result<List<Section>>

    /**
     * Get a particular [Space] by its `spaceId`.
     */
    suspend fun getSpace(spaceId: String): Result<Space>

    /**
     * Get a particular [Section] by its `sectionId`.
     */
    suspend fun getSection(sectionId: String): Result<Section>

    /**
     * Increment the [sectionId]'s topic count by 1.
     * `sectionId` refers to [Section.id].
     */
    suspend fun incrementTopicCount(sectionId: String): Result<Unit>

    /**
     * Decrement the [sectionId]'s topic count by 1.
     * `sectionId` refers to [Section.id].
     */
    suspend fun decrementTopicCount(sectionId: String): Result<Unit>
}
