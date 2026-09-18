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
import mbworld.context.ServerContext
import mbworld.domain.Members
import mbworld.domain.lobby.view.model.LobbyModel
import mbworld.domain.lobby.view.response.ActivityData
import mbworld.domain.lobby.view.response.ActivityResponse
import mbworld.routes.guard.OptionalAccountGuard
import mbworld.routes.guard.getAccountData
import java.text.SimpleDateFormat
import kotlin.random.Random

/**
 * Routes for lobby, which is the root page `/`.
 */
class LobbyRoutes(private val serverContext: ServerContext) : RouteHandler {
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

        get("/activity") {
            guard(call, NoAuthGuard) {
                val response = ActivityResponse(
                    listOf(
                        ActivityData(
                            "First activity received (${Random.nextInt(1, 200)})",
                            TimeCenter.now()
                        ),
                        ActivityData(
                            "Hello this is an activity (${Random.nextInt(1, 200)})",
                            TimeCenter.now()
                        )
                    )
                )

                Fancam.debug { "Request to /activity" }

                call.respond(JSON.encode(response))
            }
        }
    }
}
