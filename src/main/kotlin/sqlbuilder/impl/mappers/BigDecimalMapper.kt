package sqlbuilder.impl.mappers

import sqlbuilder.mapping.BiMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import sqlbuilder.mapping.ToSQLMappingParameters
import java.math.BigDecimal
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
class BigDecimalMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): BigDecimal? {
        val value = params.resultSet.getBigDecimal(params.index)
        return if (params.resultSet.wasNull()) {
            null
        } else {
            value
        }
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value != null) {
            params.preparedStatement.setBigDecimal(params.index, params.value as BigDecimal)
        } else {
            params.preparedStatement.setNull(params.index, Types.DECIMAL)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return BigDecimal::class.java == targetType
    }
}