package mbworld.domain.dummy

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import encore.backstage.command.Command
import encore.backstage.command.types.Argument
import encore.backstage.command.types.ArgumentInputs
import encore.backstage.command.types.CommandResult
import encore.creation.UserCreationSubunit
import encore.fancam.Fancam
import encore.time.TimeCenter
import encore.utils.identifier.shortUuid
import encore.utils.types.Report
import encore.utils.types.okOrThrow
import mbworld.context.ServerContext
import mbworld.domain.activity.model.Activity
import mbworld.domain.activity.model.ActivitySource
import mbworld.domain.auth.UsersActivity
import mbworld.domain.cafe.CafeActivity
import mbworld.domain.cafe.Sections
import mbworld.domain.cafe.collection.LatestSectionPostInfo
import mbworld.domain.cafe.likes.Likes
import mbworld.domain.cafe.reply.Comment
import mbworld.domain.cafe.reply.Reply
import mbworld.domain.cafe.toUrlSlug
import mbworld.domain.cafe.topic.LatestPostInfo
import mbworld.domain.cafe.topic.PostInfoType
import mbworld.domain.cafe.topic.Topic
import mbworld.domain.profile.model.Profile
import mbworld.domain.profile.subunits.MongoProfileRepository
import mbworld.mongo.RuntimeMongoCollections
import mbworld.mongo.collection.UserAccount
import mbworld.mongo.collection.UserId
import kotlin.random.Random

/**
 * A command used to prepare dummy activites for the website.
 * For testing purposes, this command can create dummy accounts, topic posts,
 * comments, replies, and other activity around the website.
 *
 * The detailed setup:
 * Setup:
 * 1. Create 15-20 dummy accounts with random username and email
 *    produced by [AccountFactory], and password fixed to "dummy".
 * 2. Create 0-7 posts for each previously created dummy accounts;
 *    20% chance for 0 post for an account.
 * 3. Create 0-6 replies for each posts; 30% chance for 0 reply.
 * 4. Create 0-4 comments for each replies; 50% chance for 0 comment.
 *
 * Usage: call 'dummy-setup' on the backstage command tool.
 * No arguments are needed.
 */
class DummySetupCommand(private val db: MongoDatabase) : Command {
    override val id: String = "dummy-setup"
    override val name: String = "Dummy setup"
    override val description: String = """
        Prepare dummy activities for the website.
        This will make use the DummyActivitySetup class.
    """.trimIndent()
    override val arguments: List<Argument> = emptyList()

