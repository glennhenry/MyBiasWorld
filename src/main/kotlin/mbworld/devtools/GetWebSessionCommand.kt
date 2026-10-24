package mbworld.devtools

import detailedDateFormatter
import encore.backstage.command.Command
import encore.backstage.command.types.Argument
import encore.backstage.command.types.ArgumentInputs
import encore.backstage.command.types.ArgumentType
import encore.backstage.command.types.CommandResult
import encore.time.TimeCenter
import mbworld.context.ServerContext

/**
 * Command to get the details of web session by its token.
 */
class GetWebSessionCommand : Command {
    override val id: String = "verify-web-session"
    override val name: String = "Verify web session"
    override val description: String = """
        Get web session by its token, verify, and return its details.
        This is the WebsiteSessionSubunit and not SessionSubunit!
    """.trimIndent()
    override val arguments: List<Argument> = listOf(
        Argument("token", "Token", "Session unique token", ArgumentType.String)
    )

    override suspend fun execute(
        serverContext: ServerContext,
        args: ArgumentInputs
    ): CommandResult {
        val token = args.getString("token")
        if (token.isBlank()) {
            return CommandResult.ExecutionFailure("Please fill the session token.")
        }

        // getSession also verify too
        val ses = serverContext.subunits.websiteSession.getSession(token)
        if (ses != null) {
            val isExpired = if (TimeCenter.now() > ses.expiresAt) {
                "yes"
            } else {
                "no"
            }
            return CommandResult.Success(
                buildString {
                    appendLine("Session valid:")
                    appendLine("  userId: ${ses.userId}")
                    appendLine("  token: ${ses.token}")
                    appendLine("  expiresAt: ${detailedDateFormatter.format(ses.expiresAt)}")
                    appendLine("  isExpired: $isExpired")
                }
            )
        } else {
            return CommandResult.Success(
                "Session with token '$token' not found or invalid."
            )
        }
    }
}
