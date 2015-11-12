package sqlbuilder.impl.mappers

import sqlbuilder.mapping.*
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
class StringMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): String? {
        val string = params.resultSet.getString(params.index)
        if (string != null) return string.trim()
        return null

    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value == null) {
            params.preparedStatement.setNull(params.index, Types.VARCHAR)
        } else {
            params.preparedStatement.setString(params.index, params.value as String)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return String::class.java == targetType
    }
}