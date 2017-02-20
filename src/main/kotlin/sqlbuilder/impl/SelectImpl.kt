package sqlbuilder.impl

import org.slf4j.LoggerFactory
import sqlbuilder.Backend
import sqlbuilder.CacheStrategy
import sqlbuilder.CacheableQuery
import sqlbuilder.IncorrectResultSizeException
import sqlbuilder.OptionalReturningRowHandler
import sqlbuilder.PersistenceException
import sqlbuilder.Relation
import sqlbuilder.ReturningRowHandler
import sqlbuilder.RowHandler
import sqlbuilder.RowMap
import sqlbuilder.Select
import sqlbuilder.SqlConverter
import sqlbuilder.WhereGroup
import sqlbuilder.exclude
import sqlbuilder.include
import sqlbuilder.meta.PropertyReference
import sqlbuilder.result
import sqlbuilder.rowhandler.CachedRowHandler
import sqlbuilder.rowhandler.DynamicBeanRowHandler
import sqlbuilder.rowhandler.ExpandingRowHandler
import sqlbuilder.rowhandler.FieldStreamHandler
import sqlbuilder.rowhandler.FieldWriterHandler
import sqlbuilder.rowhandler.ListRowHandler
import sqlbuilder.rowhandler.MapRowHandler
import sqlbuilder.rowhandler.PropertiesHandler
import sqlbuilder.rowhandler.ReflectionHandler
import sqlbuilder.rowhandler.ReflectiveBeanListRowHandler
import sqlbuilder.rowhandler.SingleFieldListRowHandler
import sqlbuilder.rowhandler.SingleFieldRowHandler
import java.io.File
import java.io.OutputStream
import java.io.Writer
import java.sql.ResultSet
import java.sql.SQLException
import java.util.ArrayList

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

    private val columnPattern = "^(\\w|_|\\.)+$".toRegex()

    private val metaResolver = backend.metaResolver

    override fun from(entity: String): Select {
        this.entity = entity
        return this
    }

    override fun where(where: String): Select {
        whereGroup.and(where)
        return this
    }

    override fun where(where: String, vararg parameters: Any?): Select {
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

    override fun orderBy(column: String?, ascending: Boolean): Select {
        if (column != null) {
            if (column.matches(columnPattern)) {
                orderBy = column
                orderAscending = ascending
            } else {
                throw IllegalArgumentException("can't order by <$column>")
            }
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

    override fun <T : Any> selectBean(beanClass: Class<T>): T? {
        val result = selectBeans(beanClass)
        if (!result.isEmpty()) {
            if (result.size > 1) {
                throw IncorrectResultSizeException("more than 1 result")
            }
            return result[0]
        }
        return null
    }

    override fun <T : Any> selectBeans(beanClass: Class<T>): List<T> {
        if (entity == null) {
            entity = backend.configuration.escapeEntity(
                    metaResolver.getTableName(beanClass)
            )
        }

        var properties = metaResolver.getProperties(beanClass, true)

        var fields: List<String>? = null

        // don't check fields if using custom SQL
        if (sql == null && selectOption == null) {
            if (entity == null) {
                throw PersistenceException("no entity set in statement nor declared as final static String TABLE")
            }

            properties = properties.include(includeFields)

            properties = properties.exclude(excludeFields)

            fields = properties.map { property ->
                val mappedColumn = propertyMappings?.get(property.name)
                if (mappedColumn == null || mappedColumn !is String) {
                    property.columnName
                } else {
                    mappedColumn
                }
            }
        }

        var beanHandler = getRowHandler(beanClass)
        injectRowHandler(beanHandler, properties)

        beanHandler = execute(fields, beanHandler)

        if (beanHandler is ListRowHandler<*> || beanHandler is CachedRowHandler<*>) {
            @Suppress("UNCHECKED_CAST")
            return beanHandler.result as List<T>
        } else {
            throw IllegalStateException("A RowHandler of type <ListRowHandler> is required for selectBeans()")
        }
    }

    private fun <T : Any> getRowHandler(beanClass: Class<T>): RowHandler {
        if (sql == null && selectOption == null) {
            return ReflectiveBeanListRowHandler(beanClass)
        } else {
            return DynamicBeanRowHandler(beanClass)
        }
    }

    override fun <T : Any> selectField(soleField: String?, requiredType: Class<T>): T? {
        var handler: RowHandler = SingleFieldRowHandler(requiredType)
        handler = execute(if (soleField == null) null else listOf(soleField), handler)
        @Suppress("UNCHECKED_CAST")
        return handler.result as T?
    }

    override fun <T : Any> selectField(soleField: String?, requiredType: Class<T>, defaultValue: T): T {
        var handler: RowHandler = SingleFieldRowHandler(requiredType)
        handler = execute(if (soleField == null) null else listOf(soleField), handler)
        @Suppress("UNCHECKED_CAST")
        return handler.result as T? ?: defaultValue
    }

    override fun <T : Any> selectAllField(soleField: String?, requiredType: Class<T>): List<T> {
        var handler: RowHandler = SingleFieldListRowHandler(requiredType)
        handler = execute(if (soleField == null) null else listOf(soleField), handler)
        @Suppress("UNCHECKED_CAST")
        return handler.result as List<T>
    }

    @SuppressWarnings("unchecked")
    override fun selectMap(vararg fields: String): RowMap? {
        val list = selectMaps(*fields)
        if (list.isNotEmpty()) {
            if (list.size > 1) {
                throw IncorrectResultSizeException("more than 1 result")
            }
            return list[0]
        }
        return null
    }

    override fun selectMaps(vararg fields: String): List<RowMap> {
        val beanHandler = MapRowHandler()
        val list: List<String> = fields.toList()

        @Suppress("UNCHECKED_CAST")
        return execute(list, beanHandler).result as List<RowMap>
    }

    override fun selectFieldToStream(field: String, os: OutputStream) {
        execute(listOf(field), FieldStreamHandler(os))
    }

    override fun selectFieldToStream(field: String, writer: Writer) {
        execute(listOf(field), FieldWriterHandler(writer))
    }

    override fun <T : Any> select(rowHandler: ReturningRowHandler<T>): T {
        injectRowHandler(rowHandler, null)
        return from(execute(null, rowHandler))!!
    }

    override fun <T : Any> select(rowHandler: OptionalReturningRowHandler<T>): T? {
        injectRowHandler(rowHandler, null)
        return from(execute(null, rowHandler))
    }

    fun execute(fields: List<String>?, rowHandler: RowHandler): RowHandler {
        val (sql,whereParameters) = prepareSql(this.sql, fields, rowHandler)

        if (cacheStrategy != null) {
            if (rowHandler is ReturningRowHandler<*> || rowHandler is OptionalReturningRowHandler<*>) {
                val cachedResult = cacheStrategy!!.get(CacheableQuery(sql, whereParameters, offset, rows))
                if (cachedResult != null) {
                    return CachedRowHandler(cachedResult)
                }
            }
        }
        val con = backend.getSqlConnection()
        try {
            logger.info("{} ({})", sql, whereParameters)

            val sqlConverter = SqlConverter(backend.configuration)

            val ps = con.prepareStatement(sql, cursorType, cursorConcurrency)!!
            if (fetchSize != null) ps.fetchSize = fetchSize!!
            try {
                val parameterCount = ps.parameterMetaData.parameterCount
                whereParameters.withIndex().forEach { pair ->
                    if (pair.index < parameterCount) {
                        sqlConverter.setParameter(ps, pair.value, pair.index + 1, pair.value?.javaClass)
                    }
                }

                sqlbuilder.ResultSet(ps.executeQuery(), backend.configuration, sql).use { set ->
                    var row = 0
                    var continueCursorLoop = true
                    while (continueCursorLoop && set.next() && (offset == null || rows == null || rows!! + offset!! > row)) {
                        if (offset == null || (offset != null && offset!! <= row)) {
                            continueCursorLoop = rowHandler.handle(set, row)
                        }
                        row++
                    }
                }

                if (cacheStrategy != null) {
                    if (rowHandler is ReturningRowHandler<*>) {
                        cacheStrategy!!.put(CacheableQuery(sql, whereParameters, offset, rows), rowHandler.result!!)
                    }
                    if (rowHandler is OptionalReturningRowHandler<*>) {
                        if (rowHandler.result != null) {
                            cacheStrategy!!.put(CacheableQuery(sql, whereParameters, offset, rows), rowHandler.result!!)
                        }
                    }
                }
            } finally {
                ps.close()
            }
        } catch (e: SQLException) {
            throw PersistenceException("<$sql> failed with parameters $whereParameters", e)
        } finally {
            this.sql = null
            this.offset = null
            this.backend.closeConnection(con)
        }

        return rowHandler
    }

    private fun prepareSql(suppliedSql: String?, fields: List<String>?, rowHandler: RowHandler): PreparedSql {
        val sqlBuffer: StringBuilder
        if (suppliedSql == null) {
            sqlBuffer = StringBuilder(sql ?: "")
            sqlBuffer.append("select ")
            if (selectOption != null) {
                if (rowHandler is ExpandingRowHandler) {
                    sqlBuffer.append(rowHandler.expand(selectOption))
                } else {
                    sqlBuffer.append(selectOption)
                }
                sqlBuffer.append(' ')
            } else if (fields != null && fields.isNotEmpty()) {
                fields.joinTo(sqlBuffer, ",")
                sqlBuffer.append(" ")
            } else {
                sqlBuffer.append("* ")
            }
            if (entity != null) sqlBuffer.append("from ").append(entity)
        } else {
            if (rowHandler is ExpandingRowHandler) {
                sqlBuffer = StringBuilder(rowHandler.expand(sql))
            } else {
                sqlBuffer = StringBuilder(sql ?: "")
            }
        }
        val whereParameters: MutableList<Any?>
        if (this.parameters == null) {
            whereParameters = ArrayList<Any?>()
        } else {
            whereParameters = parameters!!.toMutableList()
        }
        if (whereGroup.isNotEmpty) {
            sqlBuffer.append(" where ")
            whereGroup.toSql(sqlBuffer, whereParameters)
        }
        if (groupBy != null) sqlBuffer.append(" ").append(groupBy)
        if (orderBy != null) {
            sqlBuffer.append(" order by ").append(orderBy).append(if (orderAscending) " asc" else " desc")
        }
        if (suffix != null) sqlBuffer.append(" ").append(suffix)

        return PreparedSql(sqlBuffer.toString(), whereParameters)
    }

    private fun injectRowHandler(rowHandler: Any, properties: List<PropertyReference>?) {
        if (rowHandler is PropertiesHandler) {
            rowHandler.properties = properties!!
        }
        if (rowHandler is ReflectionHandler) {
            rowHandler.metaResolver = metaResolver
        }
        if (rowHandler is DynamicBeanRowHandler<*>) {
            rowHandler.mappings = sqlMappings
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> from(rowHandler: RowHandler): T? {
        if (rowHandler is ReturningRowHandler<*>) {
            return rowHandler.result as T
        }

        if (rowHandler is OptionalReturningRowHandler<*>) {
            return rowHandler.result as T
        }

        return null
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

    override fun resultSetType(cursorType: Int, concurrency: Int): Select {
        this.cursorType = cursorType
        this.cursorConcurrency = concurrency
        return this
    }

    override fun groupBy(groupBy: String): Select {
        this.groupBy = groupBy
        return this
    }
}