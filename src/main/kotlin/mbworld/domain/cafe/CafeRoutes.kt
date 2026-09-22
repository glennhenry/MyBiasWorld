package mbworld.domain.cafe

import encore.fancam.Fancam
import encore.route.RouteHandler
import encore.route.guard
import encore.route.handle
import encore.serialization.JSON
import encore.time.TimeCenter
import encore.utils.identifier.Ids
import encore.utils.identifier.shortUuid
import encore.utils.types.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import mbworld.context.ServerContext
import mbworld.domain.activity.model.Activity
import mbworld.domain.activity.model.ActivitySource
import mbworld.domain.cafe.reply.Comment
import mbworld.domain.cafe.reply.Reply
import mbworld.domain.cafe.topic.Topic
import mbworld.domain.cafe.topic.TopicDeletionOutcome
import mbworld.domain.cafe.view.model.*
import mbworld.domain.cafe.view.payload.CommentPayload
import mbworld.domain.cafe.view.payload.ReplyPayload
import mbworld.domain.cafe.view.payload.WriteTopicPayload
import mbworld.routes.common.Action
import mbworld.routes.common.ErrorModel
import mbworld.routes.guard.OptionalAccountGuard
import mbworld.routes.guard.RequireAccountGuard
import mbworld.routes.guard.getAccountData
import mbworld.routes.guard.getUserAccount
import mbworld.routes.utils.serverError

val Sections = mapOf(
    "kep1er" to "Kep1er Discussion",
    "kpop" to "K-pop Discussion",
    "yujin" to "Yujin's Space",
    "xiaoting" to "Xiaoting's Space",
    "mashiro" to "Mashiro's Space",
    "chaehyun" to "Chaehyun's Space",
    "dayeon" to "Dayeon's Space",
    "hikaru" to "Hikaru's Space",
    "hiyyih" to "Hiyyih's Space",
    "youngeun" to "Youngeun's Space",
    "yeseo" to "Yeseo's Space",
    "media" to "Media",
    "games" to "Games"
)

class CafeRoutes(private val serverContext: ServerContext) : RouteHandler {
    private val optionalAccountGuard = OptionalAccountGuard(serverContext)
    private val requireAccountGuard = RequireAccountGuard(serverContext)

