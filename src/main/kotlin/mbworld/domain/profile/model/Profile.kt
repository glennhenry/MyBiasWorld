package mbworld.domain.profile.model

import kotlinx.serialization.Serializable
import mbworld.mongo.collection.UserAccount

/**
 * User profile information.
 *
 * `Profile` contains the user's personal information and not including
 * system information, such as avatar, description, title, mood, etc.
 *
 * @property userId Unique identifier of the user. References [UserAccount.userId]
 * @property username Username of the user, unique. This should match [UserAccount.username].
 * @property displayName Display name of the user, non-unique. This should match [UserAccount.displayName].
 * @property avatarUrl Directory path that points to the user's avatar image.
 * @property country The country origin of user.
 * @property birthday The day when the user was born (or whatever they claim 😅).
 * @property bio Self-description of user about themselves.
 * @property fanProfile Fan-related profile of user.
 * @property gameProfile Game-related profile of user.
 * @property blockedUsers The list of blocked users.
 * @property stats Various stats of user in the platform.
 */
@Serializable
data class Profile(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String = "avatars/duck.jpg",
    val country: String,
    val birthday: String,
    val bio: String,
    val fanProfile: FanProfile,
    val gameProfile: GameProfile,
    val blockedUsers: List<String>,
    val stats: UsersStats
)
