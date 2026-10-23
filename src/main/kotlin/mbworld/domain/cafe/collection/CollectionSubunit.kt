package mbworld.domain.cafe.collection

import encore.fancam.Fancam
import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import encore.utils.types.Outcome
import encore.utils.types.Report
import encore.utils.types.toReport
import mbworld.domain.cafe.view.model.SectionItem
import mbworld.domain.cafe.view.model.SpaceItem

/**
 * Server subunits that handles [Space] and [Section] concerns from [CollectionRepository].
 *
 * This is also the main component for cafe operation related to them
 * as they are rarely updated and can't be updated as of now.
 *
 * @property collectionRepository [CollectionRepository] implementation.
 */
class CollectionSubunit(
    private val collectionRepository: CollectionRepository
) : Subunit<ServerScope> {
    private lateinit var spaces: MutableList<Space>
    private lateinit var sections: MutableList<Section>

    fun getSpaces() = spaces
    fun getSections() = sections

    /**
     * Get space by its [spaceId].
     * @return [Outcome.Ok] with the space or `null` if not found.
     */
    fun getSpace(spaceId: String): Outcome<Space?> {
        return Outcome.Ok(spaces.find { it.id == spaceId })
    }

    /**
     * Get section by its [sectionId].
     * @return [Outcome.Ok] with the section or `null` if not found.
     */
    fun getSection(sectionId: String): Outcome<Section?> {
        return Outcome.Ok(sections.find { it.id == sectionId })
    }

    fun getSpacesForLandingModel(): List<SpaceItem> {
        val sectionsBySpace = sections.groupBy { it.spaceId }
        return spaces
            .sortedBy { it.order }
            .map { space ->
                SpaceItem(
                    name = space.name,
                    sections = sectionsBySpace[space.id]
                        .orEmpty()
                        .sortedBy { it.order }
                        .map { SectionItem(it.id, it.name, it.description, it.topicCounts) }
                )
            }
    }

    /**
     * Increment the topic count of [sectionId] by 1.
     * `sectionId` refers to the [Section.id].
     * @return Report type indicating success or failure.
     */
    suspend fun incrementTopicCount(sectionId: String): Report {
        return collectionRepository.incrementTopicCount(sectionId)
            .onFailure {
                Fancam.error(it) { "incrementTopicCount failed for $sectionId" }
            }
            .onSuccess {
                // not thread safe: may modified concurrently resulting in inconsistent number
                // but topic counts aren't very important
                sections.find { it.id == sectionId }?.topicCounts += 1
            }
            .toReport()
    }

    /**
     * Decrement the topic count of [sectionId] by 1.
     * `sectionId` refers to the [Section.id].
     * @return Report type indicating success or failure.
     */
    suspend fun decrementTopicCount(sectionId: String): Report {
        return collectionRepository.decrementTopicCount(sectionId)
            .onFailure {
                Fancam.error(it) { "decrementTopicCount failed for $sectionId" }
            }
            .onSuccess {
                sections.find { it.id == sectionId }?.topicCounts -= 1
            }
            .toReport()
    }

    /**
     * Refresh the internal collection model of spaces and sections.
     *
     * The subunit only query collection once at application startup.
     * Use this to refresh the `spaces` and `sections` collection,
     * forcing [CollectionSubunit] to re-query the [CollectionRepository].
     *
     * Use this when you see "No spaces have been made yet ㅠㅠ" while opening the cafe.
     */
    suspend fun refreshCollection(): Report {
        return runCatching {
            spaces = collectionRepository.getSpaces().getOrThrow().toMutableList()
            sections = collectionRepository.getSections().getOrThrow().toMutableList()
        }.toReport()
    }

    override suspend fun debut(scope: ServerScope): Result<Unit> {
        return runCatching {
            spaces = collectionRepository.getSpaces().getOrThrow().toMutableList()
            sections = collectionRepository.getSections().getOrThrow().toMutableList()
        }
    }

    override suspend fun disband(scope: ServerScope): Result<Unit> {
        return runCatching { }
    }

    companion object {
        /**
         * Creates a test instance of [CollectionSubunit].
         *
         * @param collectionRepository use [BlankCollectionRepository] when not under test.
         */
        fun createForTest(
            collectionRepository: CollectionRepository = BlankCollectionRepository()
        ): CollectionSubunit {
            return CollectionSubunit(collectionRepository)
        }
    }
}
