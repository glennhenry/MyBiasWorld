package mbworld.domain.auth

import encore.route.RouteHandler
import encore.route.handle
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import mbworld.context.ServerContext
import mbworld.routes.common.BasicModel
import mbworld.routes.guard.MustNotHaveAccountGuard
import mbworld.routes.guard.OptionalAccountGuard
import mbworld.routes.guard.getAccountData

/**
 * The routes for authentication pages. For authentication API: [AuthApiRoutes].
 *
 * - `/register`
 * - `/login`
 * - `/logout`
 */
class AuthPageRoutes(serverContext: ServerContext) : RouteHandler {
    private val optionalAccountGuard = OptionalAccountGuard(serverContext)
    private val mustNotHaveAccountGuard = MustNotHaveAccountGuard(serverContext, serverContext.subunits.websiteSession)

    override fun Route.install() {
        get("/register") {
            handle(call, mustNotHaveAccountGuard) {
                call.respond(
                    ThymeleafContent(
                        "register",
                        mapOf("data" to BasicModel(null))
                    )
                )
            }
        }

        get("/login") {
            handle(call, mustNotHaveAccountGuard) {
                call.respond(
                    ThymeleafContent(
                        "login",
                        mapOf("data" to BasicModel(null))
                    )
                )
            }
        }

        get("/logout") {
            handle(call, optionalAccountGuard) {
                call.respond(
                    ThymeleafContent(
                        "logout", mapOf(
                            "data" to BasicModel(
                                account = call.attributes.getAccountData(),
                            )
                        )
                    )
                )
            }
        }
    }
}
