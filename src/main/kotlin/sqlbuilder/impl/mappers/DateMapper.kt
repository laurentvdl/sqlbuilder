package sqlbuilder.impl.mappers

import sqlbuilder.mapping.BiMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import sqlbuilder.mapping.ToSQLMappingParameters
import java.sql.Types
import java.util.Date

/**
 * @author Laurent Van der Linden.
 */
class DateMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Date? {
        val value = params.resultSet.getDate(params.index)
        if (params.resultSet.wasNull()) {
            return null
        } else {
            return value
        }
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value != null) {
            if (params.value is java.sql.Date) {
                params.preparedStatement.setDate(params.index, params.value)
            } else {
                params.preparedStatement.setDate(params.index, java.sql.Date((params.value as Date).time))
            }
        } else {
            params.preparedStatement.setNull(params.index, Types.TINYINT)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return Date::class.java == targetType
    }
}