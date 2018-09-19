package sqlbuilder.impl.mappers

import sqlbuilder.mapping.BiMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import sqlbuilder.mapping.ToSQLMappingParameters
import java.sql.Date
import java.sql.Types
import java.time.LocalDate

class LocalDateMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Any? {
        val date = params.resultSet.getDate(params.index)
        return date?.toLocalDate()
    }

    override fun handles(targetType: Class<*>): Boolean {
        return LocalDate::class.java == targetType
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        val dateTime = params.value as LocalDate?
        if (dateTime != null) {
            params.preparedStatement.setDate(params.index, Date.valueOf(dateTime))
        } else {
            params.preparedStatement.setNull(params.index, Types.DATE)
        }
    }
}