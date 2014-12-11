package sqlbuilder.meta

import java.lang.reflect.Field

/**
 * @author
 */
public trait MetaResolver {
    fun <T> getProperties(beanClass: Class<T>, mutators: Boolean): List<PropertyReference>

    fun <T> getTableName(beanClass: Class<T>): String

    /**
     * Find a field in a type or its super type
     */
    fun <T> findField(name: String, fieldType: Class<in T>): Field?

    fun <T> getKeys(beanClass: Class<T>): Array<String>
}
