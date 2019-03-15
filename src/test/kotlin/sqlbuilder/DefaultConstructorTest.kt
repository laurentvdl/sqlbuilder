package sqlbuilder

import org.junit.Before
import org.junit.Test
import sqlbuilder.beans.UserWithNoDefaultConstructor
import sqlbuilder.exceptions.NoDefaultConstructorException
import sqlbuilder.impl.SqlBuilderImpl
import sqlbuilder.kotlin.select.selectBeans
import sqlbuilder.meta.FieldPropertyResolver

class DefaultConstructorTest {
    private val sqlBuilder = SqlBuilderImpl(Setup.dataSource).apply {
        this.configuration.propertyResolutionStrategies = listOf(FieldPropertyResolver())
    }

    @Before
    fun setup() {
        Setup.createTables()

        Setup.insertOneUser()
    }

    @Test(expected = NoDefaultConstructorException::class)
    fun explainsDefaultConstructorRequired() {
        sqlBuilder.select().selectBeans<UserWithNoDefaultConstructor>()
    }

    @Test(expected = NoDefaultConstructorException::class)
    fun explainsDefaultConstructorRequiredWhenUsingCustomSql() {
        sqlBuilder.select().sql("select * from users").selectBeans<UserWithNoDefaultConstructor>()
    }
}