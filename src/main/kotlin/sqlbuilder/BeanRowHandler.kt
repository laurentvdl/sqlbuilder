package sqlbuilder

/**
 * A handler type that handles an already mapped bean.
 *
 * @author Laurent Van der Linden
 */
public trait BeanRowHandler<T> : RowHandler, ReflectionHandler {
    /**
     * Do something with this bean that was mapped to a single row.
     * @param bean
     */
    public fun handle(bean: T, set: ResultSet)
}
