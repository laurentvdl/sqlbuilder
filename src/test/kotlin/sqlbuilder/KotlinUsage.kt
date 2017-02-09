package sqlbuilder

import org.junit.Before
import org.junit.Test
import sqlbuilder.impl.SqlBuilderImpl
import sqlbuilder.meta.Table
import sqlbuilder.meta.Transient
import sqlbuilder.pool.DataSourceImpl
import sqlbuilder.pool.DefaultConfig
import sqlbuilder.pool.Drivers
import sqlbuilder.rowhandler.JoiningRowHandler
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KotlinUsage {
    private val sqlBuilder = SqlBuilderImpl(DataSourceImpl(
            DefaultConfig(
                    null, null, "jdbc:h2:mem:test", Drivers.H2
            )
    ))

    @Before fun setup() {
        Setup.createTables(sqlBuilder)

        assertEquals(1L, sqlBuilder.insert().getKeys(true).insertBean(User().apply {
            username = "test a"
            birthYear = 1976
        }), "generated key incorrect")

        assertEquals(2L, sqlBuilder.insert().getKeys(true).insertBean(User().apply {
            username = "test b"
            birthYear = 1977
        }), "generated key incorrect")

        sqlBuilder.insert().insertBean(File().apply {
            userid = 1
            name = "home"
        })

        sqlBuilder.insert().insertBean(File().apply {
            userid = 1
            name = "guest"
        })

        sqlBuilder.insert().insertBean(Attribute().apply {
            fileid = 1
            name = "size"
            value = "2kb"
        })
    }

    @Test fun selectBeans() {
        val users = sqlBuilder.select().from("users").selectBeans(User::class.java)
        assertEquals(2, users.size, "row count")
        assertEquals(1L, users[0].id)
        assertEquals(users[0].username, "test a")
    }

    @Test fun selectMap() {
        val usersAsMaps = sqlBuilder.select().from("users").selectMaps("*")
        assertEquals(1L, usersAsMaps[0].get("id"))
        assertEquals(1L, usersAsMaps[0].get(0))
    }

    @Test fun joinHandler() {
        val usersWithFiles = sqlBuilder.select()
                .sql("select * from users left join files on users.id = files.userid left join attributes on files.id = attributes.fileid")
                .select(object : JoiningRowHandler<User>() {
                    override fun handle(set: ResultSet, row: Int): Boolean {
                        val user = mapPrimaryBean(set, User::class.java, "users")
                        val file = joinList(set, user, User::files, File::class.java, "files")
                        joinSet(set, file, File::attributes, Attribute::class.java, "attributes")
                        return true
                    }
                })
        assertEquals(2, usersWithFiles.first().files?.size, "first user should have 2 files")
    }

    @Test fun criteria() {
        val filteredCount = sqlBuilder.select()
                .from("users")
                .where()
                .and("username like ?", "%a")
                .and("username like ?", "test%")
                .endWhere()
                .selectField("count(*)", Long::class.java)

        assertEquals(1L, filteredCount, "expecting x users starting matching wildcard")
    }

    @Test fun caching() {
        val firstMaps = sqlBuilder.select()
                .from("users")
                .cache()
                .selectMaps("*")

        val firstBeans = sqlBuilder.select()
                .from("users")
                .cache()
                .selectBeans(User::class.java)

        val firstCount = sqlBuilder.select()
                .from("users")
                .cache()
                .selectField("count(*)", java.lang.Long::class.java)

        sqlBuilder.update().updateStatement("delete from users where id = ?", 1)

        val secondMaps = sqlBuilder.select()
                .from("users")
                .cache()
                .selectMaps("*")

        val secondBeans = sqlBuilder.select()
                .from("users")
                .cache()
                .selectBeans(User::class.java)

        val secondCount = sqlBuilder.select()
                .from("users")
                .cache()
                .selectField("count(*)", java.lang.Long::class.java)

        assertEquals(secondMaps.size, firstMaps.size)
        assertEquals(secondBeans.size, firstBeans.size)
        assertEquals(secondCount, firstCount)

        val thridMaps = sqlBuilder.select()
                .from("users")
                .selectMaps("*")

        assertEquals(1, thridMaps.size)

        val thirdBeans = sqlBuilder.select()
                .from("users")
                .selectBeans(User::class.java)

        assertEquals(1, thirdBeans.size)

        val thirdCount = sqlBuilder.select()
                .from("users")
                .selectField("count(*)", java.lang.Long::class.java)

        assertEquals(1, thirdCount!!.toLong())
    }

    @Test(expected = IncorrectMetadataException::class)
    fun verifyKeys() {
        sqlBuilder.update().updateBean(InvalidBean())
    }

    @Test
    fun transientFields() {
        val firstUserWithBirthYear = sqlBuilder.select().offset(0, 1).selectBean(User::class.java)
        assertNotNull(firstUserWithBirthYear?.birthYear)

        val firstUserWithoutBirthYear = sqlBuilder.select().offset(0, 1).selectBean(UserWithNoBirthYear::class.java)
        assertNull(firstUserWithoutBirthYear?.birthYear)
    }

    @Test
    fun joinAliasCaseSensitivity() {
        val usersWithFiles = sqlBuilder.select()
                .sql("select {User.* as users},{File.* as files},{Attribute.* as attributes} from users left join files on users.id = files.userid left join attributes on files.id = attributes.fileid")
                .select(object : JoiningRowHandler<User>() {
                    override fun handle(set: ResultSet, row: Int): Boolean {
                        val user = mapPrimaryBean(set, User::class.java, "USERS")
                        val file = joinList(set, user, User::files, File::class.java, "files")
                        joinSet(set, file, File::attributes, Attribute::class.java, "attributes")
                        return true
                    }
                }.entities(User::class.java, File::class.java, Attribute::class.java))

        assertNotNull(usersWithFiles.first().birthYear)
        assertNotNull(usersWithFiles.first().files?.first()?.name)
    }

    @Table("users")
    class User {
        var id: Long? = null
        var username: String? = null
        var birthYear: Short? = null
        var files: MutableList<File>? = null
    }

    @Table("users")
    class UserWithNoBirthYear {
        var id: Long? = null
        var username: String? = null
        @Transient
        var birthYear: Short? = null
        var files: MutableList<File>? = null
    }

    @Table("files")
    class File {
        var id: Long? = null
        var userid: Long? = null
        var name: String? = null
        var attributes: MutableSet<Attribute>? = null
    }

    @Table("attributes")
    class Attribute {
        var id: Long? = null
        var fileid: Long? = null
        var name: String? = null
        var value: String? = null
    }

    class InvalidBean {
        var uuid: String? = null
    }
}