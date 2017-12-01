package sqlbuilder.impl.mappers

import sqlbuilder.mapping.BiMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import sqlbuilder.mapping.ToSQLMappingParameters
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
class IntegerMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Int? {
        val value = params.resultSet.getInt(params.index)
        return if (params.resultSet.wasNull()) {
            null
        } else {
            value
        }
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value != null) {
            params.preparedStatement.setInt(params.index, params.value as Int)
        } else {
            params.preparedStatement.setNull(params.index, Types.BIGINT)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return Int::class.java == targetType || java.lang.Integer::class.java == targetType
    }
}