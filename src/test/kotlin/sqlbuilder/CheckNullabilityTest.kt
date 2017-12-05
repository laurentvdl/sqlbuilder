package sqlbuilder

import org.junit.Before
import org.junit.Test
import sqlbuilder.exceptions.NotNullColumnException
import sqlbuilder.kotlin.beans.User

class CheckNullabilityTest {
    @Before
    fun setup() {
        Setup.createTables()
    }

    @Test(expected = NotNullColumnException::class)
    fun discoverNullability() {
        Setup.sqlBuilder.insert().checkNullability(true).insertBean(User(
                null, null, null, null
        ))
    }
}