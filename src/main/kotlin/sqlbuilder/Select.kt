package sqlbuilder

import org.intellij.lang.annotations.Language
import java.io.File
import java.io.OutputStream
import java.io.Writer

/**
 * Select statement: select bean/beans, single fields or Maps.
 *
 * @author Laurent Van der Linden
 */
interface Select {
    /**
     * if your bean does not have a static entity reference like<br/>
     * <code>final static String TABLE = "schema.table";</code><br/>
     * , specify the schema.table here
     * @param entity
     * @return
     */
    fun from(entity: String): Select

    /**
     * A simple where clause
     * @param where
     * @return this
     */
    fun where(where: String): Select

    /**
     * A simple where clause
     * @param where
     * @return this
     */
    fun where(where: String, vararg parameters: Any?): Select

    /**
     * Start building a complex where clause (nested groups, conditional clauses).
     * @return the root where group
     */
    fun where(): WhereGroup

    /**
     * Custom suffix: eg. order/group by
     * @param suffix
     * @return
     */
    fun suffix(suffix: String): Select

    /**
     * Order by 1 column. This will replace any existing suffix.
     * @param column
     * @param ascending
     */
    fun orderBy(column: String?, ascending: Boolean): Select

    /**
     * For partial lists: row 20 -> 40
     * @param offset
     * @param rows
     * @return this
     */
    fun offset(offset: Int, rows: Int): Select

    /**
     * Change the default fetch size.
     * Mysql by default fetches everything unless fetchsize = Integer.MIN_VALUE.
     * @param fetchSize
     * @return this
     */
    fun fetchSize(fetchSize: Int): Select

    /**
     * Cache the results in the global cache.
     * @return this
     */
    fun cache(): Select

    /**
     * Cache the results in a specific cache.
     * @return this
     */
    fun cache(cacheId: String): Select

    /**
     * Cache the result in a File using serialisation.
     * @param file
     * @return this
     */
    fun cache(file: File): Select

    /**
     * Select data using a custom caching strategy.
     * @param cacheStrategy
     * @return this
     */
    fun cache(cacheStrategy: CacheStrategy): Select

    fun <T : Any> selectBean(beanClass: Class<T>): T?

    fun <T : Any> selectBeans(beanClass: Class<T>): List<T>

    fun excludeFields(vararg excludes: String): Select

    fun includeFields(vararg includes: String): Select

    /**
     * Select first/only occurrence of a single field and return null if no result.
     * @return result or null
     */
    fun <T : Any> selectField(requiredType: Class<T>): T?

    /**
     * Select first/only occurrence of a single field and return null if no result.
     * @return result or null
     */
    fun <T : Any> selectField(soleField: String?, requiredType: Class<T>): T?

    /**
     * Select first/only occurrence of a single field and return default value if no result.
     * @return result or defaultValue
     */
    fun <T : Any> selectField(soleField: String?, requiredType: Class<T>, defaultValue: T): T

    /**
     * Select all occurrences of a single field.
     * @param requiredType
     * @return
     */
    fun <T : Any> selectAllField(requiredType: Class<T>): List<T>

    /**
     * Select all occurrences of a single field.
     * @param soleField
     * @param requiredType
     * @return
     */
    fun <T : Any> selectAllField(soleField: String?, requiredType: Class<T>): List<T>

    /**
     * Select the result as a single Map.
     * <br/>Use either the column name or index(from 0) as key.
     * @param fields selected fields
     */
    fun selectMap(vararg fields: String): RowMap?

    /**
     * Select the result as a List of RowMaps.
     * <br/>Use this parameterized version in combination with a from and where.
     * @param fields selected fields
     */
    fun selectMaps(vararg fields: String): List<RowMap>

    /**
     * Select a single field (1/first row) and pump the result into the OutputStream.
     * @param field
     * @param os
     */
    fun selectFieldToStream(field: String, os: OutputStream)

    /**
     * Select a single CLOB field and stream it to a Writer.
     * @param field
     * @param writer
     */
    fun selectFieldToStream(field: String, writer: Writer)

    /**
     * Run select statement with custom ReturningRowHandler returning results of type R which cannot be null.
     */
    fun <T : Any> select(rowHandler: ReturningRowHandler<T>): T

    /**
     * Run select statement with custom ReturningRowHandler returning results of type R which can be null.
     */
    fun <T : Any> select(rowHandler: OptionalReturningRowHandler<T>): T?

    fun sql(@Language("SQL") sql: String, vararg parameters: Any): Select

    /**
     * Map a SQL expression to a Bean Property: eg. sum(amount) -> totalAmount.
     * @param sqlColumn column index in result (starting from 0)
     * @param propertyName a Bean Property name (without setter/getter prefix)
     * @return the current Select statement
     */
    fun map(sqlColumnIndex: Int, propertyName: String): Select

    /**
     * Map a SQL expression to a Bean Property: eg. sum(amount) -> totalAmount.
     * @param sqlColumn column name (alias if applied)
     * @param propertyName a Bean Property name (without setter/getter prefix)
     * @return the current Select statement
     */
    fun map(sqlColumn: String, propertyName: String): Select

    /**
     * Add a vendor-specific command to the statement at "select <i>option</i> ..."
     * @param option eg. "SQL_CALC_FOUND_ROWS"
     * @return the current Select statement
     */
    fun selectOption(option: String): Select

    /**
     * Override the cursor direction or concurrency to allow eg. updatable resultsets
     * @param cursorType one of
     *        <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *        <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *        <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @param concurrency one of
     *        <code>ResultSet.CONCUR_READ_ONLY</code> or
     *        <code>ResultSet.CONCUR_UPDATABLE</code>
     * @return the current Select statement
     */
    fun resultSetType(cursorType: Int, concurrency: Int): Select

    /**
     * Specifiy group by statement, eg. <code>select.groupBy("group by aardvz")</code>
     * <br/>Insert after where clause and before orderBy/suffix
     * @param groupBy complete group statement (including group by keyword)
     * @return the current Select statement
     */
    fun groupBy(groupBy: String): Select
}
