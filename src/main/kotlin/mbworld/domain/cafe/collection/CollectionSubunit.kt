package mbworld.domain.cafe.collection

import encore.datastore.DocumentNotFoundException
import encore.fancam.Fancam
import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import encore.utils.types.Outcome
import encore.utils.types.Report
import encore.utils.types.toOutcome
import encore.utils.types.toReport
import mbworld.domain.cafe.topic.LatestPostInfo

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
                val idx = sections.indexOfFirst { it.id == sectionId }
                sections[idx] = sections[idx].copy(topicCounts = sections[idx].topicCounts + 1)
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
                val idx = sections.indexOfFirst { it.id == sectionId }
                sections[idx] = sections[idx].copy(topicCounts = sections[idx].topicCounts - 1)
            }
            .toReport()
    }

    /**
     * Get the latest post information of [sectionId].
     * - [Outcome.Fail] when there is internal repository error.
     * - [Outcome.Ok] with `null` if topic is not found.
     * - [Outcome.Ok] with the [LatestSectionPostInfo] otherwise.
     */
    suspend fun getLatestPost(sectionId: String): Outcome<LatestSectionPostInfo?> {
        return collectionRepository.getLatestPost(sectionId)
            .onFailure {
                if (it is DocumentNotFoundException) {
                    Fancam.warn("topic") {
                        "getLatestPost topic not found for sectionId=$sectionId"
                    }
                    return Outcome.Ok(null)
                }
                Fancam.error(it, "topic") {
                    "getLatestPost query failed for sectionId=$sectionId"
                }
            }
            .toOutcome { it }
    }

    /**
     * Update the latest post information of [sectionId].
     * @return [Report] denoting success or failure.
     */
    suspend fun updateLatestPost(sectionId: String, postInfo: LatestSectionPostInfo): Report {
        return collectionRepository.updateLatestPost(sectionId, postInfo)
            .onFailure {
                Fancam.error(it, "topic") {
                    "getLatestPost failed for sectionId=$sectionId"
                }
            }
            .onSuccess {
                val idx = sections.indexOfFirst { it.id == sectionId }
                sections[idx] = sections[idx].copy(latestPost = postInfo)
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
