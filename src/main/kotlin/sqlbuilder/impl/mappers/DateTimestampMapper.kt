package sqlbuilder.impl.mappers

import sqlbuilder.mapping.ToObjectMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import sqlbuilder.mapping.ToSQLMapper
import sqlbuilder.mapping.ToSQLMappingParameters
import java.sql.SQLException
import java.sql.Timestamp
import java.sql.Types
import java.util.Date

/**
 * Map java.util.Date to TIMESTAMP in the database
 */
class DateTimestampMapper : ToObjectMapper, ToSQLMapper {
    @Throws(SQLException::class)
    override fun toObject(params: ToObjectMappingParameters): Any? {
        return params.resultSet.getTimestamp(params.index)
    }

    override fun handles(targetType: Class<*>): Boolean {
        return Date::class.java == targetType
    }

    @Throws(SQLException::class)
    override fun toSQL(params: ToSQLMappingParameters) {
        val date = params.value as Date?
        if (date != null) {
            params.preparedStatement.setTimestamp(params.index, Timestamp(date.time))
        } else {
            params.preparedStatement.setNull(params.index, Types.TIMESTAMP)
        }
    }
}
