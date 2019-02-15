package sqlbuilder

import org.junit.Before
import org.junit.Test
import sqlbuilder.kotlin.beans.Attribute
import sqlbuilder.kotlin.beans.File
import sqlbuilder.kotlin.beans.User
import sqlbuilder.kotlin.select
import sqlbuilder.kotlin.select.excludeProperties
import sqlbuilder.kotlin.select.group
import sqlbuilder.kotlin.select.includeProperties
import sqlbuilder.kotlin.select.join.mapPrimaryBean
import sqlbuilder.kotlin.select.or
import sqlbuilder.kotlin.select.selectBean
import sqlbuilder.kotlin.select.selectBeans
import sqlbuilder.kotlin.select.selectField
import sqlbuilder.kotlin.select.selectJoinedEntities
import sqlbuilder.kotlin.select.selectJoinedEntitiesPaged
import sqlbuilder.kotlin.select.where
import sqlbuilder.kotlin.update.excludeProperties
import sqlbuilder.kotlin.update.includeProperties
import sqlbuilder.meta.Table
import sqlbuilder.meta.Transient
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class KotlinUsage {
    private val sqlBuilder = Setup.sqlBuilder

    @Before fun setup() {
        Setup.createTables()

        assertEquals(1L, sqlBuilder.insert().getKeys(true).insertBean(User(
                username = "test a",
                birthYear = 1976,
                files = null,
                id = null
        )), "generated key incorrect")

        assertEquals(2L, sqlBuilder.insert().getKeys(true).insertBean(User(
                username = "test b",
                birthYear = 1977,
                files = null,
                id = null
        )), "generated key incorrect")

        sqlBuilder.insert().insertBean(File(
                userid = 1,
                name = "home",
                id = null,
                attributes = null
        ))

        sqlBuilder.insert().insertBean(File(
                userid = 1,
                name = "guest",
                id = null,
                attributes = null
        ))

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
                .selectJoinedEntities<User> { set, _ ->
                    val user = mapPrimaryBean(set, User::class.java, "users")
                    val file = joinCollection(set, user, User::files, "files")
                    joinCollection(set, file, File::attributes, "attributes")
                }
        assertEquals(2, usersWithFiles.first().files?.size, "first user should have 2 files")
    }

    @Test fun joinPagedHandler() {
        val usersWithFiles = sqlBuilder.select()
                .sql("select * from users left join files on users.id = files.userid left join attributes on files.id = attributes.fileid")
                .selectJoinedEntitiesPaged<User>(offset = 0, rows = 1, prefix = "users") { set, _ ->
                    val user = mapPrimaryBean(set, User::class.java, "users")
                    val file = joinCollection(set, user, User::files, "files")
                    joinCollection(set, file, File::attributes, "attributes")
                }
        assertEquals(1, usersWithFiles.size, "only the first user should be included")
        assertEquals(2, usersWithFiles.first().files?.size, "first user should have 2 files")
    }

    @Test fun criteria() {
        val filteredCount = sqlBuilder.select {
            sql("select count(*) from users")
            where {
                and("username like ?", "%a")
                group {
                    and("username like ?", "test%")
                    or("username like ?", "abc%")
                }
            }
            selectField<Long>()
        }

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
                .sql("select count(*) from users")
                .cache()
                .selectField<Long>()

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
                .sql("select count(*) from users")
                .cache()
                .selectField<Long>()

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

        val thirdCount = sqlBuilder.select().sql("select count(*) from users").selectField<Long>()

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
                .selectJoinedEntities<User>(setOf(File::class.java, Attribute::class.java)) { set: ResultSet, _: Int ->
                    val user = mapPrimaryBean(set, "USERS")
                    val file = joinCollection(set, user, User::files, "files")
                    joinCollection(set, file, File::attributes, "attributes")
                }

        assertNotNull(usersWithFiles.first().birthYear)
        assertNotNull(usersWithFiles.first().files?.first()?.name)
    }

    @Test
    fun emptyNestedWhereGroup() {
        sqlBuilder.select()
                .where()
                    .group()
                        .or(false, "jiberisch")
                        .or(false, "jiberda")
                    .endGroup()
                    .and(false, "jiberu")
                .endWhere()
            .selectBeans(User::class.java)

        sqlBuilder.select {
            where {
                and("1 = 1")
                group {
                    or(false, "1 = 1")
                    or(emptyList()) { subGroup: WhereGroup, item: String -> subGroup.or(item) }
                }
            }
            selectBeans<User>()
        }
    }

    @Test(expected = IncorrectJoinMapping::class)
    fun duplicateAssignment() {
        sqlBuilder.select()
            .sql("select * from users " +
                    "left join users parents on users.parent_id = parents.id")
            .selectJoinedEntities<User>(setOf(File::class.java, Attribute::class.java)) { set,_ ->
                val user = mapPrimaryBean(set, User::class.java, "users")
                join(set, user, "parent", User::class.java, "parents")
            }
    }

    @Test
    fun joinCustomAliasMix() {
        val usersWithFiles = sqlBuilder.select()
                .sql("select u.id as users_id,{File.* as files} from users u left join files on u.id = files.userid")
                .selectJoinedEntities<User>(setOf(File::class.java, Attribute::class.java)) { set,_ ->
                    val user = mapPrimaryBean(set, User::class.java, "users")
                    joinCollection(set, user, User::files, "files")
                }

        assertNotNull(usersWithFiles.first().id)
        assertNotNull(usersWithFiles.first().files?.first()?.name)
    }

    @Test
    fun excludeSelectProperties() {
        val usersWithoutUsername = sqlBuilder.select().excludeProperties(User::username, User::files).selectBeans<User>()
        assertNull(usersWithoutUsername.first().username)
        assertNotNull(usersWithoutUsername.first().id)
        assertNotNull(usersWithoutUsername.first().birthYear)
    }

    @Test
    fun includeSelectProperties() {
        val usersWithUsername = sqlBuilder.select().includeProperties(User::id, User::username).selectBeans<User>()
        assertNotNull(usersWithUsername.first().id)
        assertNotNull(usersWithUsername.first().username)
        assertNull(usersWithUsername.first().birthYear)
    }

    @Test
    fun excludePropertiesForUpdate() {
        val firstUser = sqlBuilder.select().offset(0, 1).selectBean<User>() ?: fail("Expected at least 1 user")

        firstUser.username = "update"
        firstUser.birthYear = 2222
        sqlBuilder.update().excludeProperties(User::birthYear).updateBean(firstUser)

        val updatedUser = sqlBuilder.select().offset(0, 1).selectBean<User>() ?: fail("Expected at least 1 user")

        assertEquals(firstUser.username, updatedUser.username)
        assertNotEquals(firstUser.birthYear, updatedUser.birthYear)
    }

    @Test
    fun includePropertiesForUpdate() {
        val firstUser = sqlBuilder.select().offset(0, 1).selectBean<User>() ?: fail("Expected at least 1 user")

        firstUser.username = "update"
        firstUser.birthYear = 2222
        sqlBuilder.update().includeProperties(User::birthYear).updateBean(firstUser)

        val updatedUser = sqlBuilder.select().offset(0, 1).selectBean<User>() ?: fail("Expected at least 1 user")

        assertNotEquals(firstUser.username, updatedUser.username)
        assertEquals(firstUser.birthYear, updatedUser.birthYear)
    }

    @Test
    fun generatedKeyIsStoredInBean() {
        val user = User(null, "dude", 1979, emptyList<File>())
        val generatedKey = sqlBuilder.insert().insertBean(user)
        assertEquals(generatedKey, user.id)
    }

    @Test
    fun filterByIterable() {
        val userNames = listOf("test a", "test b")

        var users = sqlBuilder.select {
            where {
                or(userNames, object: WhereGroupVisitor<String> {
                    override fun forItem(subGroup: WhereGroup, item: String) {
                        subGroup.or("username = ?", item)
                    }
                })
            }
            selectBeans<User>()
        }

        assertEquals(2, users.size)
    }

    @Test
    fun alwaysInitializesRelationCollections() {
        val username = "userwithnofiles"
        sqlBuilder.insert().getKeys(true).insertBean(User(
                username = username,
                birthYear = 1976,
                files = null,
                id = null
        ))

        val userWithNoFiles = sqlBuilder.select()
                .sql("select * from users " +
                        " left join files files on users.id = files.userid" +
                        " where username = ?", username)
                .selectJoinedEntities<User>(setOf(File::class.java, Attribute::class.java)) { set,_ ->
                    val user = mapPrimaryBean(set, User::class.java, "users")
                    join(set, user, "files", File::class.java, "files")
                }
                .first()

        assertNotNull(userWithNoFiles.files, "Files collection should be initialized")
        assertTrue(userWithNoFiles.files!!.isEmpty(), "Files collection should be initialized")
    }

    @Test
    fun recognizesFinalFields() {
        val username = "userwithnofiles"
        sqlBuilder.insert().getKeys(true).insertBean(User(
            username = username,
            birthYear = 1976,
            files = null,
            id = null
        ))

        val finalUser = sqlBuilder.select().where("username = ?", username).selectBean(UserWithFinalFields::class.java)
        assertNotNull(finalUser)
        assertNotNull(finalUser.username)
    }

    @Table("users")
    data class UserWithNoBirthYear (
        var id: Long?,
        var username: String?,
        @Transient
        var birthYear: Short?,
        var files: MutableList<File>?
    )

    class InvalidBean {
        var uuid: String? = null
    }

    @Table("users")
    data class UserWithFinalFields (
        val id: Long?,
        val username: String?
    )
}