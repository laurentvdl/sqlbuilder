package sqlbuilder

import sqlbuilder.meta.PropertyReference

import java.lang.ref.SoftReference
import java.sql.Connection
import java.sql.SQLException

/**
 * @author Laurent Van der Linden
 */
public interface Backend {
    public fun getSqlConnection(): Connection

    public fun closeConnection(connection: Connection)

    public fun getCache(cacheId: String?): MutableMap<CacheableQuery, SoftReference<Any>>

    throws(SQLException::class)
    public fun checkNullability(entity: String, bean: Any, sqlCon: Connection, getters: List<PropertyReference>)

    public fun isInTransaction(): Boolean

    val configuration: Configuration
}
