package mbworld.domain.events

import encore.subunit.Subunit
import encore.subunit.scope.ServerScope
import mbworld.domain.events.model.Event
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Subunit that provides API for events.
 *
 * - Upload an event.
 * - Request for the latest events.
 */
class EventsSubunit : Subunit<ServerScope> {
    private val events = ConcurrentLinkedDeque<Event>()

    override suspend fun debut(scope: ServerScope): Result<Unit> {
        return runCatching { }
    }

    override suspend fun disband(scope: ServerScope): Result<Unit> {
        return runCatching { }
    }

    companion object {
        /**
         * Creates a test instance of [EventsSubunit].
         */
        fun createForTest(): EventsSubunit {
            return EventsSubunit()
        }
    }
}
