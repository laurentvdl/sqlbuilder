package sqlbuilder

import java.math.BigDecimal
import java.sql.Date
import java.sql.SQLException
import java.sql.Timestamp
import java.io.Closeable
import java.lang

class ResultSet(private val target: java.sql.ResultSet) : Closeable {

    public fun getJdbcResultSet(): java.sql.ResultSet {
        return target
    }

    throws(javaClass<SQLException>())
    public fun <T> getObject(targetType: Class<T>, index: Int): T {
        when {
            targetType == javaClass<String>() -> {
                val string = target.getString(index)
                if (string != null) return string.trim() as T
                return null
            }
            targetType == javaClass<Int>(), targetType == javaClass<lang.Integer>() -> {
                val intValue = target.getInt(index)
                if (target.wasNull()) {
                    return null
                } else {
                    return intValue as T
                }
            }
            targetType == javaClass<Long>(), targetType == javaClass<lang.Long>() -> {
                val longValue = target.getLong(index)
                if (target.wasNull()) {
                    return null
                } else {
                    return longValue as T
                }
            }
            targetType == javaClass<Short>(), targetType == javaClass<lang.Short>() -> {
                val shortValue = target.getShort(index)
                if (target.wasNull()) {
                    return null
                } else {
                    return shortValue as T
                }
            }
            targetType == javaClass<Double>(), targetType == javaClass<lang.Double>() -> {
                val doubleValue = target.getDouble(index)
                if (target.wasNull()) {
                    return null
                } else {
                    return doubleValue as T
                }
            }
            targetType == javaClass<Float>(), targetType == javaClass<lang.Float>() -> {
                val floatValue = target.getFloat(index)
                if (target.wasNull()) {
                    return null
                } else {
                    return floatValue as T
                }
            }
            targetType == javaClass<Timestamp>() -> {
                return target.getTimestamp(index) as T
            }
            targetType == javaClass<Date>(), targetType == javaClass<java.util.Date>() -> {
                val sqlDate = target.getDate(index)
                if (sqlDate == null) {
                    return null
                } else {
                    return java.util.Date(sqlDate.getTime()) as T
                }
            }
            targetType == javaClass<Boolean>() -> {
                val truth = target.getBoolean(index)
                if (target.wasNull()) return null
                return truth as T
            }
            targetType == javaClass<String>(), targetType == javaClass<lang.Character>(), targetType.getName() == "char" -> {
                return target.getString(index)?.charAt(0) as T
            }
            targetType == javaClass<ByteArray>() -> {
                return target.getBytes(index) as T
            }
            targetType == javaClass<Boolean>(), targetType == javaClass<lang.Boolean>(), targetType.getName() == "boolean" -> {
                return target.getBoolean(index) as T
            }
            javaClass<Enum<*>>().isAssignableFrom(targetType) -> {
                val enumValue = target.getInt(index)
                if (target.wasNull()) return null
                return targetType.getEnumConstants()?.get(enumValue) as T
            }
            targetType == javaClass<BigDecimal>() -> {
                return target.getBigDecimal(index) as T
            }
            javaClass<Any>() == targetType -> {
                return target.getObject(index) as T
            }
        }
        throw PersistenceException("unknown bean property, unable to find matching SQL variant: " + targetType.getName())
    }

    throws(javaClass<SQLException>())
    public fun next(): Boolean {
        return target.next()
    }

    throws(javaClass<SQLException>())
    override fun close() {
        target.close()
    }
}
