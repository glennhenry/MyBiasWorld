package mbworld.domain.profile.subunits

import encore.datastore.DocumentNotFoundException
import mbworld.domain.profile.model.FanProfileSummary
import mbworld.domain.profile.model.OverviewSummary
import mbworld.domain.profile.model.Profile
import mbworld.mongo.collection.UserId

interface ProfileRepository {
    /**
     * Insert a new profile.
     * @return [Result] type denoting success or failure.
     */
    suspend fun insert(profile: Profile): Result<Unit>

    /**
     * Get the profile associated with the given [userId].
     *
     * Returns:
     * - [Result.success] with the [Profile].
     * - [Result.failure] with [DocumentNotFoundException] if profile is not found.
     * - [Result.failure] if other error occurs while retrieving the data.
     */
    suspend fun getProfile(userId: UserId): Result<Profile>

    /**
     * Get the overview summary of [userId].
     * This queries the overview information of user's [Profile].
     *
     * Returns:
     * - [Result.success] with the [OverviewSummary].
     * - [Result.failure] with [DocumentNotFoundException] if profile is not found.
     * - [Result.failure] if other error occurs while retrieving the data.
     */
    suspend fun getProfileOverview(userId: UserId): Result<OverviewSummary>

    /**
     * Get the fan profile summary of [userId].
     * This queries the fan profile information of user's [Profile.fanProfile].
     *
     * Returns:
     * - [Result.success] with the [FanProfileSummary].
     * - [Result.failure] with [DocumentNotFoundException] if profile is not found.
     * - [Result.failure] if other error occurs while retrieving the data.
     */
    suspend fun getFanProfile(userId: UserId): Result<FanProfileSummary>

    /**
     * Get the user summary of [userId].
     * This cherry pick user's profile information to construct [UserSummary].
     *
     * Returns:
     * - [Result.success] with the [UserSummary].
     * - [Result.failure] with [DocumentNotFoundException] if user is not found.
     * - [Result.failure] if other error occurs while retrieving the data.
     */
    suspend fun getUserSummary(userId: UserId): Result<UserSummary>

    /**
     * Get the user summary of all users identified by `userId`
     * in the [userIds] list.
     *
     * Returns:
     * - [Result.success] with a map of each `userId` to the [UserSummary].
     *   If `userId` is not available in the map, it means the ID is not found.
     * - [Result.failure] if other error occurs while retrieving the data.
     */
    suspend fun getUserSummaries(userIds: List<UserId>): Result<Map<UserId, UserSummary>>
}
