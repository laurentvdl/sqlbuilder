package sqlbuilder

import java.util.HashMap

/**
 * Wrapper for JDBC rows mapped to both a named and an indexed map.
 *
 * @author Laurent Van der Linden
 */
public class RowMap() {
    private val named = HashMap<String, Any?>()
    private val indexed = HashMap<Int, Any?>()

    public fun get(key: String?): Any? {
        if (key == null) throw NullPointerException("key cannot be null")
        return named.get(key.toLowerCase())
    }

    public fun get(index: Int): Any? {
        return indexed.get(index)
    }

    public fun put(index: Int, value: Any?): RowMap {
        indexed.put(index, value)
        return this
    }

    public fun put(name: String?, value: Any?): RowMap {
        if (name == null) throw NullPointerException("name cannot be null")
        named.put(name.toLowerCase(), value)
        return this
    }

    public fun getNamedMap(): Map<String, Any?> {
        return named
    }

    public fun getIndexedMap(): Map<Int, Any?> {
        return indexed
    }

    public fun size(): Int {
        return named.size()
    }


    override fun toString(): String {
        return named.toString()
    }
}
