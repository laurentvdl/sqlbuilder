package sqlbuilder

import java.sql.SQLException

interface RowHandler {
    @Throws(SQLException::class) fun handle(set: ResultSet, row: Int): Boolean
}
