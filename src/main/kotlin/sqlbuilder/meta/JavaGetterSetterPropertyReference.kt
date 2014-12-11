package sqlbuilder.meta

import sqlbuilder.PersistenceException

import java.lang.reflect.Method

/**
 * Wrapper for bean property using getter/setter reflection.
 *
 * @author Laurent Van der Linden
 */
public class JavaGetterSetterPropertyReference(override var name: String, private val method: Method, override var classType: Class<*>) : PropertyReference {
    override fun set(bean: Any, value: Any?) {
        try {
            if (!(value == null && classType.isPrimitive())) {
                method.invoke(bean, value)
            }
        } catch (e: Exception) {
            val signature = "${method.getName()}(${method.getParameterTypes()?.joinToString(",")})"
            throw PersistenceException("unable to set value $name to '$value' on bean $bean using setter <${signature}>, expected argument of type <${classType}>, but got <${value?.javaClass}>", e)
        }

    }

    override fun get(bean: Any): Any? {
        try {
            if (!method.isAccessible()) {
                method.setAccessible(true)
            }
            return method.invoke(bean)
        } catch (e: Exception) {
            val signature = "${method.getName()}(${method.getParameterTypes()?.joinToString(",")})"
            throw PersistenceException("unable to get value $name from bean $bean using getter $signature", e)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this == other) return true
        if (other == null || javaClass != other.javaClass) return false

        val setter = other as JavaGetterSetterPropertyReference

        return name == setter.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "property <$name>"
    }
}
