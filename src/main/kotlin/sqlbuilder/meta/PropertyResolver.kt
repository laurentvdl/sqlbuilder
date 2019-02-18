package sqlbuilder.meta

import sqlbuilder.Configuration

interface PropertyResolver {
    fun resolvePropertiesForBean(beanClass: Class<*>, mutators: Boolean, configuration: Configuration): List<PropertyReference>
}