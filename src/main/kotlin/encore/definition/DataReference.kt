package encore.definition

import encore.fancam.Fancam
import encore.fancam.Tags
import encore.time.TimeCenter
import encore.utils.support.className
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds

/**
 * A global registry of data definitions.
 *
 * Each [DataDefinition] encapsulates rules, policies, or static data
 * describing some aspect of the platform.
 *
 * This class act as a registry and provides a lookup for particular data definition.
 * - Registration of definitions via [initialize].
 * - Lookup of definitions via [get].
 *
 * Typical usage:
 * ```
 * DataReference.initialize {
 *     add(XmlRes("boss.xml"), XmlLoader())
 *     add(JsonRes("items.json"), JsonLoader())
 * }
 *
 * val bossHp = DataReference.get<BossConfig>().getBossHpFor(bossId)
 * ```
 */
object DataReference {
    private var initializeState = 0

    /**
     * Holds all registered data definitions.
     *
     * Access should generally go through [get]. Direct modification of
     * this map is not intended. Definitions should be registered through [initialize].
     */
    val registry = mutableMapOf<KClass<out Any>, Any>()
        get() = if (initializeState == 0) {
            error("DataReference is not initialized. Call initialize() first.")
        } else {
            field
        }

    /**
     * Initializes the registry by loading definitions from
     * the provided [FileDataSource] and [FileDataLoader] pairs
     * via the DSL [block].
     *
     * Subsequent calls after initialization are ignored with a warning.
     *
     * @param block A DSL context to register sources and loaders.
     */
    fun initialize(block: InitContext.() -> Unit) {
        if (initializeState == 1 || initializeState == 2) {
            Fancam.warn(Tags.Reference) { "DataReference.initialize() called during or after initialization. Ignoring." }
            return
        }
        initializeState = 1

        val start1 = TimeCenter.now()
        Fancam.info(Tags.Reference) { "Initializing DataReference..." }

        val ctx = InitContext()
        ctx.block()
        ctx.entries.forEach { (source, loader) ->
            val start2 = TimeCenter.now()
            val definitions = loader.produce(source)
            val finish2 = (TimeCenter.now() - start2).milliseconds
            Fancam.trace(Tags.Reference) {
                "Loaded '${source.className()}' by '${loader.className()}' in ${finish2}, produced ${definitions.size} definition entries."
            }

            definitions.forEach { registry[it::class] = it }
        }

        Fancam.info(Tags.Reference) { "DataReference initialized in ${(TimeCenter.now() - start1).milliseconds}" }
        initializeState = 2
    }

    /**
     * Retrieves a registered [DataDefinition] of type [T].
     *
     * @throws IllegalArgumentException if the requested definition is not registered.
     */
    inline fun <reified T : Any> get(): T {
        return registry[T::class] as? T
            ?: throw IllegalArgumentException(
                "DataDefintion <${T::class.simpleName}> is not registered. " +
                        "Call DataReference.initialize() and provide the definition first."
            )
    }
}

/**
 * DSL context for registering [FileDataSource] and [FileDataLoader] pairs
 * during [DataReference.initialize].
 */
class InitContext {
    internal val entries = mutableListOf<Pair<FileDataSource, FileDataLoader>>()

    /**
     * Registers a [source] with its corresponding [loader].
     *
     * @param source The raw file data source.
     * @param loader The loader responsible for converting the source into [DataDefinition]s.
     */
    fun add(source: FileDataSource, loader: FileDataLoader) {
        entries += source to loader
    }
}
