package mbworld.domain.cafe.topic

import encore.datastore.DocumentNotDeletedException
import encore.datastore.DocumentNotFoundException
import kotlin.math.max

/**
 * In-memory implementation for [TopicRepository].
 */
class InMemoryTopicRepository(
    private val topics: MutableList<Topic> = mutableListOf()
) : TopicRepository {
    override suspend fun awaitInit() = Unit

    override suspend fun getTopic(topicId: String): Result<Topic> {
        val topic = topics.find { it.topicId == topicId }
            ?: return Result.failure(DocumentNotFoundException("topicId=$topicId not found"))
        return Result.success(topic)
    }

    override suspend fun getTopicByShortId(shortTopicId: String): Result<Topic> {
        val topic = topics.find { it.topicId.startsWith(shortTopicId) }
            ?: return Result.failure(DocumentNotFoundException("shortTopicId=$shortTopicId not found"))
        return Result.success(topic)

    }

    override suspend fun getFullTopicId(shortTopicId: String): Result<String> {
        val topic = topics.find { it.topicId.startsWith(shortTopicId) }
            ?: return Result.failure(DocumentNotFoundException("shortTopicId=$shortTopicId not found"))
        return Result.success(topic.topicId)
    }

    override suspend fun getTopics(): Result<List<Topic>> {
        return Result.success(topics)
    }

    override suspend fun getTopicsOfSection(sectionId: String): Result<List<Topic>> {
        return Result.success(topics.filter { it.sectionId == sectionId })
    }

    override suspend fun getTopicsCountForEachSection(): Result<Map<String, Int>> {
        return Result.success(
            topics
                .groupBy { it.sectionId }
                .mapValues { it.value.size }
        )
    }

    override suspend fun addTopic(topic: Topic): Result<Unit> {
        topics.add(topic)
        return Result.success(Unit)
    }

    override suspend fun deleteTopic(topicId: String): Result<Unit> {
        if (!topics.removeIf { it.topicId == topicId }) {
            return Result.failure(DocumentNotDeletedException("topicId=$topicId fails to be deleted"))
        }
        return Result.success(Unit)
    }

    override suspend fun deleteAllTopics(): Result<Unit> {
        topics.clear()
        return Result.success(Unit)
    }

    override suspend fun incrementLike(topicId: String): Result<Unit> {
        val topic = topics.find { it.topicId == topicId }
            ?.let { it.copy(likes = it.likes + 1) }
            ?: return Result.failure(DocumentNotFoundException("topicId=$topicId not found"))

        topics.removeIf { it.topicId == topicId }
        topics.add(topic)
        return Result.success(Unit)
    }

    override suspend fun decrementLike(topicId: String): Result<Unit> {
        val topic = topics.find { it.topicId == topicId }
            ?.let { it.copy(likes = max(0, it.likes - 1)) }
            ?: return Result.failure(DocumentNotFoundException("topicId=$topicId not found"))

        topics.removeIf { it.topicId == topicId }
        topics.add(topic)
        return Result.success(Unit)
    }
}
