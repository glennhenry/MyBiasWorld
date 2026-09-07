package mbworld.routes.common

import mbworld.mongo.collection.UserId

/**
 * Data of user's account which is needed by most pages
 * to display account information.
 *
 * The account information itself is usually optional,
 * this typically happens when user is not logged in yet.
 *
 * @property userId The [UserId].
 * @property username The username.
 * @property displayName The display name.
 */
data class AccountData(
    val userId: UserId,
    val username: String,
    val displayName: String
)
