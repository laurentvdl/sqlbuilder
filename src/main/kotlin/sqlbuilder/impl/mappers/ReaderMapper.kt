package sqlbuilder.impl.mappers

import sqlbuilder.mapping.BiMapper
import sqlbuilder.mapping.ToObjectMappingParameters
import sqlbuilder.mapping.ToSQLMappingParameters
import java.io.Reader
import java.sql.Types

/**
 * @author Laurent Van der Linden.
 */
class ReaderMapper : BiMapper {
    override fun toObject(params: ToObjectMappingParameters): Reader? {
        return params.resultSet.getCharacterStream(params.index)
    }

    override fun toSQL(params: ToSQLMappingParameters) {
        if (params.value != null) {
            params.preparedStatement.setCharacterStream(params.index, params.value as Reader)
        } else {
            params.preparedStatement.setNull(params.index, Types.CLOB)
        }
    }

    override fun handles(targetType: Class<*>): Boolean {
        return Reader::class.java == targetType
    }
}