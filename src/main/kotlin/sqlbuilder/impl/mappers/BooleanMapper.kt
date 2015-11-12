package sqlbuilder.impl.mappers

import sqlbuilder.mapping.*
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
public class BooleanMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Boolean? {
        val value = params.resultSet.getBoolean(params.index)
        if (params.resultSet.wasNull()) {
            return null
        } else {
            return value
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
        return Boolean::class.java == targetType
    }
}