package mbworld.domain.dummy

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import encore.backstage.command.Command
import encore.backstage.command.types.ArgumentCollection
import encore.backstage.command.types.CommandResult
import encore.creation.UserCreationSubunit
import encore.fancam.Fancam
import encore.time.TimeCenter
import encore.utils.types.Report
import encore.utils.types.okOrThrow
import mbworld.context.ServerContext
import mbworld.domain.activity.model.Activity
import mbworld.domain.activity.model.ActivitySource
import mbworld.domain.auth.UsersActivity
import mbworld.domain.cafe.CafeActivity
import mbworld.domain.cafe.Sections
import mbworld.domain.cafe.likes.Likes
import mbworld.domain.cafe.reply.Reply
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
    override val commandId: String = "dummy-setup"
    override val description: String = "Prepare dummy activities for the website. " +
            "This will make use the DummyActivitySetup class."

    override suspend fun execute(
        serverContext: ServerContext,
        args: ArgumentCollection
    ): CommandResult {
        val numAccounts = (15..20).random()

        val insertedUsers = mutableListOf<String>()
        val addedTopics = mutableListOf<Topic>()
        val addedReplies = mutableListOf<Reply>()
        val addedTopicLikes = mutableListOf<Likes>()
        val addedReplyLikes = mutableListOf<Likes>()

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
                        addedTopics.add(topic)
                    }
                }
            }

            // 3. create replies and comments
            addedTopics.forEach { topic ->
                // 70% chance of reply, 30% chance for no reply
                if (Random.nextDouble() < 0.7) {
                    val amountOfReply = (1..6).random()
                    val replies = List(amountOfReply) {
                        ReplyFactory.reply(
                            topicId = topic.topicId,
                            topicPostDate = topic.postedDate,
                            possibleReplyAuthors = insertedUsers,
                            possibleAmountofComments = 1..4,
                            possibleCommentAuthors = insertedUsers
                        )
                    }.sortedBy { it.postedDate }
                    replies.forEach {
                        addedReplies.add(it)
                        serverContext.subunits.reply.addReply(it)
                    }
                }
            }

            // 4. add user likes to topic and replies
            insertedUsers.forEach { userId ->
                // 30% chance for a user to like a particular topic
                addedTopics.forEach { topic ->
                    if (Random.nextDouble() < 0.3) {
                        val now = TimeCenter.now() // differ very slightly
                        serverContext.subunits.likes.addLike(userId, topic.topicId)
                        serverContext.subunits.topic.incrementLike(topic.topicId)
                        addedTopicLikes.add(Likes(userId, topic.topicId, now))
                    }
                }

                // 10% chance for a user to like a particular reply
                addedReplies.forEach { reply ->
                    if (Random.nextDouble() < 0.1) {
                        val now = TimeCenter.now() // differ very slightly
                        serverContext.subunits.likes.addLike(userId, reply.replyId)
                        serverContext.subunits.reply.incrementLike(reply.replyId)
                        addedReplyLikes.add(Likes(userId, reply.replyId, now))
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
                        )
                    )
                )
            }

            // 5 topic posts
            addedTopics.shuffled().take(5).forEach { topic ->
                serverContext.subunits.activity.publish(
                    Activity(
                        source = ActivitySource.Cafe,
                        type = CafeActivity.TopicCreated,
                        timestamp = topic.postedDate,
                        metadata = mapOf(
                            "topicId" to topic.topicId,
                            "authorId" to topic.authorId,
                            "authorDisplayName" to accounts[topic.authorId]!!.displayName,
                            "sectionName" to Sections[topic.sectionId]
                        )
                    )
                )
            }

            // 5 replies
            addedReplies.shuffled().take(5).forEach { reply ->
                val topicTitle = serverContext.subunits.topic.getTopic(reply.topicId)
                    .okOrThrow()?.title
                val replyAmount = serverContext.subunits.reply.getReplyCount(reply.topicId)
                    .okOrThrow()
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
                            "topicTitle" to topicTitle,
                            "replyAmount" to replyAmount
                        )
                    )
                )
            }

            // 0-5 comments
            addedReplies.shuffled().take(5).forEach { reply ->
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
                                "replyAuthorDisplayName" to accounts[reply.authorId]!!.displayName
                            )
                        )
                    )
                }
            }

            // 5 likes
            addedTopicLikes.shuffled().take(5).forEach { likes ->
                val topic = serverContext.subunits.topic.getTopic(likes.postId)
                    .okOrThrow()!!
                serverContext.subunits.activity.publish(
                    Activity(
                        source = ActivitySource.Cafe,
                        type = CafeActivity.TopicLiked,
                        timestamp = likes.castedAt,
                        metadata = mapOf(
                            "topicId" to likes.postId,
                            "authorId" to likes.userId,
                            "displayName" to accounts[likes.userId]!!.displayName,
                            "topicTitle" to topic.title,
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
        return CommandResult.Executed("Dummy setup completed.")
    }
}
