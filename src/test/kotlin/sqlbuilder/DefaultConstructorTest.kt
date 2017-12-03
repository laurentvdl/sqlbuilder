package sqlbuilder

import org.junit.Before
import org.junit.Test
import sqlbuilder.exceptions.NoDefaultConstructorException
import sqlbuilder.kotlin.select.selectBeans
import sqlbuilder.pojo.UserWithNoDefaultConstructor

class DefaultConstructorTest {
    @Before
    fun setup() {
        Setup.createTables(Setup.sqlBuilder)

        Setup.insertOneUser()
    }

    @Test(expected = NoDefaultConstructorException::class)
    fun explainsDefaultConstructorRequired() {
        Setup.sqlBuilder.select().selectBeans<UserWithNoDefaultConstructor>()
    }

    @Test(expected = NoDefaultConstructorException::class)
    fun explainsDefaultConstructorRequiredWhenUsingCustomSql() {
        Setup.sqlBuilder.select().sql("select * from users").selectBeans<UserWithNoDefaultConstructor>()
    }
}