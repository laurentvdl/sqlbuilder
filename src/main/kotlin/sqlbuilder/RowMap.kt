package sqlbuilder

import java.io.Serializable
import java.util.HashMap

/**
 * Wrapper for JDBC rows mapped to both a named and an indexed map.
 *
 * @author Laurent Van der Linden
 */
class RowMap : Serializable {
    private val named = HashMap<String, Any?>()
    private val indexed = HashMap<Int, Any?>()

    fun get(key: String?): Any? {
        if (key == null) throw NullPointerException("key cannot be null")
        return named[key.toLowerCase()]
    }

    fun get(index: Int): Any? {
        return indexed[index]
    }

    fun put(index: Int, value: Any?): RowMap {
        indexed.put(index, value)
        return this
    }

    fun put(name: String?, value: Any?): RowMap {
        if (name == null) throw NullPointerException("name cannot be null")
        named.put(name.toLowerCase(), value)
        return this
    }

    fun getNamedMap(): Map<String, Any?> {
        return named
    }

    fun getIndexedMap(): Map<Int, Any?> {
        return indexed
    }

    fun size(): Int {
        return named.size
    }


    override fun toString(): String {
        return named.toString()
    }
}
