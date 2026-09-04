package mbworld.domain.profile

import encore.fancam.Fancam
import encore.route.RouteHandler
import encore.route.guard
import encore.utils.types.okOrNull
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import mbworld.context.ServerContext
import mbworld.domain.profile.view.model.FanProfileModel
import mbworld.domain.profile.view.model.OverviewModel
import mbworld.routes.common.Action
import mbworld.routes.common.ErrorModel
import mbworld.routes.guard.OptionalAccountGuard
import mbworld.routes.guard.getAccountData

/**
 * Routes for profile, which is the page `/profile/@username/{...}`.
 */
class ProfileRoutes(private val serverContext: ServerContext) : RouteHandler {
    private val optionalAccountGuard = OptionalAccountGuard(serverContext)

    override fun Route.install() {
        get("/profile/{username}/{section}") {
            guard(call, optionalAccountGuard) {
                val username = requireNotNull(call.request.pathVariables["username"])
                val section = requireNotNull(call.request.pathVariables["section"])
                if (!username.startsWith("@")) {
                    call.respondRedirect("/profile/@$username/${section}", permanent = true)
                }
            }
        }

        get("/profile/random") {
            guard(call, optionalAccountGuard) {
                val username = serverContext.subunits.account.getRandomUsername().okOrNull() ?: run {
                    call.respond(HttpStatusCode.NotFound, "No user exist")
                }
                Fancam.debug { "Random profile request to '$username'" }
                call.respondRedirect("/profile/@$username/overview", permanent = false)
            }
        }

        get("/profile/@{username}/overview") {
            guard(call, optionalAccountGuard) {
                val username = requireNotNull(call.request.pathVariables["username"])

                val userId = serverContext.subunits.account.getUserIdByUsername(username)
                    .okOrNull() ?: run {
                    call.profileNotFound()
                    return@guard
                }

                val summary = serverContext.subunits.profile.getProfileOverview(userId).okOrNull() ?: run {
                    Fancam.warn { "UserId=$userId was previously found, but getProfileOverview failed" }
                    call.profileNotFound()
                    return@guard
                }

                val model = OverviewModel(
                    account = call.attributes.getAccountData(),
                    username = username,
                    displayName = summary.displayName,
                    avatarUrl = summary.avatarUrl,
                    country = summary.country,
                    birthday = summary.birthday,
                    bio = summary.bio
                )
                call.respond(ThymeleafContent("profile/overview", mapOf("data" to model)))
            }
        }

        get("/profile/@{username}/fan-profile") {
            guard(call, optionalAccountGuard) {
                val username = requireNotNull(call.request.pathVariables["username"])

                val userId = serverContext.subunits.account.getUserIdByUsername(username)
                    .okOrNull() ?: run {
                    call.profileNotFound()
                    return@guard
                }

                val summary = serverContext.subunits.profile.getFanProfile(userId).okOrNull() ?: run {
                    Fancam.warn { "UserId=$userId was previously found, but getFanProfile failed" }
                    call.profileNotFound()
                    return@guard
                }

                val model = FanProfileModel(
                    account = call.attributes.getAccountData(),
                    displayName = summary.displayName,
                    avatarUrl = summary.avatarUrl,
                    startedStan = summary.startedStan,
                    favoriteSong = summary.favoriteSong,
                    favoriteEra = summary.favoriteEra,
                    bias = summary.bias,
                    story = summary.story,
                )
                call.respond(ThymeleafContent("profile/fan-profile", mapOf("data" to model)))
            }
        }
    }
}

/**
 * Respond with an error page when the account profile is not found.
 */
suspend fun ApplicationCall.profileNotFound() {
    val data = ErrorModel(
        account = attributes.getAccountData(),
        title = "Profile not found",
        heading = "Profile not found",
        message = "The requested profile is not found.",
        action = Action("/", "Back to lobby")
    )
    respond(HttpStatusCode.NotFound, ThymeleafContent("error", mapOf("data" to data)))
}
