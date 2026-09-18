package mbworld.domain.activity

import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import mbworld.domain.activity.model.Activity
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Subunit that provides API for activities.
 *
 * - Upload an activity.
 * - Register and receive a specific source or type of activities.
 */
class ActivitySubunit : Subunit<ServerScope> {
    private val activities = ConcurrentLinkedDeque<Activity>()

    override suspend fun debut(scope: ServerScope): Result<Unit> {
        return runCatching { }
    }

    override suspend fun disband(scope: ServerScope): Result<Unit> {
        return runCatching { }
    }

    companion object {
        /**
         * Creates a test instance of [ActivitySubunit].
         */
        fun createForTest(): ActivitySubunit {
            return ActivitySubunit()
        }
    }
}
