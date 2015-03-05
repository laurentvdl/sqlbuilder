package sqlbuilder

import java.sql.SQLException

/**
 * A ReturningRowHandler that exports its result as a List
 *
 * @author Laurent Van der Linden
 */
public trait ListRowHandler<T> : ReturningRowHandler<MutableList<T>> {
    val list: MutableList<T>
}
