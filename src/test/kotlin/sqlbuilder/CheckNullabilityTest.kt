package sqlbuilder

import org.junit.Before
import org.junit.Test
import sqlbuilder.exceptions.NotNullColumnException
import sqlbuilder.impl.SqlBuilderImpl
import sqlbuilder.kotlin.beans.User

class CheckNullabilityTest {
    private val sqlBuilder = SqlBuilderImpl(Setup.dataSource)

    @Before
    fun setup() {
        Setup.createTables()
    }

    @Test(expected = NotNullColumnException::class)
    fun discoverNullability() {
        sqlBuilder.insert().checkNullability(true).insertBean(User(
                null, null, null, null
        ))
    }
}