package sqlbuilder.impl

import sqlbuilder.exclude
import sqlbuilder.Backend
import sqlbuilder.Select
import sqlbuilder.CacheStrategy
import sqlbuilder.RowHandler
import sqlbuilder.WhereGroup
import java.io.File
import sqlbuilder.ReflectionHandler
import sqlbuilder.DynamicBeanRowHandler
import sqlbuilder.ListRowHandler
import sqlbuilder.RowMap
import java.io.OutputStream
import java.io.Writer
import sqlbuilder.CachedRowHandler
import java.sql.SQLException
import sqlbuilder.Relation
import java.sql.ResultSet
import sqlbuilder.IncorrectResultSizeException
import sqlbuilder.PersistenceException
import sqlbuilder.BeanListRowHandler
import sqlbuilder.SingleFieldRowHandler
import sqlbuilder.FieldStreamHandler
import sqlbuilder.FieldWriterHandler
import java.util.ArrayList
import sqlbuilder.CacheableQuery
import sqlbuilder.SqlConverter
import sqlbuilder.include
import sqlbuilder.ReturningRowHandler
import sqlbuilder.SingleFieldListRowHandler
import sqlbuilder.meta.PropertyReference
import sqlbuilder.PropertiesHandler
import org.slf4j.LoggerFactory
import sqlbuilder.ReflectiveBeanListRowHandler

class SelectImpl(val backend: Backend) : Select {
    private val logger = LoggerFactory.getLogger(javaClass)

    private var entity: String? = null
    private var suffix: String? = null
    private var orderBy: String? = null
    private var orderAscending: Boolean = false
    private var selectOption: String? = null
    private val whereGroup = WhereGroup(this, this, Relation.AND)
    private var offset: Int? = null
    private var rows: Int? = null
    private var cacheStrategy: CacheStrategy? = null
    private var rowHandler: RowHandler? = null
    private var sql: String? = null
    private var parameters: Array<out Any>? = null
    private var includeFields: Array<out String>? = null
    private var excludeFields: Array<out String>? = null

    private var sqlMappings: MutableMap<Any, String>? = null
    private var propertyMappings: MutableMap<String, Any>? = null

    private var fetchSize: Int? = null
    private var groupBy: String? = null
    private var cursorType = ResultSet.TYPE_FORWARD_ONLY
    private var cursorConcurrency = ResultSet.CONCUR_READ_ONLY

    override fun from(entity: String): Select {
        this.entity = entity
        return this
    }

    override fun where(where: String): Select {
        whereGroup.and(where)
        return this
    }

    override fun where(where: String, vararg parameters: Any): Select {
        whereGroup.and(where, *parameters)
        return this
    }

    override fun where(): WhereGroup {
        return whereGroup
    }

    override fun suffix(suffix: String): Select {
        this.suffix = suffix
        return this
    }

    override fun orderBy(column: String, ascending: Boolean): Select {
        if (column.matches("^(\\w|_|\\.)+$")) {
            orderBy = column
            orderAscending = ascending
        } else {
            throw IllegalArgumentException("can't order by <" + column + ">")
        }
        return this
    }

    override fun offset(offset: Int, rows: Int): Select {
        this.offset = if (offset >= 0) offset else null
        this.rows = if (rows > 0) rows else null
        return this
    }

    override fun fetchSize(fetchSize: Int): Select {
        this.fetchSize = fetchSize
        return this
    }

    override fun cache(): Select {
        this.cacheStrategy = sqlbuilder.MemoryCache(backend, null)
        return this
    }

    override fun cache(cacheId: String): Select {
        this.cacheStrategy = sqlbuilder.MemoryCache(backend, cacheId)
        return this
    }

    override fun cache(file: File): Select {
        this.cacheStrategy = sqlbuilder.FileStrategy(file)
        return this
    }

    override fun cache(cacheStrategy: CacheStrategy): Select {
        this.cacheStrategy = cacheStrategy
        return this
    }

    override fun excludeFields(vararg excludes: String): Select {
        this.excludeFields = excludes
        return this
    }

    override fun includeFields(vararg includes: String): Select {
        this.includeFields = includes
        return this
    }

