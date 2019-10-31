package sqlbuilder

import org.junit.Test
import sqlbuilder.impl.SqlBuilderImpl
import kotlin.test.assertEquals
import kotlin.test.fail

class ExceptionHandlingTest {
    @Test
    fun exceptionMessageContainsAllInfo() {
        val sqlBuilder = SqlBuilderImpl(Setup.dataSource)
        try {
            sqlBuilder.select().sql("select a from x").selectField(Int::class.java)
            fail("query should not be valid")
        } catch (e: PersistenceException) {
            assertEquals("<select a from x> failed with parameters [] caused by Table \"X\" not found; SQL statement:\n" +
                    "select a from x [42102-197]", e.message)
        }
    }
}