package sqlbuilder

import org.junit.Test
import sqlbuilder.impl.DefaultConfiguration
import sqlbuilder.javabeans.File
import sqlbuilder.javabeans.User
import sqlbuilder.meta.StaticJavaResolver
import sqlbuilder.rowhandler.JoiningRowHandler
import kotlin.test.assertEquals

class QueryExpansion {
    @Test fun testJoinExpansion() {
        val joiner = object : JoiningRowHandler<User>() {
            override fun handle(set: ResultSet, row: Int): Boolean {
                return true
            }
        }

        joiner.metaResolver = StaticJavaResolver(DefaultConfiguration())

        joiner.entities(User::class.java, File::class.java)

        assertEquals("select users.birthyear as users_birthyear,users.gender as users_gender,users.id as users_id," +
                "users.username as users_username,files.id as files_id,files.name as files_name,files.userid as files_userid from users",
                joiner.expand("select {User.*},{File.*} from users")
        )
    }
}