package sqlbuilder.impl.mappers

import sqlbuilder.mapping.*
import java.sql.Timestamp
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
public class TimestampMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Timestamp? {
        val value = params.resultSet.getTimestamp(params.index)
        if (params.resultSet.wasNull()) {
            return null
        } else {
            return value
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