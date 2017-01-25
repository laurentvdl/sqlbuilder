package sqlbuilder

import org.junit.Assert
import org.junit.Test
import sqlbuilder.impl.DefaultConfiguration
import sqlbuilder.javabeans.File
import sqlbuilder.javabeans.User
import sqlbuilder.meta.ReflectionResolver
import sqlbuilder.rowhandler.JoiningRowHandler

class QueryExpansion {
    @Test fun testJoinExpansion() {
        val joiner = object : JoiningRowHandler<User>() {
            override fun handle(set: ResultSet, row: Int): Boolean {
                return true
            }
        }

        joiner.metaResolver = ReflectionResolver(DefaultConfiguration())

        joiner.entities(User::class.java, File::class.java)

        Assert.assertEquals("select someuser.birthyear as someuser_0,someuser.id as someuser_1,someuser.sex as someuser_2," +
                "someuser.username as someuser_3,files.id as files_0,files.name as files_1,files.userid as files_2 from users someuser",
                joiner.expand("select {User.* as someuser},{File.*} from users someuser")
        )
    }
}