    override fun <T> selectBean(beanClass: Class<T>): T {
        val result = selectBeans(beanClass)
        if (!result.isEmpty()) {
            if (result.size() > 1) {
                throw IncorrectResultSizeException("more than 1 result")
            }
            return result.get(0)
        }
        return null
    }

    override fun <T> selectBeans(beanClass: Class<T>): List<T> {
        val metaResolver = backend.configuration.metaResolver

        if (entity == null) {
            entity = backend.configuration.escapeEntity(
                    metaResolver.getTableName(beanClass)
            )
        }

        var properties = metaResolver.getProperties(beanClass, true)

        var fields: List<String>? = null

        // don't check fields if using custom SQL
        if (sql == null) {
            if (entity == null) {
                throw PersistenceException("no entity set in statement nor declared as final static String TABLE")
            }

            properties = properties.include(includeFields)

            properties = properties.exclude(excludeFields)

            fields = properties.map { property ->
                val fieldName = property.name
                val mappedColumn = propertyMappings?.get(fieldName)
                if (mappedColumn == null || mappedColumn !is String) {
                    fieldName
                } else {
                    mappedColumn
                }
            }
        }

        val beanHandler = getRowHandler(beanClass)
        injectRowHandler(beanHandler, properties)

        execute(fields, beanHandler)

        if (beanHandler is ListRowHandler<*>) {
            [suppress("UNCHECKED_CAST")]
            return beanHandler.list as List<T>
        } else {
            throw IllegalStateException("A RowHandler of type <ListRowHandler> is required for selectBeans()")
        }
    }

    private fun <T> getRowHandler(beanClass: Class<T>): RowHandler {
        if (rowHandler != null) {
            return rowHandler!!
        } else {
            if (sql == null) {
                return ReflectiveBeanListRowHandler(beanClass)
            } else {
                return DynamicBeanRowHandler(beanClass)
            }
        }
    }

    override fun <T> selectField(soleField: String, requiredType: Class<T>): T {
        return selectField(soleField, requiredType, null as T)
    }

    override fun <T> selectField(soleField: String, requiredType: Class<T>, defaultValue: T): T {
        val handler = SingleFieldRowHandler(requiredType)
        execute(listOf(soleField), handler)
        val value = handler.result
        if (value == null) return defaultValue
        return value
    }

    SuppressWarnings("unchecked")
    override fun <T> selectAllField(soleField: String, requiredType: Class<T>): List<T> {
        val handler = SingleFieldListRowHandler(requiredType)
        execute(listOf(soleField), handler)
        return handler.result
    }

    SuppressWarnings("unchecked")
    override fun selectMap(vararg fields: String): RowMap? {
        val list = selectMaps(*fields)
        if (list.isNotEmpty()) {
            if (list.size() > 1) {
                throw IncorrectResultSizeException("more than 1 result")
            }
            return list.get(0)
        }
        return null
    }

    override fun selectMaps(vararg fields: String): List<RowMap> {
        val beanHandler = sqlbuilder.MapRowHandler()
        val list: List<String> = fields.toList()

        execute(list, beanHandler)

        return beanHandler.list
    }

    override fun selectFieldToStream(field: String, os: OutputStream) {
        execute(listOf(field), FieldStreamHandler(os))
    }

    override fun selectFieldToStream(field: String, writer: Writer) {
        execute(listOf(field), FieldWriterHandler(writer))
    }

    override fun <T> select(rowHandler: ReturningRowHandler<T>): T {
        injectRowHandler(rowHandler, null)
        execute(null, rowHandler)
        return rowHandler.result
    }

