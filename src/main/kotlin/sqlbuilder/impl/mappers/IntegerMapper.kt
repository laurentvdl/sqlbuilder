package sqlbuilder.impl.mappers

import sqlbuilder.mapping.*
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
public class IntegerMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Int? {
        val value = params.resultSet.getInt(params.index)
        if (params.resultSet.wasNull()) {
            return null
        } else {
            return value
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