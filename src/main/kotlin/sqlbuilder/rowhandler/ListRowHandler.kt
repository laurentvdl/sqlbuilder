package sqlbuilder.rowhandler

import sqlbuilder.ReturningRowHandler

/**
 * A ReturningRowHandler that exports its result as a List

 * @author Laurent Van der Linden
 */
interface ListRowHandler<T> : ReturningRowHandler<MutableList<T>>
