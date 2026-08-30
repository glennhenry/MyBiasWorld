package forum.domain.auth

import encore.route.RouteHandler
import encore.route.handle
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import forum.context.ServerContext
import forum.routes.common.BasicModel
import forum.routes.guard.MustNotHaveAccountGuard
import forum.routes.guard.OptionalAccountGuard
import forum.routes.guard.getAccountData

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
