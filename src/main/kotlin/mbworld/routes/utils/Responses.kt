package mbworld.routes.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import mbworld.routes.common.Action
import mbworld.routes.common.ErrorModel
import mbworld.routes.guard.getAccountData

/**
 * Respond with an error page of BadRequest (400).
 */
suspend fun ApplicationCall.badRequest() {
    val data = ErrorModel(
        account = attributes.getAccountData(),
        title = "Bad request",
        heading = "Bad request",
        message = "",
        action = Action("/", "Back to lobby")
    )
    respond(HttpStatusCode.BadRequest, ThymeleafContent("error", mapOf("data" to data)))
}

/**
 * Respond with an error page of InternalServerError (500).
 */
suspend fun ApplicationCall.serverError(reason: String = "Internal server error") {
    respond(HttpStatusCode.InternalServerError, mapOf("reason" to reason))
}
