package mbworld.domain.cafe.collection

import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
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
     * Use this to refresh the `spaces` and `sections` collection,
     * forcing this subunit to re-query the [CollectionRepository].
     *
     * This is typically used when `spaces` or `sections` collections
     * are empty before the application is run, so they could be updated.
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
