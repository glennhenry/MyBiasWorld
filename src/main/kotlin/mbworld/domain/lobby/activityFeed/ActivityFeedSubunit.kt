package mbworld.domain.lobby.activityFeed

import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import mbworld.domain.activity.ActivityReceiver
import mbworld.domain.activity.model.Activity
import mbworld.domain.activity.model.ActivitySource
import mbworld.domain.activity.model.AllActivitySources
import mbworld.utils.CircularList

/**
 * Subunit that handles the activity feed of lobby.
 *
 * The activity feed is a list that shows the surrounding activities
 * that happened around the platform. This includes user registration,
 * user interaction, user achievements, cafe activity, and many more.
 *
 * This subunit keeps track an internal list of feed.
 * Use [retrieve] to retrieve a particular amount of the latest feed data.
 *
 * Feeds aren't permanent and exist for informational purpose, not bookeeping.
 * Whenever the list exceeds the [feedLimit], the old ones are going to be overwritten.
 *
 * @param feedLimit Specify the maximum amount of feed to be stored.
 */
class ActivityFeedSubunit(private val feedLimit: Int = 30) : Subunit<ServerScope>, ActivityReceiver {
    private val feeds = CircularList<ActivityFeedData>(feedLimit)

    override val sources: Set<ActivitySource> = AllActivitySources
    override val types: Set<String> = setOf()

    /**
     * Retrieve the latest [amount] of feed.
     *
     * Feeds are sorted based on its occurence time, where the first element
     * is the latest.
     *
     * @return a list of [ActivityFeedData].
     */
    fun retrieve(amount: Int): List<ActivityFeedData> {
        return feeds.get(amount)
    }

    override fun onActivity(activity: Activity) {
        feeds.add(ActivityFeedData(activity.timestamp, activity.type))
    }

    override suspend fun debut(scope: ServerScope): Result<Unit> {
        return runCatching { }
    }

    override suspend fun disband(scope: ServerScope): Result<Unit> {
        return runCatching { }
    }

    companion object {
        /**
         * Creates a test instance of [ActivityFeedSubunit].
         */
        fun createForTest(): ActivityFeedSubunit {
            return ActivityFeedSubunit()
        }
    }
}
