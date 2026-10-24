package mbworld.devtools

import bootstrap.CodecRegistry
import com.mongodb.kotlin.client.coroutine.MongoClient
import encore.backstage.command.Command
import encore.backstage.command.types.Argument
import encore.backstage.command.types.ArgumentInputs
import encore.backstage.command.types.CommandResult
import encore.utils.types.onFail
import encore.venue.Venue
import mbworld.context.ServerContext
import mbworld.domain.cafe.collection.Section
import mbworld.domain.cafe.collection.Space
import mbworld.mongo.RuntimeMongoCollections
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

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
    override val id: String = "setupdb"
    override val name: String = "Setup DB"
    override val description: String = """
    This command can be used to setup the database for MyBiasWorld.

    The setup for database includes:
    
    1. Creating the database (uses [encore.EncoreDatabaseConfig.dbName]) if not already exist.
    2. Preparing two collections of `spaces` and `sections`.
    
    This command will be ignored if both `spaces` and `sections` collection already exists.
    
    This command is typically used to re-setup the database for cafe compatibility
    after the entire database is dropped.
    """.trimIndent()
    override val arguments: List<Argument> = emptyList()

    override suspend fun execute(
        serverContext: ServerContext,
        args: ArgumentInputs
    ): CommandResult {
        val db = mongoClient.getDatabase(Venue.encore.database.dbName)
            .withCodecRegistry(CodecRegistry)

        // setup spaces
        val spaceFile = File("assets/spaces.xml").also {
            if (!it.exists()) {
                return CommandResult.ExecutionFailure("assets/spaces.xml does not exist")
            }
        }
        val spaces = readSpacesXml(spaceFile)
        val spaceCollection = db.getCollection<Space>(RuntimeMongoCollections.spaces)
        spaceCollection.insertMany(spaces)

        // setup sections
        val sectionFile = File("assets/sections.xml").also {
            if (!it.exists()) {
                return CommandResult.ExecutionFailure("assets/sections.xml does not exist")
            }
        }
        val sections = readSectionsXml(sectionFile)
        val sectionCollection = db.getCollection<Section>(RuntimeMongoCollections.sections)
        sectionCollection.insertMany(sections)

        // refresh collection subunit
        serverContext.subunits.collection.refreshCollection()
            .onFail { error("Error during call to CollectionSubunit.refreshCollection") }

        return CommandResult.Success("Inserted space and sections successfully")
    }

    private val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    private fun readSpacesXml(file: File): List<Space> {
        val document = builder.parse(InputSource(StringReader(file.readText())))
        val spaces = document.getElementsByTagName("space")
        val result = mutableListOf<Space>()

        for (i in 0 until spaces.length) {
            val space = spaces.item(i) as Element
            result.add(
                Space(
                    id = space.getAttribute("id"),
                    name = space.textContent,
                    order = space.getAttribute("order").toInt()
                )
            )
        }

        return result
    }

    private fun readSectionsXml(file: File): List<Section> {
        val document = builder.parse(InputSource(StringReader(file.readText())))
        val sections = document.getElementsByTagName("section")
        val result = mutableListOf<Section>()

        for (i in 0 until sections.length) {
            val space = sections.item(i) as Element
            result.add(
                Section(
                    id = space.getAttribute("id"),
                    spaceId = space.getAttribute("spaceId"),
                    name = space.getElementsByTagName("name").item(0).textContent,
                    description = space.getElementsByTagName("description").item(0).textContent,
                    order = space.getAttribute("order").toInt(),
                    topicCounts = 0,
                    latestPost = null
                )
            )
        }

        return result
    }
}
