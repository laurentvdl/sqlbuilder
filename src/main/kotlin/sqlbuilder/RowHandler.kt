package sqlbuilder

import java.sql.SQLException

public interface RowHandler {
    throws(SQLException::class)
    public fun handle(set: ResultSet, row: Int): Boolean
}