    override suspend fun execute(
        serverContext: ServerContext,
        args: ArgumentInputs
    ): CommandResult {
        val numAccounts = (15..20).random()

        val insertedUsers = mutableListOf<String>()
        val addedTopics = mutableMapOf<String, Topic>()
        val addedReplies = mutableMapOf<String, Reply>()
        val addedTopicLikes = mutableListOf<Likes>()
        val addedReplyLikes = mutableListOf<Likes>()
        val latestPostOfSections = mutableMapOf<String, Long>()

        val accounts = mutableMapOf<UserId, UserAccount>()
        val profiles = mutableMapOf<UserId, Profile>()
        val idsToUse = ArrayDeque<UserId>()

        val profileRepository = MongoProfileRepository(
            profiles = db.getCollection(RuntimeMongoCollections.profiles)
        )

        val creation = UserCreationSubunit(
            dataStore = serverContext.dataStore,
            profileRepository = profileRepository,
            factory = FakeCreationFactory(nextUserId = {
                idsToUse.removeFirst()
            }, accounts, profiles) { Report.Ok }
        )

        try {
            // 1. create accounts
            repeat(numAccounts) {
                val acc = AccountFactory.account()
                accounts[acc.userId] = acc
                profiles[acc.userId] = ProfileFactory.profile(acc.userId, acc.username, acc.displayName)
                idsToUse.add(acc.userId)
                insertedUsers.add(creation.createUser("ignoreThisParam", "ignoreThisParam", "ignoreThisParam"))
            }

            // 2. create topics
            insertedUsers.forEach { userId ->
                // 80% chance of post, 20% chance of no post
                if (Random.nextDouble() < 0.8) {
                    val numPostsEachAccounts = (1..7).random()
                    val topics = TopicFactory.topics(userId, numPostsEachAccounts)
                    for (topic in topics) {
                        serverContext.subunits.topic.addTopic(topic)
                        addedTopics[topic.topicId] = topic
                        serverContext.subunits.collection.incrementTopicCount(topic.sectionId)
                        // update latest post of each section depending on topic postedDate
                        val latestPostDate = latestPostOfSections[topic.sectionId]
                        if (latestPostDate != null) {
                            if (topic.postedDate > latestPostDate) {
                                serverContext.subunits.collection.updateLatestPost(
                                    sectionId = topic.sectionId,
                                    postInfo = LatestSectionPostInfo(
                                        PostInfoType.Topic,
                                        topic.topicId,
                                        null,
                                        topic.authorId,
                                        topic.postedDate
                                    )
                                )
                                latestPostOfSections[topic.sectionId] = topic.postedDate
                            }
                        } else {
                            latestPostOfSections[topic.sectionId] = topic.postedDate
                        }
                    }
                }
            }

            // 3. create replies and comments
            addedTopics.forEach { (topicId, topic) ->
                // 70% chance of reply, 30% chance for no reply
                if (Random.nextDouble() < 0.7) {
                    val amountOfReply = (1..6).random()
                    val replies = List(amountOfReply) {
                        ReplyFactory.reply(
                            topicId = topicId,
                            topicPostDate = topic.postedDate,
                            possibleReplyAuthors = insertedUsers,
                            possibleAmountofComments = 1..4,
                            possibleCommentAuthors = insertedUsers
                        )
                    }.sortedBy { it.postedDate }
                    replies.forEach { reply ->
                        addedReplies[reply.replyId] = reply
                        serverContext.subunits.reply.addReply(reply)
                        serverContext.subunits.topic.incrementReply(reply.topicId)

                        // update latest post of each section depending on reply postedDate
                        val latestPostDate = latestPostOfSections[topic.sectionId]
                        if (latestPostDate != null) {
                            if (reply.postedDate > latestPostDate) {
                                serverContext.subunits.collection.updateLatestPost(
                                    sectionId = topic.sectionId,
                                    postInfo = LatestSectionPostInfo(
                                        PostInfoType.Reply,
                                        reply.topicId,
                                        reply.replyId,
                                        reply.authorId,
                                        reply.postedDate
                                    )
                                )
                                latestPostOfSections[topic.sectionId] = reply.postedDate
                            }
                        } else {
                            latestPostOfSections[topic.sectionId] = reply.postedDate
                        }
                    }
                }
            }

            // update latest post of each topic depending on reply/comment
            addedReplies.values.groupBy { it.topicId }.forEach { (topicId, replies) ->
                if (replies.isNotEmpty()) {
                    val latestReply = replies.maxBy { it.postedDate }
                    var latestCommentDate = -1L
                    var latestComment: Comment? = null

                    replies.forEach { reply ->
                        if (reply.comments.isNotEmpty()) {
                            val comment = reply.comments.maxBy { it.postedDate }
                            if (comment.postedDate > latestCommentDate) {
                                latestComment = comment
                                latestCommentDate = comment.postedDate
                            }
                        }
                    }

                    if (latestReply.postedDate > latestCommentDate) {
                        serverContext.subunits.topic.updateLatestPost(
                            topicId = topicId,
                            postInfo = LatestPostInfo(
                                PostInfoType.Reply,
                                latestReply.replyId,
                                latestReply.authorId,
                                latestReply.postedDate
                            )
                        )
                    } else {
                        serverContext.subunits.topic.updateLatestPost(
                            topicId = topicId,
                            postInfo = LatestPostInfo(
                                PostInfoType.Comment,
                                latestComment!!.commentId,
                                latestComment.authorId,
                                latestCommentDate
                            )
                        )
                    }
                }
            }

            // 4. add user likes to topic and replies
            insertedUsers.forEach { userId ->
                // 30% chance for a user to like a particular topic
                addedTopics.forEach { (topicId, _) ->
                    if (Random.nextDouble() < 0.3) {
                        val now = TimeCenter.now() // differ very slightly
                        serverContext.subunits.likes.addLike(userId, topicId)
                        serverContext.subunits.topic.incrementLike(topicId)
                        addedTopicLikes.add(Likes(userId, topicId, now))
                    }
                }

                // 10% chance for a user to like a particular reply
                addedReplies.forEach { (replyId, reply) ->
                    if (Random.nextDouble() < 0.1) {
                        val now = TimeCenter.now() // differ very slightly
                        serverContext.subunits.likes.addLike(userId, replyId)
                        serverContext.subunits.reply.incrementLike(replyId)
                        addedReplyLikes.add(Likes(userId, replyId, now))
                    }
                }
            }

            // 5. add activities
            // 5 accounts registration
            accounts.toList().shuffled().take(5).forEach { (_, acc) ->
                serverContext.subunits.activity.publish(
                    Activity(
                        source = ActivitySource.Users,
                        type = UsersActivity.UserRegistered,
                        timestamp = acc.registeredAt,
                        metadata = mapOf(
                            "userId" to acc.userId,
                            "username" to acc.username,
                            "email" to acc.email,
                            "authorProfileUrl" to "/profile/@${acc.username}/overview"
                        )
                    )
                )
            }

            // 5 topic posts
            addedTopics.values.shuffled().take(5).forEach { topic ->
                val topicUrl =
                    "/cafe/${topic.sectionId}/${topic.topicId.shortUuid()}/${topic.title.toUrlSlug()}"
                serverContext.subunits.activity.publish(
                    Activity(
                        source = ActivitySource.Cafe,
                        type = CafeActivity.TopicCreated,
                        timestamp = topic.postedDate,
                        metadata = mapOf(
                            "topicId" to topic.topicId,
                            "authorId" to topic.authorId,
                            "authorDisplayName" to accounts[topic.authorId]!!.displayName,
                            "authorProfileUrl" to "/profile/@${accounts[topic.authorId]!!.username}/overview",
                            "sectionName" to Sections[topic.sectionId],
                            "topicTitle" to topic.title,
                            "topicUrl" to topicUrl
                        )
                    )
                )
            }

            // 5 replies
            addedReplies.values.shuffled().take(5).forEach { reply ->
                val topic = serverContext.subunits.topic.getTopic(reply.topicId)
                    .okOrThrow()!!
                val replyAmount = serverContext.subunits.reply.getReplyCount(reply.topicId)
                    .okOrThrow()!!
                val topicUrl =
                    "/cafe/${topic.sectionId}/${topic.topicId.shortUuid()}/${topic.title.toUrlSlug()}"
                serverContext.subunits.activity.publish(
                    Activity(
                        source = ActivitySource.Cafe,
                        type = CafeActivity.ReplyAdded,
                        timestamp = reply.postedDate,
                        metadata = mapOf(
                            "topicId" to reply.topicId,
                            "replyId" to reply.replyId,
                            "authorId" to reply.authorId,
                            "authorDisplayName" to accounts[reply.authorId]!!.displayName,
                            "authorProfileUrl" to "/profile/@${accounts[reply.authorId]!!.username}/overview",
                            "topicTitle" to topic.title,
                            "topicUrl" to topicUrl,
                            "replyAmount" to replyAmount
                        )
                    )
                )
            }

            // 0-5 comments
            addedReplies.values.shuffled().take(5).forEach { reply ->
                reply.comments.shuffled().take(1).firstOrNull()?.let {
                    serverContext.subunits.activity.publish(
                        Activity(
                            source = ActivitySource.Cafe,
                            type = CafeActivity.CommentAdded,
                            timestamp = it.postedDate,
                            metadata = mapOf(
                                "topicId" to reply.topicId,
                                "replyId" to reply.replyId,
                                "commentId" to it.commentId,
                                "authorId" to it.authorId,
                                "commentAuthorDisplayName" to accounts[it.authorId]!!.displayName,
                                "commentAuthorProfileUrl" to "/profile/@${accounts[it.authorId]!!.username}/overview",
                                "replyAuthorDisplayName" to accounts[reply.authorId]!!.displayName,
                                "replyAuthorProfileUrl" to "/profile/@${accounts[reply.authorId]!!.username}/overview"
                            )
                        )
                    )
                }
            }

            // 5 likes
            addedTopicLikes.shuffled().take(5).forEach { likes ->
                val topic = serverContext.subunits.topic.getTopic(likes.postId)
                    .okOrThrow()!!
                val topicUrl =
                    "/cafe/${topic.sectionId}/${topic.topicId.shortUuid()}/${topic.title.toUrlSlug()}"
                serverContext.subunits.activity.publish(
                    Activity(
                        source = ActivitySource.Cafe,
                        type = CafeActivity.TopicLiked,
                        timestamp = likes.castedAt,
                        metadata = mapOf(
                            "topicId" to likes.postId,
                            "authorId" to likes.userId,
                            "displayName" to accounts[likes.userId]!!.displayName,
                            "profileUrl" to "/profile/@${accounts[likes.userId]!!.username}/overview",
                            "topicTitle" to topic.title,
                            "topicUrl" to topicUrl,
                            "amount" to topic.likes
                        )
                    )
                )
            }
        } catch (e: Exception) {
            Fancam.error(e, "dummysetup") { "Scandal during dummy setup" }
            return CommandResult.Error("Scandal during dummy setup: ${e.message}")
        }

        Fancam.info { "Dummy setup completed." }
        return CommandResult.Success("Dummy setup completed.")
    }
}
