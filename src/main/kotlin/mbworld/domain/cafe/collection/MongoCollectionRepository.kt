package mbworld.domain.cafe.collection

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import encore.datastore.runMongoCatching
import encore.datastore.throwIfNothingModified
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

/** `order` */
val FieldSpaceOrder = Space::order.name

/** `order` */
val FieldSectionOrder = Section::order.name

/** `id` */
val FieldSpaceId = Space::id.name

/** `id` */
val FieldSectionId = Section::id.name

/** `topicCouts` */
val FieldSectionTopicCounts = Section::topicCounts.name

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
}
