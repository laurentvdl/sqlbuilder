package sqlbuilder.meta

import java.lang.reflect.Field

/**
 * @author Laurent Van der Linden
 */
public interface MetaResolver {
    fun getProperties(beanClass: Class<*>, mutators: Boolean): List<PropertyReference>

    fun getTableName(beanClass: Class<*>): String

    /**
     * Find a field in a type or its super type
     */
    fun findField(name: String, fieldType: Class<*>): Field?

    fun getKeys(beanClass: Class<*>): Array<String>
}