    override fun Route.install() {
        get("/cafe") {
            guard(call, optionalAccountGuard) {
                val spaces = serverContext.subunits.collection.getSpacesForLandingModel()
                val counts = serverContext.subunits.topic.getTopicsCountForEachSection().okOrThrow()

                val data = CafeModel(
                    account = call.attributes.getAccountData(),
                    spaces = spaces,
                    topicCounts = counts
                )

                call.respond(ThymeleafContent("cafe/cafe", mapOf("data" to data)))
            }
        }

        get("/cafe/{section}") {
            guard(call, optionalAccountGuard) {
                val section = requireNotNull(call.request.pathVariables["section"])

                if (!Sections.contains(section)) {
                    call.sectionNotFound()
                    return@guard
                }

                val topics = serverContext.subunits.topic.getTopicsOfSection(section).okOrNull()
                if (topics == null) {
                    call.respond(HttpStatusCode.InternalServerError, "internal server error")
                    return@guard
                }

                val authorIds = mutableListOf<String>()
                val topicIds = mutableListOf<String>()

                for ((topicId, _, _, authorId) in topics) {
                    authorIds.add(authorId)
                    topicIds.add(topicId)
                }

                val summaries = serverContext.subunits.profile
                    .getUserSummaries(authorIds)
                    .okOrNull() ?: run {
                    call.respond(HttpStatusCode.InternalServerError, "internal server error")
                    return@guard
                }

                val replyCounts = serverContext.subunits.reply
                    .getReplyCounts(topicIds)
                    .okOrNull() ?: run {
                    call.respond(HttpStatusCode.InternalServerError, "internal server error")
                    return@guard
                }

                val data = TopicListModel(
                    account = call.attributes.getAccountData(),
                    sectionId = section,
                    topics = topics.map {
                        val authorDisplayName = summaries[it.authorId]?.displayName ?: run {
                            Fancam.warn { "authorDisplayName of ${it.topicId} (title=${it.title}) is null" }
                            "<authorDisplayName:null>"
                        }
                        val authorUsername = summaries[it.authorId]?.username ?: run {
                            Fancam.warn { "authorUsername of ${it.topicId} (title=${it.title}) is null" }
                            "<authorUsername:null>"
                        }
                        TopicListItemData(
                            topicId = it.topicId,
                            link = "${section}/${it.topicId.shortUuid()}/${it.title.toUrlSlug()}",
                            title = it.title,
                            authorName = authorDisplayName,
                            authorProfileUrl = "/profile/@${authorUsername}/overview",
                            replyCount = replyCounts[it.topicId] ?: 0,
                            likesCount = it.likes,
                            postedDate = it.postedDate
                        )
                    }
                )

                call.respond(ThymeleafContent("cafe/topiclist", mapOf("data" to data)))
            }
        }

        get("/cafe/{section}/write") {
            handle(call, requireAccountGuard) {
                val section = requireNotNull(call.request.pathVariables["section"])
                if (!Sections.contains(section)) {
                    call.respond(HttpStatusCode.NotFound, "Section not found")
                    return@handle
                }

                call.respond(
                    ThymeleafContent(
                        "cafe/writetopic",
                        mapOf(
                            "data" to WriteTopicModel(
                                account = call.attributes.getAccountData(),
                                sectionName = requireNotNull(Sections[section]) { "Ensure Sections contains $section" }
                            )
                        )
                    )
                )
            }
        }

        post("/cafe/{section}/write") {
            handle(call, requireAccountGuard) {
                val section = requireNotNull(call.request.pathVariables["section"])
                if (!Sections.contains(section)) {
                    call.respond(HttpStatusCode.NotFound, "Section not found")
                    return@handle
                }

                val post = JSON.decode<WriteTopicPayload>(call.receiveText())

                if (post.title.length < 10 || post.content.length < 20) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Title should be at least 10 characters and content should contains at least 20 characters"
                    )
                    return@handle
                }

                val acc = call.attributes.getUserAccount()

                val time = TimeCenter.now()
                val id = Ids.uuid()
                val topic = Topic(
                    topicId = id,
                    sectionId = section,
                    title = post.title,
                    authorId = acc.userId,
                    content = post.content,
                    likes = 0,
                    postedDate = time
                )
                serverContext.subunits.topic.addTopic(topic)
                    .onFail {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to post")
                        return@handle
                    }

                serverContext.subunits.activity.publish(
                    Activity(
                        source = ActivitySource.Cafe,
                        type = CafeActivity.TopicCreated,
                        timestamp = time,
                        metadata = mapOf(
                            "topicId" to id,
                            "authorId" to acc.userId
                        )
                    )
                )

                Fancam.debug { "Created new topicId=$id" }
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/cafe/delete") {
            guard(call, requireAccountGuard) {
                val topicId = call.receiveText()

                when (val outcome = serverContext.subunits.topic.deleteTopic(topicId)) {
                    is Outcome.Fail ->
                        call.respond(HttpStatusCode.InternalServerError)

                    is Outcome.Ok -> when (outcome.value) {
                        TopicDeletionOutcome.Success -> {
                            serverContext.subunits.activity.publish(
                                Activity(
                                    source = ActivitySource.Cafe,
                                    type = CafeActivity.TopicDeleted,
                                    timestamp = TimeCenter.now(),
                                    metadata = mapOf(
                                        "topicId" to topicId
                                    )
                                )
                            )
                            call.respond(HttpStatusCode.NoContent)
                        }

                        TopicDeletionOutcome.TopicNotDeleted ->
                            call.respond(HttpStatusCode.NotFound, "Topic not found")
                    }
                }
            }
        }

