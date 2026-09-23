package mbworld.domain.activity

import encore.fancam.Fancam
import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import encore.utils.support.className
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mbworld.domain.activity.model.Activity
import mbworld.domain.activity.model.ActivitySource
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Subunit that provides API for activities.
 *
 * - [publish]: to send a new activity.
 * - [register]: register an [ActivityReceiver] to receive a specific source
 *   or type of activities.
 *
 * @param parentScope The parent coroutine scope that holds the lifecycle.
 */
class ActivitySubunit(
    private val parentScope: CoroutineScope
) : Subunit<ServerScope> {
    private val activities = Channel<Activity>()
    private val bySources = mutableMapOf<ActivitySource, MutableSet<ActivityReceiver>>()
    private val byTypes = mutableMapOf<String, MutableSet<ActivityReceiver>>()

    /**
     * Start the subunit to listen and call receiver for activities.
     */
    fun start() {
        parentScope.launch {
            for (activity in activities) {
                val toBeCalled = mutableSetOf<ActivityReceiver>()
                toBeCalled.addAll(byTypes[activity.type].orEmpty())
                toBeCalled.addAll(bySources[activity.source].orEmpty())
                toBeCalled.forEach { receiver ->
                    runCatching {
                        receiver.onActivity(activity)
                    }.onFailure {
                        Fancam.error(it) { "Failure on activity receiver: ${receiver.className()}" }
                    }
                }
            }
        }
    }

    /**
     * Register a [receiver].
     */
    fun register(receiver: ActivityReceiver) {
        for (source in receiver.sources) {
            bySources
                .getOrPut(source) { mutableSetOf() }
                .add(receiver)
        }
        for (type in receiver.types) {
            byTypes
                .getOrPut(type) { mutableSetOf() }
                .add(receiver)
        }
    }

    /**
     * Publish the [activity] for receivers.
     */
    suspend fun publish(activity: Activity) {
        activities.send(activity)
    }

    override suspend fun debut(scope: ServerScope): Result<Unit> {
        return runCatching { }
    }

    override suspend fun disband(scope: ServerScope): Result<Unit> {
        return runCatching { }
    }

    companion object {
        /**
         * Creates a test instance of [ActivitySubunit].
         * @param parentScope scope used for lifecycle and cleanup job (e.g., `TestScope`).
         */
        fun createForTest(parentScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)): ActivitySubunit {
            return ActivitySubunit(parentScope)
        }
    }
}
