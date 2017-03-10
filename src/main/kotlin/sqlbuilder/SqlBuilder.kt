package sqlbuilder

/**
 * Primary interface to be used as a stateless prototype.
 *
 * @author Laurent Van der Linden
 */
interface SqlBuilder {
    fun select(): Select
    fun insert(): Insert
    fun delete(): Delete
    fun update(): Update

    /**
     * Insert or Update a bean by looking for an ID field.
     * After an insert, the autogenerated id is set on the result
     * @param bean DTO with an Integer or Long 'id' field
     * @param <T> type of the bean
     * @return the same bean with the id set
     */
    fun <T : Any> save(bean: T, vararg excludedFields: String): T
    /**
     * Start a transaction with default isolation.
     */
    fun startTransaction()
    /**
     * Start a transaction using an isolationlevel other than TRANSACTION_READ_COMMITTED
     * @param isolationLevel Connection.TRANSACTION_...
     */
    fun startTransaction(isolationLevel: Int, readonly: Boolean)
    fun commitTransaction()
    /**
     * This must be called in the a finally clause whenever you have started a transaction.
     * <br/>If commitTransaction has already been called, it will only cleanup resources,
     * else it will rollback the transaction.
     */
    fun endTransaction()
    fun purgeCache(cacheId: String)
    fun purgeAllCaches()

    var configuration: Configuration
}
