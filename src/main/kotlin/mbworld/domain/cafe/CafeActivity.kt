@file:Suppress("ConstPropertyName", "unused")

package mbworld.domain.cafe

import mbworld.domain.activity.model.ActivitySource

/**
 * The types of cafe activities [ActivitySource.Cafe].
 */
object CafeActivity {
    const val TopicCreated = "CafeActivity.Topic.Created"
    const val TopicDeleted = "CafeActivity.Topic.Deleted"
    const val TopicLiked = "CafeActivity.Topic.Liked"
    const val TopicUnliked = "CafeActivity.Topic.Unliked"
    const val ReplyAdded = "CafeActivity.Reply.Added"
    const val ReplyLiked = "CafeActivity.Reply.Liked"
    const val ReplyUnliked = "CafeActivity.Reply.Unliked"
    const val CommentAdded = "CafeActivity.Comment.Added"
}
