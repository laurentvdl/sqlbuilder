package sqlbuilder

import sqlbuilder.exceptions.CacheException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * Caching strategy that use Serialisation to store any query to a single file.
 * This means the user must specify a different file for every query and parameter combination.
 */
class FileStrategy(private val file: File) : CacheStrategy {
    override fun get(query: CacheableQuery): Any? {
        if (file.isFile && file.length() > 0) {
            try {
                return FileInputStream(file).use { fis ->
                    ObjectInputStream(fis).readObject()
                }
            } catch (e: Exception) {
                try {
                    file.delete()
                } finally {
                    throw CacheException("failed to read cached value from file ${file.absolutePath}", e)
                }
            }
        }
        return null
    }

    override fun put(query: CacheableQuery, result: Any?) {
        if (result != null) {
            if (result is Serializable) {
                try {
                    ObjectOutputStream(FileOutputStream(file)).use { oos ->
                        oos.writeObject(result)
                    }
                } catch (e: Exception) {
                    try {
                        file.delete()
                    } finally {
                        throw CacheException("failed to cache value to file ${file.absolutePath}", e)
                    }
                } finally {
                }
            } else {
                throw CacheException("when using a FileStrategy cache, all query results must be java.io.Serializable")
            }
        }
    }
}
