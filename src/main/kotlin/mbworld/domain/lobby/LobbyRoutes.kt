package mbworld.domain.lobby

import encore.fancam.Fancam
import encore.route.RouteHandler
import encore.route.guard
import encore.route.guard.NoAuthGuard
import encore.serialization.JSON
import encore.time.TimeCenter
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import kotlinx.serialization.Serializable
import mbworld.context.ServerContext
import mbworld.domain.Members
import mbworld.domain.lobby.model.LobbyModel
import mbworld.routes.guard.OptionalAccountGuard
import mbworld.routes.guard.getAccountData
import java.text.SimpleDateFormat
import kotlin.random.Random

/**
 * Routes for lobby, which is the root page `/`.
 */
class LobbyRoutes(serverContext: ServerContext) : RouteHandler {
    private val optionalAccountGuard = OptionalAccountGuard(serverContext)

    override fun Route.install() {
        get("/") {
            guard(call, optionalAccountGuard) {
                val systemTime = TimeCenter.now()
                val bias = Members.all.random()

                val data = LobbyModel(
                    account = call.attributes.getAccountData(),
                    time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(systemTime),
                    bias = bias,
                )

                call.respond(ThymeleafContent("lobby", mapOf("data" to data)))
            }
        }

        get("/events") {
            guard(call, NoAuthGuard) {
                val response = EventsResponse(
                    listOf(
                        Event(
                            "First event received (${Random.nextInt(1, 200)})",
                            TimeCenter.now()
                        ),
                        Event(
                            "Hello this is an event (${Random.nextInt(1, 200)})",
                            TimeCenter.now()
                        )
                    )
                )

                Fancam.debug { "Request to /events" }

                call.respond(JSON.encode(response))
            }
        }
    }
}


@Serializable
data class EventsResponse(
    val events: List<Event>
)

@Serializable
data class Event(
    val text: String,
    val happenedAt: Long
)
