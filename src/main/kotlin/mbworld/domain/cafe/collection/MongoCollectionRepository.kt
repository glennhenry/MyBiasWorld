package mbworld.domain.cafe.collection

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import encore.datastore.runMongoCatching
import encore.datastore.throwIfNothingModified
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import mbworld.domain.cafe.topic.LatestPostInfo
import mbworld.domain.cafe.topic.QueryTopicLatestPost

/** `order` */
val FieldSpaceOrder = Space::order.name

/** `order` */
val FieldSectionOrder = Section::order.name

/** `id` */
val FieldSpaceId = Space::id.name

/** `id` */
val FieldSectionId = Section::id.name

/** `topicCounts` */
val FieldSectionTopicCounts = Section::topicCounts.name

/** `latestPost` */
val FieldSectionLatestPost = Section::latestPost.name

class MongoCollectionRepository(
    private val spaceCollection: MongoCollection<Space>,
    private val sectionCollection: MongoCollection<Section>,
) : CollectionRepository {
    override suspend fun getSpaces(): Result<List<Space>> {
        return runMongoCatching {
            spaceCollection
                .find()
                .sort(Sorts.descending(FieldSpaceOrder))
                .toList()
        }
    }

    override suspend fun getSections(): Result<List<Section>> {
        return runMongoCatching {
            sectionCollection
                .find()
                .sort(Sorts.descending(FieldSectionOrder))
                .toList()
        }
    }

    override suspend fun getSpace(spaceId: String): Result<Space> {
        return runMongoCatching {
            spaceCollection
                .find(Filters.eq(FieldSpaceId, spaceId))
                .firstOrNull()
        }
    }

    override suspend fun getSection(sectionId: String): Result<Section> {
        return runMongoCatching {
            sectionCollection
                .find(Filters.eq(FieldSectionId, sectionId))
                .firstOrNull()
        }
    }

    override suspend fun incrementTopicCount(sectionId: String): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.eq(FieldSectionId, sectionId)
            val update = Updates.inc(FieldSectionTopicCounts, 1)

            sectionCollection
                .updateOne(filter, update)
                .throwIfNothingModified("incrementTopicCount", { filter }, { update })
        }
    }

    override suspend fun decrementTopicCount(sectionId: String): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.eq(FieldSectionId, sectionId)
            val update = Updates.inc(FieldSectionTopicCounts, -1)

            sectionCollection
                .updateOne(filter, update)
                .throwIfNothingModified("incrementTopicCount", { filter }, { update })
        }
    }

    override suspend fun getLatestPost(sectionId: String): Result<LatestPostInfo> {
        return runMongoCatching {
            sectionCollection
                .withDocumentClass<QueryTopicLatestPost>()
                .find(Filters.eq(FieldSectionId, sectionId))
                .projection(
                    Projections.fields(
                        Projections.include(FieldSectionLatestPost),
                        Projections.excludeId()
                    )
                )
                .firstOrNull()
                ?.latestPost
        }
    }

    override suspend fun updateLatestPost(
        sectionId: String,
        postInfo: LatestPostInfo
    ): Result<Unit> {
        return runMongoCatching {
            val filter = Filters.eq(FieldSectionId, sectionId)
            val update = Updates.set(FieldSectionLatestPost, postInfo)

            sectionCollection.updateOne(filter, update)
                .throwIfNothingModified("updateLatestPost", { filter }, { update })
        }
    }
}
