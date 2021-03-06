package sqlbuilder

/**
 * RowHandler that returns a R (likely collection of sorts) after a select operation.
 *
 * @author Laurent Van der Linden
 */
interface ReturningRowHandler<out R> : RowHandler {
    val result: R
}