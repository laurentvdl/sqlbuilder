package sqlbuilder.meta

import sqlbuilder.PersistenceException

import java.lang.reflect.Method

/**
 * Wrapper for bean property using getter/setter reflection.
 */
class JavaGetterSetterPropertyReference(override var name: String, private val method: Method, override var classType: Class<*>) : PropertyReference {
    override val columnName: String = findColumnName()

    override fun set(bean: Any, value: Any?) {
        try {
            if (!(value == null && classType.isPrimitive)) {
                method.invoke(bean, value)
            }
        } catch (e: Exception) {
            val signature = "${method.name}(${method.parameterTypes?.joinToString(",")})"
            throw PersistenceException("unable to set value $name to '$value' on bean $bean using setter <$signature>, expected argument of type <$classType>, but got <${value?.javaClass}>", e)
        }

    }

    override fun get(bean: Any): Any? {
        try {
            if (!method.isAccessible) {
                method.isAccessible = true
            }
            return method.invoke(bean)
        } catch (e: Exception) {
            val signature = "${method.name}(${method.parameterTypes?.joinToString(",")})"
            throw PersistenceException("unable to get value $name from bean $bean using getter $signature", e)
        }
    }

    private fun findColumnName(): String {
        try {
            val fieldAnnotationName = method.declaringClass.getDeclaredField(this.name)?.getAnnotation(Column::class.java)?.name
            if (fieldAnnotationName != null) {
                return fieldAnnotationName.toLowerCase()
            } else {
                return name.toLowerCase()
            }
        } catch(ignore: NoSuchFieldException) {
            return name.toLowerCase()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val setter = other as JavaGetterSetterPropertyReference

        return name == setter.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + classType.hashCode()
        return result
    }

    override fun toString(): String {
        return "property <${method.declaringClass}.$name>"
    }
}
