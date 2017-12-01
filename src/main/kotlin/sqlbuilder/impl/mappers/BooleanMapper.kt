package sqlbuilder.impl.mappers

import sqlbuilder.mapping.BiMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import sqlbuilder.mapping.ToSQLMappingParameters
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
class BooleanMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Boolean? {
        val value = params.resultSet.getBoolean(params.index)
        return if (params.resultSet.wasNull()) {
            null
        } else {
            value
        }
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value != null) {
            params.preparedStatement.setBoolean(params.index, params.value as Boolean)
        } else {
            params.preparedStatement.setNull(params.index, Types.TINYINT)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return Boolean::class.java == targetType || java.lang.Boolean::class.java == targetType
    }
}