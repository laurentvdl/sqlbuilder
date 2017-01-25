package sqlbuilder

import sqlbuilder.meta.MetaResolver
import sqlbuilder.meta.PropertyReference

import java.lang.ref.SoftReference
import java.sql.Connection
import java.sql.SQLException

interface Backend {
    fun getSqlConnection(): Connection

    fun closeConnection(connection: Connection)

    fun getCache(cacheId: String?): MutableMap<CacheableQuery, SoftReference<Any>>

    @Throws(SQLException::class) fun checkNullability(entity: String, bean: Any, sqlCon: Connection, getters: List<PropertyReference>)

    fun isInTransaction(): Boolean

    val configuration: Configuration

    val metaResolver: MetaResolver
}
