package mbworld.domain.cafe.collection

import encore.datastore.DocumentNotFoundException
import encore.datastore.DocumentNotUpdatedException
import mbworld.domain.cafe.topic.LatestPostInfo
import mbworld.domain.cafe.topic.Topic

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

    /**
     * Get the [sectionId]'s [Section.latestPost].
     *
     * Returns:
     * - [Result.success] if the operation succeeded.
     * - [Result.failure] with [DocumentNotFoundException] if topic is not found.
     * - [Result.failure] with [DocumentNotUpdatedException] if topic fails to be updated.
     * - [Result.failure] if other error occurs during the operation.
     */
    suspend fun getLatestPost(sectionId: String): Result<LatestSectionPostInfo>

    /**
     * Update the [sectionId]'s [Section.latestPost] with [postInfo].
     *
     * Returns:
     * - [Result.success] if the operation succeeded.
     * - [Result.failure] with [DocumentNotFoundException] if topic is not found.
     * - [Result.failure] with [DocumentNotUpdatedException] if topic fails to be updated.
     * - [Result.failure] if other error occurs during the operation.
     */
    suspend fun updateLatestPost(sectionId: String, postInfo: LatestSectionPostInfo): Result<Unit>

}
