package mbworld.devtools

import detailedDateFormatter
import encore.backstage.command.Command
import encore.backstage.command.types.Argument
import encore.backstage.command.types.ArgumentInputs
import encore.backstage.command.types.ArgumentType
import encore.backstage.command.types.CommandResult
import encore.session.SessionSubunit
import encore.time.TimeCenter
import mbworld.context.ServerContext
import mbworld.domain.auth.session.SessionMemoryModel

/**
 * Command to list the active sessions of [SessionSubunit].
 */
class ListWebSessionCommand : Command {
    override val id: String = "list-web-session"
    override val name: String = "List web sessions"
    override val description: String = """
        List all active web sessions. This does not verify the sessions' validity.
        This is the WebsiteSessionSubunit and not SessionSubunit!
    """.trimIndent()
    override val arguments: List<Argument> = listOf(
        Argument("limit", "Limit", "Limit the results", ArgumentType.Int, 20)
    )

    override suspend fun execute(
        serverContext: ServerContext,
        args: ArgumentInputs
    ): CommandResult {
        val limit = args.getInt("limit")
        val sb = StringBuilder()

        val sessions = serverContext.subunits.websiteSession.getAllActiveSessions(limit)
        if (sessions.isEmpty()) {
            sb.appendLine("No web sessions.")
        } else {
            sb.appendLine("Found ${sessions.size} web sessions:")
        }
        sessions.forEachIndexed { idx, ses ->
            sb.appendLine("${idx + 1}) ${formatSessionInfo(ses)}")
            sb.appendLine()
        }

        return CommandResult.Success(sb.toString())
    }
}

/**
 * Format [SessionMemoryModel] into human-readable list.
 */
fun formatSessionInfo(ses: SessionMemoryModel): String {
    val isExpired = if (TimeCenter.now() > ses.expiresAt) {
        "yes"
    } else {
        "no"
    }
    return """
        userId=${ses.userId}
           token=${ses.token}
           expiresAt=${detailedDateFormatter.format(ses.expiresAt)}
           isExpired=$isExpired
    """.trimIndent()
}