        get("/cafe/{section}/{id}/{title}") {
            guard(call, optionalAccountGuard) {
                val section = requireNotNull(call.request.pathVariables["section"])
                val id = requireNotNull(call.request.pathVariables["id"])
                val title = requireNotNull(call.request.pathVariables["title"])

                if (!Sections.contains(section)) {
                    call.sectionNotFound()
                    return@guard
                }

                val topic = serverContext.subunits.topic.getTopicByShortId(id).okOrNull()
                if (topic == null) {
                    call.topicNotFound()
                    return@guard
                }

                // title from link is different than title in DB: redirect this
                val currentSlug = topic.title.toUrlSlug()
                if (title != currentSlug) {
                    call.respondRedirect(
                        url = "/cafe/$section/$id/$currentSlug",
                        permanent = true
                    )
                    return@guard
                }
                val authors = mutableListOf(topic.authorId)

                val allPostIds = mutableListOf(topic.topicId)
                val replies = serverContext.subunits.reply.getRepliesUnder(topic.topicId).okOrNull() ?: emptyList()
                for ((replyId, _, authorId, _, _, _, comments) in replies) {
                    allPostIds.add(replyId)
                    authors.add(authorId)
                    for ((_, authorId2) in comments) {
                        authors.add(authorId2)
                    }
                }

                val summaries = serverContext.subunits.profile.getUserSummaries(authors.distinct()).okOrThrow()
                val topicAuthorSummary = summaries[topic.authorId]

                // gather liked posts if logged in
                val likedPosts = mutableMapOf<String, Long>()
                val account = call.attributes.getAccountData()
                val isLoggedIn = call.attributes.getAccountData() != null
                if (isLoggedIn) {
                    serverContext.subunits.likes.likedPosts(account!!.userId, allPostIds)
                        .okOrNull()
                        ?.forEach { (postId, timestamp) ->
                            // if a postId was in allPostIds but not in the map,
                            // that means the post is not liked
                            likedPosts[postId] = timestamp
                        }
                }

                val data = TopicViewModel(
                    account = account,
                    sectionName = requireNotNull(Sections[section]) { "Ensure Sections contains $section" },
                    topicId = topic.topicId,
                    topic = TopicViewData(
                        title = topic.title,
                        authorUserId = topicAuthorSummary?.userId ?: "<topicAuthor.userId:null>",
                        authorDisplayName = topicAuthorSummary?.displayName ?: "<topicAuthor.displayName:null>",
                        authorAvatarUrl = topicAuthorSummary?.avatarUrl ?: "<topicAuthor.avatarUrl:null>",
                        authorProfileUrl = topicAuthorSummary?.username?.let { "/profile/@${it}/overview" } ?: "#",
                        postedDate = topic.postedDate,
                        content = topic.content,
                        likesCount = topic.likes,
                        isLikedByUser = isLoggedIn && likedPosts[topic.topicId] != null
                    ),
                    replies = replies.map { reply ->
                        val replyAuthorSummary = summaries[reply.authorId]
                        ReplyData(
                            replyId = reply.replyId,
                            authorUserId = replyAuthorSummary?.userId ?: "<replyAuthor.userId:null>",
                            authorDisplayName = replyAuthorSummary?.displayName ?: "<replyAuthor.displayName:null>",
                            authorAvatarUrl = replyAuthorSummary?.avatarUrl ?: "<replyAuthor.avatarUrl:null>",
                            authorProfileUrl = replyAuthorSummary?.username?.let { "/profile/@${it}/overview" } ?: "#",
                            content = reply.content,
                            postedDate = reply.postedDate,
                            likesCount = reply.likes,
                            isLikedByUser = isLoggedIn && likedPosts[reply.replyId] != null,
                            comments = reply.comments.map { comment ->
                                val commentAuthorSummary = summaries[comment.authorId]
                                CommentData(
                                    commentId = comment.commentId,
                                    authorUserId = commentAuthorSummary?.userId ?: "<commentAuthor.userId:null>",
                                    authorDisplayName = commentAuthorSummary?.displayName
                                        ?: "<commentAuthor.displayName:null>",
                                    authorAvatarUrl = commentAuthorSummary?.avatarUrl
                                        ?: "<commentAuthor.avatarUrl:null>",
                                    authorProfileUrl = commentAuthorSummary?.username?.let { "/profile/@${it}/overview" }
                                        ?: "#",
                                    postedDate = comment.postedDate,
                                    content = comment.content
                                )
                            }
                        )
                    }
                )

                call.respond(ThymeleafContent("cafe/topicview", mapOf("data" to data)))
            }
        }

        post("/cafe/{section}/{id}/{title}") {
            guard(call, requireAccountGuard) {
                val section = requireNotNull(call.request.pathVariables["section"])
                val id = requireNotNull(call.request.pathVariables["id"])

                if (!Sections.contains(section)) {
                    call.sectionNotFound()
                    return@guard
                }

                val topicId = serverContext.subunits.topic.getFullTopicId(id).okOrNull()
                if (topicId == null) {
                    call.topicNotFound()
                    return@guard
                }

                val replyPayload = JSON.decode<ReplyPayload>(call.receiveText())
                if (replyPayload.reply.length < 20) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Reply should be at least contains 20 characters."
                    )
                    return@guard
                }

                val time = TimeCenter.now()
                val replyId = Ids.uuid()
                val reply = Reply(
                    replyId = replyId,
                    topicId = topicId,
                    authorId = call.attributes.getUserAccount().userId,
                    content = replyPayload.reply,
                    likes = 0,
                    postedDate = time,
                    comments = emptyList()
                )

