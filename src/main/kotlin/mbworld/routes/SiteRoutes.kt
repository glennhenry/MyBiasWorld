package mbworld.routes

import encore.fancam.Fancam
import encore.route.RouteHandler
import encore.route.guard
import encore.route.guard.NoAuthGuard
import encore.route.handle
import encore.serialization.JSON
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import kotlinx.serialization.Serializable
import mbworld.context.ServerContext
import mbworld.routes.common.BasicModel
import mbworld.routes.guard.OptionalAccountGuard
import mbworld.routes.guard.getAccountData

/**
 * Contains simple, uncategorized, or unimplemented site routes that don't justify
 * its own route handler.
 */
class SiteRoutes(serverContext: ServerContext) : RouteHandler {
    private val optionalAccountGuard = OptionalAccountGuard(serverContext)

    override fun Route.install() {
        get("/about") {
            handle(call, optionalAccountGuard) {
                call.respond(
                    ThymeleafContent(
                        "about",
                        mapOf("data" to BasicModel(call.attributes.getAccountData()))
                    )
                )
            }
        }

        get("/feedback") {
            handle(call, optionalAccountGuard) {
                call.respond(
                    ThymeleafContent(
                        "feedback",
                        mapOf("data" to BasicModel(call.attributes.getAccountData()))
                    )
                )
            }
        }

        post("/feedback") {
            guard(call, NoAuthGuard) {
                val data = JSON.decode<FeedbackPayload>(call.receiveText())

                if (data.feedback.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("reason" to "blank feedback"))
                    return@guard
                }

                if (data.feedback.length < 10) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("reason" to "Need at least 10 characters")
                    )
                    return@guard
                }

                Fancam.info { "Feedback received: ${data.feedback}" }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

@Serializable
data class FeedbackPayload(
    val feedback: String
)
