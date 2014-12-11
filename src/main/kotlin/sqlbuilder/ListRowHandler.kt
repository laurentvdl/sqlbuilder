package sqlbuilder

/**
 * A ReturningRowHandler that exports its result as a List
 *
 * @author Laurent Van der Linden
 */
public trait ListRowHandler<T> : ReturningRowHandler<MutableList<T>> {
    public val list: MutableList<T>
}
