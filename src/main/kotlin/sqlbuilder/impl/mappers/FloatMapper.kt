package sqlbuilder.impl.mappers

import sqlbuilder.mapping.*
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
public class FloatMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Float? {
        val value = params.resultSet.getFloat(params.index)
        if (params.resultSet.wasNull()) {
            return null
        } else {
            return value
        }
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value != null) {
            params.preparedStatement.setFloat(params.index, params.value as Float)
        } else {
            params.preparedStatement.setNull(params.index, Types.DECIMAL)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return targetType == Double::class.java
    }
}