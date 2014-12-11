package sqlbuilder

import java.lang.reflect.ParameterizedType
import java.sql.SQLException

/**
 * ManualRowHandler derivative that helps paging left join resultsets.
 *
 * @author Laurent Van der Linden
 */
public abstract class JoiningPagedRowHandler<T>(private val offset: Int, private val rows: Int, private val table: String) : JoiningRowHandler<T>() {
    private var primaryCount = 0
    private var skip: Boolean = false
    private var lastKeyValues: List<Any?>? = null

    throws(javaClass<SQLException>())
    override fun handle(set: ResultSet, row: Int): Boolean {
        val parameterizedType = javaClass.getGenericSuperclass() as ParameterizedType
        val aType = parameterizedType.getActualTypeArguments()?.get(0)
        val keyValues = getKeyValues(set, aType as Class<T>, table)
        if (lastKeyValues == null || !(lastKeyValues?.equals(keyValues) ?: false)) {
            primaryCount++
            lastKeyValues = keyValues
        }
        skip = offset > primaryCount - 1
        if (!skip) handleInPage(set, row)
        return primaryCount <= offset + rows
    }

    override fun addPrimaryBean(instance: T) {
        if (skip) throw IllegalStateException("you should not add primary beans while skip is true")
        super.addPrimaryBean(instance)
    }

    override fun <S> mapSetToBean(set: ResultSet, table: String, instance: S, mappings: Map<String, String>?) : S {
        if (skip) throw IllegalStateException("you should not map resultsets while skip is true")
        return super.mapSetToBean(set, table, instance, mappings)
    }

    /**
     * Handle primary or full resultset depending on skip.
     * @param set query resultset
     * @param i resultSet index
     * @throws SQLException
     */
    throws(javaClass<SQLException>())
    public abstract fun handleInPage(set: ResultSet, i: Int)
}