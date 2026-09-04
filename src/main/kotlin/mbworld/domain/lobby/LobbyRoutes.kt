package mbworld.domain.lobby

import encore.route.RouteHandler
import encore.route.guard
import encore.time.TimeCenter
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import mbworld.context.ServerContext
import mbworld.domain.Members
import mbworld.domain.lobby.model.LobbyModel
import mbworld.routes.guard.OptionalAccountGuard
import mbworld.routes.guard.getAccountData
import java.text.SimpleDateFormat

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
    }
}
