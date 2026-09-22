package mbworld.domain.auth

import encore.auth.LoginResult
import encore.route.RouteHandler
import encore.route.guard.NoAuthGuard
import encore.route.handle
import encore.serialization.JSON
import encore.time.TimeCenter
import encore.utils.types.isFail
import encore.utils.types.okOrNull
import encore.utils.types.okOrThrow
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.date.*
import mbworld.context.ServerContext
import mbworld.domain.activity.model.Activity
import mbworld.domain.activity.model.ActivitySource
import mbworld.domain.auth.payload.LoginPayload
import mbworld.domain.auth.payload.RegisterPayload
import mbworld.routes.guard.OptionalAccountGuard
import mbworld.routes.guard.SessionAccountKey
import mbworld.routes.utils.serverError

const val yearInSeconds = 31_536_000L

/**
 * The routes for authentication related APIs. For authentication pages: [AuthPageRoutes].
 *
 * - `/api/register`
 * - `/api/login`
 * - `/api/logout`
 * - `/api/namecheck`
 * - `/api/emailcheck`
 */
class AuthApiRoutes(private val serverContext: ServerContext) : RouteHandler {
    private val optionalAccountGuard = OptionalAccountGuard(serverContext)
    private val usernameRegex = Regex("^[a-z0-9_]+$")

    override fun Route.install() {
        post("/api/register") {
            handle(call, optionalAccountGuard) {
                if (call.attributes.getOrNull(SessionAccountKey) != null) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("reason" to "You are already logged in."))
                    return@handle
                }

                val data = JSON.decode<RegisterPayload>(call.receiveText())

                if (data.username.isBlank() || data.password.isBlank() || data.email.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("reason" to "blank credentials"))
                    return@handle
                }

                if (data.username.length < 2 || !usernameRegex.matches(data.username)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("reason" to "invalid username minimum 2 length, only (a-z, 0-9, _)")
                    )
                    return@handle
                }

                val time = TimeCenter.now()
                val outcome = serverContext.subunits.auth
                    .register(data.username, data.password, data.email)

                if (outcome.isFail()) {
                    call.serverError()
                    return@handle
                }

                val userId = outcome.okOrThrow()
                serverContext.subunits.activity.publish(
                    Activity(
                        source = ActivitySource.Users,
                        type = UsersActivity.UserRegistered,
                        timestamp = time,
                        metadata = mapOf(
                            "userId" to userId,
                            "username" to data.username,
                            "email" to data.email,
                        )
                    )
                )

                call.response.cookies.append(
                    name = "session",
                    value = serverContext.subunits.websiteSession.create(outcome.okOrThrow()),
                    maxAge = yearInSeconds,
                    path = "/"
                )

                val returnTo = call.queryParameters["return"] ?: "/"
                call.respond(HttpStatusCode.OK, mapOf("url" to returnTo))
            }
        }

        post("/api/login") {
            handle(call, optionalAccountGuard) {
                if (call.attributes.getOrNull(SessionAccountKey) != null) {
                    call.respondText("You are already logged in.")
                    return@handle
                }

                val data = JSON.decode<LoginPayload>(call.receiveText())

                if (data.username.isBlank() || data.password.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("reason" to "blank credentials"))
                    return@handle
                }

                if (data.username.length < 2 || !usernameRegex.matches(data.username)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("reason" to "invalid username minimum 2 length, only (a-z, 0-9, _)")
                    )
                    return@handle
                }

                val result = serverContext.subunits.auth
                    .login(data.username, data.password).okOrNull() ?: run {
                    call.serverError()
                    return@handle
                }

                when (result) {
                    is LoginResult.AccountNotFound -> {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("reason" to "account not found")
                        )
                    }

                    is LoginResult.InvalidCredentials -> {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("reason" to "wrong password")
                        )
                    }

                    is LoginResult.Success -> {
                        call.response.cookies.append(
                            name = "session",
                            value = serverContext.subunits.websiteSession.create(result.userId),
                            maxAge = yearInSeconds,
                            path = "/"
                        )

                        val returnTo = call.queryParameters["return"] ?: "/"
                        call.respond(HttpStatusCode.OK, mapOf("url" to returnTo))
                    }
                }
            }
        }

        post("/api/logout") {
            handle(call, NoAuthGuard) {
                val token = call.request.cookies["session"]
                if (token == null) {
                    call.respond(HttpStatusCode.Forbidden, "Not logged in")
                    return@handle
                }

                serverContext.subunits.websiteSession.delete(token)
                call.response.cookies.delete("session", "/")
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/api/namecheck") {
            handle(call, NoAuthGuard) {
                val username = call.receiveText()
                if (username.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("reason" to "username is blank")
                    )
                    return@handle
                }

                val available = serverContext.subunits.auth
                    .isUsernameAvailable(username).okOrNull() ?: run {
                    call.serverError()
                    return@handle
                }

                call.respondText(if (available) "yes" else "no")
            }
        }

        post("/api/emailcheck") {
            handle(call, NoAuthGuard) {
                val email = call.receiveText()
                if (email.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("reason" to "email is blank")
                    )
                    return@handle
                }

                val available = serverContext.subunits.auth
                    .isEmailAvailable(email).okOrNull() ?: run {
                    call.serverError()
                    return@handle
                }

                call.respondText(if (available) "yes" else "no")
            }
        }
    }
}

fun ResponseCookies.delete(name: String, path: String) {
    append(
        name = name,
        value = "",
        encoding = CookieEncoding.URI_ENCODING,
        maxAge = 0,
        expires = GMTDate(),
        domain = null,
        path = path
    )
}
