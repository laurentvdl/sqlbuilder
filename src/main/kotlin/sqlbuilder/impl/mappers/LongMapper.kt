package sqlbuilder.impl.mappers

import sqlbuilder.mapping.BiMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import sqlbuilder.mapping.ToSQLMappingParameters
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
class LongMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Long? {
        val longValue = params.resultSet.getLong(params.index)
        return if (params.resultSet.wasNull()) {
            null
        } else {
            longValue
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