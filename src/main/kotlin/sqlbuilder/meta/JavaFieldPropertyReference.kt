package sqlbuilder.meta

import sqlbuilder.PersistenceException

import java.lang.reflect.Field

/**
 * Wrapper for bean property using java.lang.reflect.Field access.
 */
public class JavaFieldPropertyReference(override var name: String, private val field: Field, override var classType: Class<*>) : PropertyReference {
    private val fieldType = field.getType()!!

    override fun set(bean: Any, value: Any?) {
        try {
            if (!(value == null && fieldType.isPrimitive())) {
                field.set(bean, value)
            }
        } catch (e: Exception) {
            throw PersistenceException("unable to set value $name to '$value' on bean $bean using field access, expected argument of type <$fieldType>, but got <${value?.javaClass}>", e)
        }

    }

    override fun get(bean: Any): Any? {
        try {
            return field.get(bean)
        } catch (e: Exception) {
            throw PersistenceException("unable to get value $name from bean $bean using field access", e)
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
        return "property <$classType.$name>"
    }
}
