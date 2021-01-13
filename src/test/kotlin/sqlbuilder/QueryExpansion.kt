package sqlbuilder

import org.junit.Assert
import org.junit.Test
import sqlbuilder.beans.File
import sqlbuilder.beans.User
import sqlbuilder.impl.DefaultConfiguration
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

        Assert.assertEquals("select someuser.active as someuser_0,someuser.birthYear as someuser_1," +
                "someuser.id as someuser_2,someuser.parent_id as someuser_3,someuser.sex as someuser_4," +
                "someuser.superUser as someuser_5,someuser.username as someuser_6,files.ID as files_0,files.name as files_1," +
                "files.userid as files_2 from users someuser",
                joiner.expand("select {User.* as someuser},{File.*} from users someuser")
        )
    }
}