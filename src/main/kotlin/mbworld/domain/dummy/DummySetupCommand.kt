package mbworld.domain.dummy

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import encore.backstage.command.Command
import encore.backstage.command.types.ArgumentCollection
import encore.backstage.command.types.CommandResult
import encore.creation.UserCreationSubunit
import encore.fancam.Fancam
import encore.utils.types.Report
import mbworld.context.ServerContext
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
class DummySetupCommand(private val db: MongoDatabase): Command {
    override val commandId: String = "dummy-setup"
    override val description: String = "Prepare dummy activities for the website. " +
            "This will make use the DummyActivitySetup class."

    override suspend fun execute(
        serverContext: ServerContext,
        args: ArgumentCollection
    ): CommandResult {
        val numAccounts = (15..20).random()

        val insertedUsers = mutableListOf<String>()
        val addedTopics = mutableListOf<Pair<String, Long>>()

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
                profiles[acc.userId] = ProfileFactory.profile(acc.userId, acc.displayName)
                idsToUse.add(acc.userId)
                insertedUsers.add(creation.createUser("ignored", "ignored", "ignored"))
            }

            // 2. create topics
            insertedUsers.forEach { userId ->
                // 80% chance of post, 20% chance of no post
                if (Random.nextDouble() < 0.8) {
                    val numPostsEachAccounts = (1..7).random()
                    val topics = TopicFactory.topics(userId, numPostsEachAccounts)
                    for (topic in topics) {
                        serverContext.subunits.topic.addTopic(topic)
                        addedTopics.add(topic.topicId to topic.postedDate)
                    }
                }
            }

            // 3. create replies and comments
            addedTopics.forEach { (topicId, postedDate) ->
                // 70% chance of reply, 30% chance for no reply
                if (Random.nextDouble() < 0.7) {
                    val amountOfReply = (1..6).random()
                    val replies = List(amountOfReply) {
                        ReplyFactory.reply(
                            topicId = topicId,
                            topicPostDate = postedDate,
                            possibleReplyAuthors = insertedUsers,
                            possibleAmountofComments = 1..4,
                            possibleCommentAuthors = insertedUsers
                        )
                    }.sortedBy { it.postedDate }
                    replies.forEach { serverContext.subunits.reply.addReply(it) }
                }
            }
        } catch (e: Exception) {
            Fancam.error(e, "dummysetup") { "Scandal during dummy setup" }
            return CommandResult.Error("Scandal during dummy setup: ${e.message}")
        }

        Fancam.info { "Dummy setup completed." }
        return CommandResult.Executed("Dummy setup completed.")
    }
}
