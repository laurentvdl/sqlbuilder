package sqlbuilder.impl.mappers

import sqlbuilder.mapping.BiMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import sqlbuilder.mapping.ToSQLMappingParameters
import java.sql.Timestamp
import java.sql.Types
import java.time.LocalDateTime

class LocalDateTimeMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Any? {
        val timestamp = params.resultSet.getTimestamp(params.index)
        return timestamp?.toLocalDateTime()

    }

    override fun handles(targetType: Class<*>): Boolean {
        return LocalDateTime::class.java == targetType
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        val dateTime = params.value as LocalDateTime?
        if (dateTime != null) {
            params.preparedStatement.setTimestamp(params.index, Timestamp.valueOf(dateTime))
        } else {
            params.preparedStatement.setNull(params.index, Types.TIMESTAMP)
        }
    }
}