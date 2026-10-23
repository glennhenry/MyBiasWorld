package mbworld.domain.cafe.collection

import encore.datastore.DocumentNotFoundException
import encore.fancam.Fancam
import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import encore.utils.types.Outcome
import encore.utils.types.Report
import encore.utils.types.toOutcome
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
    private lateinit var spaces: List<Space>
    private lateinit var sections: List<Section>
    private lateinit var landingModel: List<SpaceItem>

    fun getSpaces() = spaces
    fun getSections() = sections
    fun getSpacesForLandingModel(): List<SpaceItem> = landingModel

    fun createLandingModel(): List<SpaceItem> {
        val sectionsBySpace = sections.groupBy { it.spaceId }

        return spaces
            .sortedBy { it.order }
            .map { space ->
                SpaceItem(
                    name = space.name,
                    sections = sectionsBySpace[space.id]
                        .orEmpty()
                        .sortedBy { it.order }
                        .map { SectionItem(it.id, it.name, it.description) }
                )
            }
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
            spaces = collectionRepository.getSpaces().getOrThrow()
            sections = collectionRepository.getSections().getOrThrow()
            landingModel = createLandingModel()
        }.toReport()
    }

    /**
     * Get the [Space] identified by its `spaceId`.
     * Returns an [Outcome]:
     * - [Outcome.Fail] when there is internal repository error.
     * - [Outcome.Ok] with null if space is not found.
     * - [Outcome.Ok] with the [Space] otherwise.
     */
    suspend fun getSpace(spaceId: String): Outcome<Space?> {
        return collectionRepository.getSpace(spaceId)
            .onFailure {
                if (it is DocumentNotFoundException) {
                    Fancam.warn("collection") {
                        "getSpace space not found for spaceId=$spaceId"
                    }
                    return Outcome.Ok(null)
                }
                Fancam.error(it, "topic") {
                    "getSpace query failed for spaceId=$spaceId"
                }
            }
            .toOutcome { it }
    }

    /**
     * Get the [Section] identified by its `sectionId`.
     * Returns an [Outcome]:
     * - [Outcome.Fail] when there is internal repository error.
     * - [Outcome.Ok] with null if space is not found.
     * - [Outcome.Ok] with the [Section] otherwise.
     */
    suspend fun getSection(sectionId: String): Outcome<Section?> {
        return collectionRepository.getSection(sectionId)
            .onFailure {
                if (it is DocumentNotFoundException) {
                    Fancam.warn("collection") {
                        "getSection space not found for sectionId=$sectionId"
                    }
                    return Outcome.Ok(null)
                }
                Fancam.error(it, "topic") {
                    "getSection query failed for sectionId=$sectionId"
                }
            }
            .toOutcome { it }
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
            .toReport()
    }

    override suspend fun debut(scope: ServerScope): Result<Unit> {
        return runCatching {
            spaces = collectionRepository.getSpaces().getOrThrow()
            sections = collectionRepository.getSections().getOrThrow()
            landingModel = createLandingModel()
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
