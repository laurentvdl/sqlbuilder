package sqlbuilder.impl.mappers

import sqlbuilder.mapping.*
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
public class BigDecimalMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): BigDecimal? {
        val value = params.resultSet.getBigDecimal(params.index)
        if (params.resultSet.wasNull()) {
            return null
        } else {
            return value
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