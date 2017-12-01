package sqlbuilder.impl.mappers

import sqlbuilder.mapping.BiMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import sqlbuilder.mapping.ToSQLMappingParameters
import java.sql.Timestamp
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
class TimestampMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Timestamp? {
        val value = params.resultSet.getTimestamp(params.index)
        return if (params.resultSet.wasNull()) {
            null
        } else {
            value
        }
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value != null) {
            params.preparedStatement.setTimestamp(params.index, params.value as Timestamp)
        } else {
            params.preparedStatement.setNull(params.index, Types.TIMESTAMP)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return Timestamp::class.java == targetType
    }
}