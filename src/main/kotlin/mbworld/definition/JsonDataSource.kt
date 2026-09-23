package mbworld.definition

import encore.definition.FileDataSource
import java.io.File

/**
 * Data source for `.json` files.
 * - Load file located at [filePath] as-is.
 * - [readText] simply reads the file as UTF-8 string.
 */
class JsonDataSource(private val filePath: String) : FileDataSource {
    override val path: String = filePath

    override fun readText(): String {
        val file = File(filePath)
        if (!file.exists()) {
            error("File doesn't exist: '$filePath'")
        }
        return file.readText()
    }

    override fun readBytes(): ByteArray {
        error("Use readText() :)")
    }
}
