package mbworld.devtools

import bootstrap.CodecRegistry
import com.mongodb.kotlin.client.coroutine.MongoClient
import encore.backstage.command.Command
import encore.backstage.command.types.ArgumentCollection
import encore.backstage.command.types.CommandResult
import encore.venue.Venue
import mbworld.context.ServerContext
import mbworld.domain.dummy.DummySetupCommand
import mbworld.mongo.collection.ServerObjects

/**
 * This command can be used to reset the database and re-setup it after.
 *
 * Actions:
 * 1. Drop the existing database of [encore.EncoreDatabaseConfig.dbName].
 * 2. Re-setup the database after using [SetupDatabaseCommand].
 *
 * This is typically used to delete the dummy activities created from
 * [DummySetupCommand]. The subsequent setup action, does not re-setup
 * every collection of the server, such as the [ServerObjects] collection.
 * For full re-setup, restart the server instead.
 */
class ResetDatabaseCommand(private val mongoClient: MongoClient) : Command {
    override val commandId: String = "resetdb"
    override val description: String = """
    This command can be used to reset the database and re-setup it after.
    
    Actions:
    1. Drop the existing database of [encore.EncoreDatabaseConfig.dbName].
    2. Re-setup the database after using [SetupDatabaseCommand].
    
    This is typically used to delete the dummy activities created from
    [DummySetupCommand]. The subsequent setup action, does not re-setup
    every collection of the server, such as the [ServerObjects] collection.
    For full re-setup, restart the server instead.
    """.trimIndent()

    override suspend fun execute(
        serverContext: ServerContext,
        args: ArgumentCollection
    ): CommandResult {
        val db = mongoClient.getDatabase(Venue.encore.database.dbName)
            .withCodecRegistry(CodecRegistry)
        db.drop()

        val res = serverContext.commandDispatcher.handleRawCommand("setupdb", serverContext)
        return CommandResult.Executed(
            "Dropped successfully; setup=${res.message}"
        )
    }
}
