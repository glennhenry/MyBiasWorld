package mbworld.devtools

import bootstrap.CodecRegistry
import com.mongodb.kotlin.client.coroutine.MongoClient
import encore.backstage.command.Command
import encore.backstage.command.types.ArgumentCollection
import encore.backstage.command.types.CommandResult
import encore.serialization.JSON
import encore.venue.Venue
import mbworld.context.ServerContext
import mbworld.domain.cafe.collection.Section
import mbworld.domain.cafe.collection.Space
import mbworld.mongo.RuntimeMongoCollections
import java.io.File

/**
 * This command can be used to setup the database for MyBiasWorld.
 *
 * The setup for database includes:
 * 1. Creating the database (uses [encore.EncoreDatabaseConfig.dbName]) if not already exist.
 * 2. Preparing two collections of `spaces` and `sections`.
 *
 * This command will be ignored if both `spaces` and `sections` collection already exists.
 *
 * This command is typically used to re-setup the database for cafe compatibility
 * after the entire database is dropped.
 */
class SetupDatabaseCommand(private val mongoClient: MongoClient) : Command {
    override val commandId: String = "setupdb"
    override val description: String = """
    This command can be used to setup the database for MyBiasWorld.

    The setup for database includes:
    
    1. Creating the database (uses [encore.EncoreDatabaseConfig.dbName]) if not already exist.
    2. Preparing two collections of `spaces` and `sections`.
    
    This command will be ignored if both `spaces` and `sections` collection already exists.
    
    This command is typically used to re-setup the database for cafe compatibility
    after the entire database is dropped.
    """.trimIndent()

    override suspend fun execute(
        serverContext: ServerContext,
        args: ArgumentCollection
    ): CommandResult {
        val db = mongoClient.getDatabase(Venue.encore.database.dbName)
            .withCodecRegistry(CodecRegistry)

        // setup spaces
        val spaceFile = File("assets/spaces.json").also {
            if (!it.exists()) {
                return CommandResult.ExecutionFailure("assets/spaces.json does not exist")
            }
        }
        val spaceCollection = db.getCollection<Space>(RuntimeMongoCollections.spaces)
        val spaces = JSON.decode<List<Space>>(spaceFile.readText())
        spaceCollection.insertMany(spaces)

        // setup sections
        val sectionFile = File("assets/sections.json").also {
            if (!it.exists()) {
                return CommandResult.ExecutionFailure("assets/sections.json does not exist")
            }
        }
        val sectionCollection = db.getCollection<Section>(RuntimeMongoCollections.sections)
        val sections = JSON.decode<List<Section>>(sectionFile.readText())
        sectionCollection.insertMany(sections)

        return CommandResult.Executed("Inserted space and sections successfully")
    }
}