                serverContext.subunits.reply.addReply(reply)
                    .onFail {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to reply")
                        return@guard
                    }

                serverContext.subunits.activity.publish(
                    Activity(
                        source = ActivitySource.Cafe,
                        type = CafeActivity.ReplyAdded,
                        timestamp = time,
                        metadata = mapOf(
                            "topicId" to topicId,
                            "replyId" to replyId,
                            "authorId" to call.attributes.getUserAccount().userId
                        )
                    )
                )

                Fancam.debug { "Created new replyId=$replyId" }
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/cafe/{section}/{id}/{title}/{replyId}") {
            guard(call, requireAccountGuard) {
                val section = requireNotNull(call.request.pathVariables["section"])
                if (!Sections.contains(section)) {
                    call.sectionNotFound()
                    return@guard
                }

                val id = requireNotNull(call.request.pathVariables["id"])
                val topicId = serverContext.subunits.topic.getFullTopicId(id).okOrNull()
                if (topicId == null) {
                    call.topicNotFound()
                    return@guard
                }

                val replyId = requireNotNull(call.request.pathVariables["replyId"])
                val reply = serverContext.subunits.reply.getReply(replyId).okOrNull() ?: run {
                    call.respond(HttpStatusCode.NotFound, "reply not found")
                    return@guard
                }

                val commentPayload = JSON.decode<CommentPayload>(call.receiveText())
                if (commentPayload.comment.length < 10) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Comment should be at least contains 10 characters."
                    )
                    return@guard
                }

                val time = TimeCenter.now()
                val commentId = Ids.uuid()
                val comment = Comment(
                    commentId = commentId,
                    authorId = call.attributes.getUserAccount().userId,
                    content = commentPayload.comment,
                    postedDate = time
                )

                serverContext.subunits.reply.addComment(replyId, comment)
                    .onFail {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to post comment")
                        return@guard
                    }

                serverContext.subunits.activity.publish(
                    Activity(
                        source = ActivitySource.Cafe,
                        type = CafeActivity.CommentAdded,
                        timestamp = time,
                        metadata = mapOf(
                            "topicId" to topicId,
                            "replyId" to replyId,
                            "commentId" to commentId,
                            "authorId" to call.attributes.getUserAccount().userId
                        )
                    )
                )

                Fancam.debug { "Created new commentId=$commentId" }
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/cafe/{section}/{id}/{title}/like") {
            guard(call, requireAccountGuard) {
                val section = requireNotNull(call.request.pathVariables["section"])
                val id = requireNotNull(call.request.pathVariables["id"])

                if (!Sections.contains(section)) {
                    call.sectionNotFound()
                    return@guard
                }

                val topicId = serverContext.subunits.topic.getFullTopicId(id).okOrNull()
                if (topicId == null) {
                    call.topicNotFound()
                    return@guard
                }

                val userId = call.attributes.getUserAccount().userId

                // if post is already liked -> ignore
                val outcome = serverContext.subunits.likes.isPostLikedBy(userId, topicId)
                if (outcome.isFail()) {
                    call.serverError()
                    return@guard
                }

                val likeCastedAt = outcome.okOrThrow()
                if (likeCastedAt != null) {
                    call.respond(HttpStatusCode.OK, "post is already liked")
                    return@guard
                }

                val time = TimeCenter.now()

                // if post is not yet liked -> add the like and increment topic's like
                serverContext.subunits.likes.addLike(userId, topicId)
                    .onFail {
                        call.serverError()
                        return@guard
                    }

                serverContext.subunits.topic.incrementLike(topicId)
                    .onFail {
                        call.serverError()
                        return@guard
                    }

                serverContext.subunits.activity.publish(
                    Activity(
                        source = ActivitySource.Cafe,
                        type = CafeActivity.TopicLiked,
                        timestamp = time,
                        metadata = mapOf(
                            "topicId" to topicId,
                            "authorId" to userId
                        )
                    )
                )

                Fancam.debug { "${call.attributes.getUserAccount().username} liked topic=$topicId" }
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/cafe/{section}/{id}/{title}/unlike") {
            guard(call, requireAccountGuard) {
                val section = requireNotNull(call.request.pathVariables["section"])
                val id = requireNotNull(call.request.pathVariables["id"])

                if (!Sections.contains(section)) {
                    call.sectionNotFound()
                    return@guard
                }

                val topicId = serverContext.subunits.topic.getFullTopicId(id).okOrNull()
                if (topicId == null) {
                    call.topicNotFound()
                    return@guard
                }

                val userId = call.attributes.getUserAccount().userId
                val time = TimeCenter.now()

                // like exist or not -> remove the like and decrement
                serverContext.subunits.likes.removeLike(userId, topicId)
                    .onFail {
                        call.serverError()
                        return@guard
                    }

                serverContext.subunits.topic.decrementLike(topicId)
                    .onFail {
                        call.serverError()
                        return@guard
                    }

                serverContext.subunits.activity.publish(
                    Activity(
                        source = ActivitySource.Cafe,
                        type = CafeActivity.TopicUnliked,
                        timestamp = time,
                        metadata = mapOf(
                            "topicId" to topicId,
                            "authorId" to userId
                        )
                    )
                )

                Fancam.debug { "${call.attributes.getUserAccount().username} unliked topic=$topicId" }
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/cafe/{section}/{id}/{title}/{replyId}/like") {
            guard(call, requireAccountGuard) {
                val section = requireNotNull(call.request.pathVariables["section"])
                if (!Sections.contains(section)) {
                    call.sectionNotFound()
                    return@guard
                }

                val replyId = requireNotNull(call.request.pathVariables["replyId"])
                val userId = call.attributes.getUserAccount().userId

                // if post is already liked -> ignore
                val outcome = serverContext.subunits.likes.isPostLikedBy(userId, replyId)
                if (outcome.isFail()) {
                    call.serverError()
                    return@guard
                }

                val likeCastedAt = outcome.okOrThrow()
                if (likeCastedAt != null) {
                    call.respond(HttpStatusCode.OK, "post is already liked")
                    return@guard
                }

                val time = TimeCenter.now()

                // if post is not yet liked -> add the like and increment topic's like
                serverContext.subunits.likes.addLike(userId, replyId)
                    .onFail {
                        call.serverError()
                        return@guard
                    }

                serverContext.subunits.reply.incrementLike(replyId)
                    .onFail {
                        call.serverError()
                        return@guard
                    }

                serverContext.subunits.activity.publish(
                    Activity(
                        source = ActivitySource.Cafe,
                        type = CafeActivity.ReplyLiked,
                        timestamp = time,
                        metadata = mapOf(
                            "replyId" to replyId,
                            "authorId" to userId
                        )
                    )
                )

                Fancam.debug { "${call.attributes.getUserAccount().username} liked reply=$replyId" }
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/cafe/{section}/{id}/{title}/{replyId}/unlike") {
            guard(call, requireAccountGuard) {
                val section = requireNotNull(call.request.pathVariables["section"])
                if (!Sections.contains(section)) {
                    call.sectionNotFound()
                    return@guard
                }

                val replyId = requireNotNull(call.request.pathVariables["replyId"])
                val userId = call.attributes.getUserAccount().userId
                val time = TimeCenter.now()

                // like exist or not -> remove the like and decrement
                serverContext.subunits.likes.removeLike(userId, replyId)
                    .onFail {
                        call.serverError()
                        return@guard
                    }

                serverContext.subunits.reply.decrementLike(replyId)
                    .onFail {
                        call.serverError()
                        return@guard
                    }

                serverContext.subunits.activity.publish(
                    Activity(
                        source = ActivitySource.Cafe,
                        type = CafeActivity.ReplyUnliked,
                        timestamp = time,
                        metadata = mapOf(
                            "replyId" to replyId,
                            "authorId" to userId
                        )
                    )
                )

                Fancam.debug { "${call.attributes.getUserAccount().username} unliked reply=$replyId" }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

/**
 * Respond with an error page when the requested section is not found.
 */
suspend fun ApplicationCall.sectionNotFound() {
    val data = ErrorModel(
        account = attributes.getAccountData(),
        title = "Not Found",
        heading = "Section not found",
        message = "",
        action = Action("/", "Back to lobby")
    )
    respond(HttpStatusCode.NotFound, ThymeleafContent("error", mapOf("data" to data)))
}

/**
 * Respond with an error page when the requested topic is not found.
 */
suspend fun ApplicationCall.topicNotFound() {
    val data = ErrorModel(
        account = attributes.getAccountData(),
        title = "Not Found",
        heading = "Topic not found",
        message = "",
        action = Action("/", "Back to lobby")
    )
    respond(HttpStatusCode.NotFound, ThymeleafContent("error", mapOf("data" to data)))
}
