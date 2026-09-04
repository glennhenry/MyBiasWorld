package mbworld.domain.cafe.reply

import encore.datastore.DocumentNotFoundException
import kotlin.math.max

/**
 * In-memory implementation for [ReplyRepository].
 */
class InMemoryReplyRepository(
    private val replies: MutableList<Reply> = mutableListOf()
) : ReplyRepository {
    override suspend fun awaitInit() = Unit

    override suspend fun getReply(replyId: String): Result<Reply> {
        val reply = replies.find { it.replyId == replyId }
            ?: return Result.failure(DocumentNotFoundException("replyId=$replyId not found"))
        return Result.success(reply)
    }

    override suspend fun getRepliesUnder(topicId: String): Result<List<Reply>> {
        return Result.success(replies.filter { it.topicId == topicId })
    }

    override suspend fun getReplyCount(topicId: String): Result<Int> {
        return Result.success(replies.count { it.topicId == topicId })
    }

    override suspend fun getReplyCounts(topicIds: List<String>): Result<Map<String, Int>> {
        val map = mutableMapOf<String, Int>()
        for ((_, topicId) in replies) {
            map[topicId] = map.getOrDefault(topicId, 0) + 1
        }

        return Result.success(map)
    }

    override suspend fun addReply(reply: Reply): Result<Unit> {
        replies.add(reply)
        return Result.success(Unit)
    }

    override suspend fun getComments(
        replyId: String,
        limit: Int
    ): Result<List<Comment>> {
        val reply = replies.find { it.replyId == replyId }
            ?: return Result.failure(DocumentNotFoundException("replyId=$replyId not found"))

        return Result.success(reply.comments.take(limit))
    }

    override suspend fun addComment(
        replyId: String,
        comment: Comment
    ): Result<Unit> {
        val reply = replies.find { it.replyId == replyId }
            ?: return Result.failure(DocumentNotFoundException("replyId=$replyId not found."))

        replies.removeIf { it.replyId == replyId }
        replies.add(reply.copy(comments = reply.comments + comment))
        return Result.success(Unit)
    }

    override suspend fun incrementLike(replyId: String): Result<Unit> {
        val reply = replies.find { it.replyId == replyId }
            ?.let { it.copy(likes = it.likes + 1) }
            ?: return Result.failure(DocumentNotFoundException("replyId=$replyId not found"))

        replies.removeIf { it.replyId == replyId }
        replies.add(reply)
        return Result.success(Unit)
    }

    override suspend fun decrementLike(replyId: String): Result<Unit> {
        val reply = replies.find { it.replyId == replyId }
            ?.let { it.copy(likes = max(0, it.likes - 1)) }
            ?: return Result.failure(DocumentNotFoundException("replyId=$replyId not found"))

        replies.removeIf { it.replyId == replyId }
        replies.add(reply)
        return Result.success(Unit)
    }
}
