package mbworld.context

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import encore.account.AccountSubunit
import encore.account.MongoAccountRepository
import encore.acts.ActIdStore
import encore.acts.StageActDirector
import encore.auth.AuthSubunit
import encore.backstage.command.CommandDispatcher
import encore.context.ContextFactory
import encore.creation.UserCreationSubunit
import encore.datastore.MongoDataStore
import encore.extra.MongoServerExtraRepository
import encore.extra.ServerExtraSubunit
import encore.session.SessionSubunit
import encore.subunit.SubunitLocator
import encore.subunit.scope.ServerScope
import encore.time.TimeCenter
import encore.websocket.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import mbworld.RealUserCreationFactory
import mbworld.domain.activity.ActivitySubunit
import mbworld.domain.auth.session.MongoSessionStore
import mbworld.domain.auth.session.WebsiteSessionSubunit
import mbworld.domain.cafe.collection.CollectionSubunit
import mbworld.domain.cafe.collection.MongoCollectionRepository
import mbworld.domain.cafe.likes.LikesSubunit
import mbworld.domain.cafe.likes.MongoLikesRepository
import mbworld.domain.cafe.reply.MongoReplyRepository
import mbworld.domain.cafe.reply.ReplySubunit
import mbworld.domain.cafe.topic.MongoTopicRepository
import mbworld.domain.cafe.topic.TopicSubunit
import mbworld.domain.lobby.activityFeed.ActivityFeedSubunit
import mbworld.domain.profile.subunits.MongoProfileRepository
import mbworld.domain.profile.subunits.ProfileSubunit
import mbworld.mongo.MongoCollections

/**
 * Real implementation of [ContextFactory].
 *
 * Context creation here is user-owned and must be updated accordingly.
 *
 * @property collections Mongo collection names
 * @property mongoDatabase Mongo database
 */
class RealContextFactory(
    private val collections: MongoCollections,
    private val mongoDatabase: MongoDatabase
) : ContextFactory {
    override suspend fun serverContext(
        appScope: CoroutineScope,
        serverSubunitScope: ServerScope
    ): ServerContext {
        /*... setup ServerContext ...*/

        val dataStore = MongoDataStore(
            db = mongoDatabase,
            collections = collections
        ).also { it.awaitInit() }

        val accountRepository = MongoAccountRepository(
            accountCollection = mongoDatabase.getCollection(collections.userAccount)
        )

        val stageActDirector = StageActDirector(
            timeSource = TimeCenter.source,
            actStore = ActIdStore
        )
        val commandDispatcher = CommandDispatcher()
        val webSocketManager = WebSocketManager()

        // setup ServerSubunits
        val accountSubunit = AccountSubunit(accountRepository)
        val extraSubunit = ServerExtraSubunit(
            MongoServerExtraRepository(mongoDatabase.getCollection(collections.serverObjects))
        )
        val sessionSubunit = SessionSubunit(appScope, TimeCenter.source)

        val profileRepository = MongoProfileRepository(
            profiles = mongoDatabase.getCollection(collections.profiles)
        )
        val profileSubunit = ProfileSubunit(profileRepository)

        val userCreationSubunit = UserCreationSubunit(dataStore, profileRepository, RealUserCreationFactory())
        val authSubunit = AuthSubunit(accountSubunit, userCreationSubunit)

        val sessionStore = MongoSessionStore(mongoDatabase.getCollection(collections.websiteSession))

        val topicRepository = MongoTopicRepository(
            topicCollection = mongoDatabase.getCollection(collections.topic)
        ).also { it.awaitInit() }
        val replyRepository = MongoReplyRepository(
            replies = mongoDatabase.getCollection(collections.reply)
        ).also { it.awaitInit() }
        val likesRepository = MongoLikesRepository(
            likes = mongoDatabase.getCollection(collections.likes)
        ).also { it.awaitInit() }
        val collectionRepository = MongoCollectionRepository(
            spaceCollection = mongoDatabase.getCollection(collections.spaces),
            sectionCollection = mongoDatabase.getCollection(collections.sections)
        )

        val websiteSession = WebsiteSessionSubunit(appScope, TimeCenter.source, sessionStore)
        val topicSubunit = TopicSubunit(topicRepository)
        val replySubunit = ReplySubunit(replyRepository)
        val likesSubunit = LikesSubunit(likesRepository)
        val collectionSubunit = CollectionSubunit(collectionRepository)
        val activitySubunit = ActivitySubunit(appScope).also { it.start() }
        val activityFeedSubunit = ActivityFeedSubunit()

        val subunits = ServerSubunits(
            account = accountSubunit,
            auth = authSubunit,
            extra = extraSubunit,
            session = sessionSubunit,

            websiteSession = websiteSession,
            profile = profileSubunit,
            topic = topicSubunit,
            reply = replySubunit,
            likes = likesSubunit,
            collection = collectionSubunit,
            activity = activitySubunit,
            activityFeed = activityFeedSubunit
        )

        // debut all subunits
        subunits.debut(serverSubunitScope)

        val serverContext = ServerContext(
            appScope = appScope,
            dataStore = dataStore,
            stageActDirector = stageActDirector,
            commandDispatcher = commandDispatcher,
            webSocketManager = webSocketManager,
            subunits = subunits,
            locator = SubunitLocator()
        )

        return serverContext
    }
}
