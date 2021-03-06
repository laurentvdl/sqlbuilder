package sqlbuilder.impl.mappers

import sqlbuilder.mapping.BiMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import sqlbuilder.mapping.ToSQLMappingParameters
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
class DoubleMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Double? {
        val value = params.resultSet.getDouble(params.index)
        return if (params.resultSet.wasNull()) {
            null
        } else {
            value
        }
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value != null) {
            params.preparedStatement.setDouble(params.index, params.value as Double)
        } else {
            params.preparedStatement.setNull(params.index, Types.DECIMAL)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return targetType == Double::class.java || targetType == java.lang.Double::class.java
    }
}