    [suppress("UNCHECKED_CAST")]
    public fun execute(fields: List<String>?,
                       rowHandler: RowHandler) {
        val sqlBuffer = StringBuilder(sql ?: "")
        if (sql == null) {
            sqlBuffer.append("select ")
            if (selectOption != null) sqlBuffer.append(selectOption).append(' ')
            if (fields != null && fields.isNotEmpty()) {
                fields.joinTo(sqlBuffer, ",")
                sqlBuffer.append(" ")
            }
            if (entity != null) sqlBuffer.append("from ").append(entity)
        }
        val whereParameters: MutableList<Any>
        if (this.parameters == null) {
            whereParameters = ArrayList<Any>()
        } else {
            whereParameters = parameters!!.toArrayList()
        }
        if (whereGroup.getNestedConditions().size() > 0) {
            sqlBuffer.append(" where ")
            whereGroup.toSql(sqlBuffer, whereParameters)
        }
        if (groupBy != null) sqlBuffer.append(" ").append(groupBy)
        if (orderBy != null) {
            sqlBuffer.append(" order by ").append(orderBy).append(if (orderAscending) " asc" else " desc")
        }
        if (suffix != null) sqlBuffer.append(" ").append(suffix)
        this.sql = sqlBuffer.toString()

        if (cacheStrategy != null && rowHandler is CachedRowHandler<*>) {
            val cachedResult = cacheStrategy!!.get(CacheableQuery(sql!!, whereParameters, offset, rows))
            if (cachedResult != null) {
                (rowHandler as CachedRowHandler<Any>).result = cachedResult
                return
            }
        }
        val con = backend.getSqlConnection()
        try {
            logger.info(sql)
            val ps = con.prepareStatement(sql, cursorType, cursorConcurrency)!!
            if (fetchSize != null) ps.setFetchSize(fetchSize!!)
            try {
                val parameterCount = ps.getParameterMetaData().getParameterCount()
                whereParameters.withIndex().forEach { pair ->
                    if (pair.index < parameterCount) {
                        SqlConverter.setParameter(ps, pair.value, pair.index + 1)
                    }
                }

                sqlbuilder.ResultSet(ps.executeQuery()).use { set ->
                    var row = 0
                    var continueCursorLoop = true
                    while (continueCursorLoop && set.next() && (offset == null || rows == null || rows!! + offset!! > row)) {
                        if (offset == null || (offset != null && offset!! <= row)) {
                            continueCursorLoop = rowHandler.handle(set, row)
                        }
                        row++
                    }
                }

                if (cacheStrategy != null && rowHandler is CachedRowHandler<*>) {
                    val value = (rowHandler as CachedRowHandler<Any>).result
                    cacheStrategy!!.put(CacheableQuery(sql!!, whereParameters, offset, rows), value)
                }
            } finally {
                ps.close()
            }
        } catch (e: SQLException) {
            throw PersistenceException("<$sql> failed with parameters $whereParameters", e)
        } finally {
            sql = null
            offset = null
            backend.closeConnection(con)
        }
    }

    private fun injectRowHandler(rowHandler: Any, properties: List<PropertyReference>?) {
        if (rowHandler is PropertiesHandler) {
            rowHandler.properties = properties!!
        }
        if (rowHandler is ReflectionHandler) {
            rowHandler.metaResolver = backend.configuration.metaResolver
        }
        if (rowHandler is DynamicBeanRowHandler<*>) {
            rowHandler.mappings = sqlMappings
        }
    }

    override fun sql(sql: String, vararg parameters: Any): Select {
        this.sql = sql
        this.parameters = parameters
        return this
    }

    override fun map(sqlColumnIndex: Int, propertyName: String): Select {
        return mapColumn(sqlColumnIndex, propertyName)
    }

    override fun map(sqlColumn: String, propertyName: String): Select {
        return mapColumn(sqlColumn.toLowerCase(), propertyName)
    }

    private fun mapColumn(sqlColumn: Any, propertyName: String): Select {
        if (this.sqlMappings == null) {
            this.sqlMappings = java.util.HashMap<Any, String>()
            this.propertyMappings = java.util.HashMap<String, Any>()
        }
        this.sqlMappings!!.put(sqlColumn, propertyName.toLowerCase())
        this.propertyMappings!!.put(propertyName.toLowerCase(), sqlColumn)
        return this
    }

    override fun selectOption(option: String): Select {
        selectOption = option
        return this
    }

    override fun resultSetType(`type`: Int, concurrency: Int): Select {
        this.cursorType = `type`
        this.cursorConcurrency = concurrency
        return this
    }

    override fun groupBy(groupBy: String): Select {
        this.groupBy = groupBy
        return this
    }
}