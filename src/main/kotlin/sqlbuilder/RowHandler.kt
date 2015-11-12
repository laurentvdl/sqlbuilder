package sqlbuilder

import java.sql.SQLException

public interface RowHandler {
    @Throws(SQLException::class)
    public fun handle(set: ResultSet, row: Int): Boolean
}
