package mbworld.devtools

import encore.backstage.command.Command
import encore.backstage.command.types.Argument
import encore.backstage.command.types.ArgumentInputs
import encore.backstage.command.types.CommandResult
import mbworld.context.ServerContext

/**
 * Command to clear all web sessions in the WebSessionSubunit
 */
class ClearWebSessionCommand : Command {
    override val id: String = "clear-web-sessions"
    override val name: String = "Clear all web sessions"
    override val description: String = """
        Clear all web sessions on WebSessionSubunit
        This is the WebsiteSessionSubunit and not SessionSubunit!
    """.trimIndent()
    override val arguments: List<Argument> = emptyList()

    override suspend fun execute(
        serverContext: ServerContext,
        args: ArgumentInputs
    ): CommandResult {
        val total = serverContext.subunits.websiteSession.clearAllSessions()
        return CommandResult.Success("Cleared $total total web sessions.")
    }
}
