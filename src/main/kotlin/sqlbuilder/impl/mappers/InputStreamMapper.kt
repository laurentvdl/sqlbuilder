package sqlbuilder.impl.mappers

import sqlbuilder.mapping.*
import java.io.InputStream
import java.sql.Types

/**
 * Fallback Mapper to be used as a last resort.
 *
 * @author Laurent Van der Linden.
 */
class InputStreamMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): InputStream? {
        return params.resultSet.getBinaryStream(params.index)
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value != null) {
            params.preparedStatement.setBinaryStream(params.index, params.value as InputStream)
        } else {
            params.preparedStatement.setNull(params.index, Types.BLOB)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return InputStream::class.java == targetType
    }
}