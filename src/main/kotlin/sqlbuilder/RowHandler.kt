package sqlbuilder

import java.sql.SQLException

public trait RowHandler {
    throws(javaClass<SQLException>())
    public fun handle(set: ResultSet, row: Int): Boolean
}
