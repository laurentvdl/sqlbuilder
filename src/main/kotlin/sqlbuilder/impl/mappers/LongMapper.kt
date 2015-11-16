package sqlbuilder.impl.mappers

import sqlbuilder.mapping.*
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
public class LongMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Long? {
        val longValue = params.resultSet.getLong(params.index)
        if (params.resultSet.wasNull()) {
            return null
        } else {
            return longValue
        }
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value != null) {
            params.preparedStatement.setLong(params.index, params.value as Long)
        } else {
            params.preparedStatement.setNull(params.index, Types.BIGINT)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return Long::class.java == targetType || java.lang.Long::class.java == targetType
    }
}