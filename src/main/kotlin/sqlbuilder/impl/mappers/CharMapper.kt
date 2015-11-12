package sqlbuilder.impl.mappers

import sqlbuilder.mapping.*
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
class CharMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Char? {
        val string = params.resultSet.getString(params.index)
        if (string != null && string.length > 0) return string[0]
        return null

    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value == null) {
            params.preparedStatement.setNull(params.index, Types.VARCHAR)
        } else {
            params.preparedStatement.setString(params.index, params.value.toString())
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return targetType == Char::class.java
    }
}