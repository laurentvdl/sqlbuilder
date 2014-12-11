package sqlbuilder

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import org.slf4j.LoggerFactory

/**
 * Caching strategy that use Serialisation to store any query to a single file.
 * This means the user must specify a different file for every dataset.
 *
 * @author Laurent Van der Linden
 */
public class FileStrategy(private var file: File) : CacheStrategy {
    private val logger = LoggerFactory.getLogger(javaClass)!!

    override fun get(query: CacheableQuery): Any? {

        if (file.isFile()) {
            try {
                return FileInputStream(file).use { fis ->
                    createObjectInputStream(fis).readObject()
                }
            } catch (e: Exception) {
                logger.warn("failed to read cached value from file ${file.getAbsolutePath()}", e)
                file.delete()
            }

        }
        return null
    }

    override fun put(query: CacheableQuery, result: Any) {
        try {
            val oos = ObjectOutputStream(FileOutputStream(file))
            try {
                oos.writeObject(result)
            } finally {
                oos.close()
            }
        } catch (e: Exception) {
            logger.warn("failed to cache value to file", e)
        }

    }

    protected fun createObjectInputStream(fis: FileInputStream): ObjectInputStream {
        return ObjectInputStream(fis)
    }
}
