package sqlbuilder

import sqlbuilder.impl.mappers.AnyMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import java.io.Closeable
import java.sql.ResultSet
import java.sql.SQLException

class ResultSet(private val target: ResultSet, private val configuration: Configuration, val query: String) : Closeable {

    fun getJdbcResultSet(): java.sql.ResultSet {
        return target
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(SQLException::class) fun <T> getObject(targetType: Class<T>, index: Int): T? {
        val objectMapperForType = configuration.objectMapperForType(targetType) ?: AnyMapper()

        return objectMapperForType.toObject(ToObjectMappingParameters(index, target, targetType)) as T
    }

    @Throws(SQLException::class) fun next(): Boolean {
        return target.next()
    }

    @Throws(SQLException::class)
    override fun close() {
        target.close()
    }
}
