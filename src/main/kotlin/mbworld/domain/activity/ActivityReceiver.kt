package mbworld.domain.activity

import mbworld.domain.activity.model.Activity
import mbworld.domain.activity.model.ActivitySource

/**
 * Represent a component that can receive an [Activity].
 *
 * Implementation populate [sources] and [types] to receive
 * a particular source or type of activities it demands for.
 * [onActivity] will be called once whenever such activity is published.
 *
 * Implementation should also register themselves with [ActivitySubunit.register].
 */
interface ActivityReceiver {
    /**
     * Set of [ActivitySource] to be received.
     * Use this to receive every types of activity from a particular source.
     */
    val sources: Set<ActivitySource>

    /**
     * Set of activity type to be received.
     * Use this to receive only a specific types of activity from any source.
     */
    val types: Set<String>

    /**
     * Receive [activity] and handle accordingly.
     */
    fun onActivity(activity: Activity)
}
