package sqlbuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import sqlbuilder.beans.Attribute;
import sqlbuilder.beans.File;
import sqlbuilder.beans.User;
import sqlbuilder.impl.SqlBuilderImpl;
import sqlbuilder.impl.mappers.BooleanCharMapper;
import sqlbuilder.mapping.ToObjectMapper;
import sqlbuilder.mapping.ToObjectMappingParameters;
import sqlbuilder.pool.DataSourceImpl;
import sqlbuilder.pool.DefaultConfig;
import sqlbuilder.pool.Drivers;
import sqlbuilder.rowhandler.BeanListRowHandler;
import sqlbuilder.rowhandler.JoiningPagedRowHandler;
import sqlbuilder.rowhandler.JoiningRowHandler;

/**
 * Test the API using Java code
 *
 * @author Laurent Van der Linden
 */
public class JavaUsage {
    private final DataSourceImpl dataSource = new DataSourceImpl(new DefaultConfig(null, null, "jdbc:h2:mem:test", Drivers.H2));
    private final SqlBuilder sqlBuilder = new SqlBuilderImpl(dataSource);

    @Before
    public void setup() {
        Setup.createTables();

        assertEquals(
            "generated key should be 1", 1L,
            sqlBuilder.insert().getKeys(true).insertBean(new User("javauser a", 2014, 'M', null, true, true))
        );

        assertEquals(
            "generated key should be 2", 2L,
            sqlBuilder.insert().getKeys(true).into("users").insertBean(new User("javauser b", 2014, 'F', 1L, false, false))
        );

        final Long fileId = sqlBuilder.insert().getKeys(true).insertBean(new File(1L, "profile"));
        sqlBuilder.insert().insertBean(new Attribute(fileId, "size", "2kb"));
        sqlBuilder.insert().insertBean(new Attribute(fileId, "color", "green"));

        sqlBuilder.insert().getKeys(true).insertBean(new File(2L, "profile"));
        sqlBuilder.insert().getKeys(true).insertBean(new File(2L, "avatar"));
    }

    @Test
    public void selectBeans() {
        final List<User> users = sqlBuilder.select().from("users").selectBeans(User.class);
        assertEquals(2, users.size());

        assertEquals(users, sqlBuilder.select().selectBeans(User.class));

        final User firstUser = users.get(0);
        assertEquals("javauser a", firstUser.getUsername());
    }

    @Test
    public void beanUpdate() {
        User firstUser = sqlBuilder.select().where("id = 1").selectBean(User.class);
        assertNotNull(firstUser);
        firstUser.setBirthYear(2000);
        sqlBuilder.update().entity("users").updateBean(firstUser);

        final User updatedUser = sqlBuilder.select().where("id = ?", firstUser.getId()).selectBean(User.class);
        assertNotNull(updatedUser);
        assertEquals(2000, updatedUser.getBirthYear());
    }

    @Test
    public void selectMaps() {
        final List<RowMap> maps = sqlBuilder.select().from("users").selectMaps("birthyear");
        assertEquals(2014, maps.get(0).get("birthyear"));
        assertEquals(2014, maps.get(0).get(0));
    }

    @Test
    public void selectAllFields() {
        final List<String> allUserNames = sqlBuilder.select().from("users").selectAllField("username", String.class);
        assertEquals(2, allUserNames.size());
        assertEquals("javauser a", allUserNames.get(0));
        assertEquals("javauser b", allUserNames.get(1));
    }

    @Test
    public void joinHandler() {
        final List<User> allUsersAndFiles = sqlBuilder.select()
                .sql("select * from users " +
                        "left join files on users.id = files.userid " +
                        "left join attributes on files.id = attributes.fileid")
                .select(new JoiningRowHandler<User>() {
                    @Override
                    public boolean handle(@NotNull ResultSet set, int row) {
                        final User user = mapPrimaryBean(set, User.class, "users");
                        join(set, user, "parent", User.class, "parents");
                        final File file = join(set, user, "files", File.class, "files");
                        join(set, file, "attributes", Attribute.class, "attributes");
                        return true;
                    }
                }.entities(User.class, File.class, Attribute.class));

        validateJoinedResult(allUsersAndFiles);
        for (User userAndFiles : allUsersAndFiles) {
            assertNotEquals("table alias is use to distinguish tables in different role", userAndFiles.getId(), userAndFiles.getParent_id());
        }

        // some databases like Oracle don't fully support JDBC resultset metadata, so we need to be more explicit
        // for every bean in the query, specify a macro to include all properties for that bean
        // or if cherry picking, define a sql alias like "users.firstname as users_firstname"

        // also, if you need the same table in multiple contexts (ie. parent-child), use macros or prefixed column aliases,
        // as JDBC metadata does not expose table aliases
        final List<User> allUsersAndFiles2 = sqlBuilder.select()
            .sql("select {User.* as someuser},{User.* as parents},{File.*},{Attribute.*} from users someuser " +
                    "left join users parents on someuser.parent_id = parents.id " +
                    "left join files on someuser.id = files.userid " +
                    "left join attributes on files.id = attributes.fileid")
            .select(new JoiningRowHandler<User>() {
                @Override
                public boolean handle(@NotNull ResultSet set, int row) {
                    // use custom prefix
                    final User user = mapPrimaryBean(set, User.class, "someuser");
                    join(set, user, "parent", User.class, "parents");
                    // specify default prefix: table name
                    final File file = join(set, user, "files", File.class, "files");
                    // leave prefix null, will be inferred as table name
                    join(set, file, "attributes", Attribute.class, null);
                    return true;
                }
            }.entities(User.class, File.class, Attribute.class));

        validateJoinedResult(allUsersAndFiles2);
        assertEquals("parent should be set for user b", allUsersAndFiles2.get(1).getParent(), allUsersAndFiles.get(0));
    }

    private void validateJoinedResult(List<User> allUsersAndFiles) {
        final Set<File> firstUserFiles = allUsersAndFiles.get(0).getFiles();
        assertEquals(1, firstUserFiles.size());

        final Set<Attribute> attributes = firstUserFiles.iterator().next().getAttributes();
        assertEquals(2, attributes.size());
        for (Attribute attribute : attributes) {
            if (attribute.getId() == 1) {
                assertEquals("2kb", attribute.getValue());
            }
            if (attribute.getId() == 2) {
                assertEquals("green", attribute.getValue());
            }
        }
    }

    @Test
    public void pagedJoinHandler() {
        final List<? extends User> allUsersAndFiles = sqlBuilder.select()
            .sql("select * from users left join files on users.id = files.userid left join attributes on files.id = attributes.fileid")
            .select(new JoiningPagedRowHandler<User>(1, 1, "users") {
                @Override
                public void handleInPage(@NotNull ResultSet set, int row) {
                    final User user = mapPrimaryBean(set, User.class, "users");
                    final File file = join(set, user, "files", File.class, "files");
                    join(set, file, "attributes", Attribute.class, "attributes");
                }
            });

        assertEquals(1, allUsersAndFiles.size());
        assertEquals(2L, allUsersAndFiles.get(0).getId().longValue());
        assertEquals(2, allUsersAndFiles.get(0).getFiles().size());
    }

    @Test
    public void criteria() {
        final String usernameFilter = null;
        final List<User> filteredUsers = sqlBuilder.select()
            .where()
                .group()
                    .and("username like ?", "java%")
                    .and("username like ?", "%user%")
                    .and(usernameFilter != null, "username like ?", usernameFilter)
            .endGroup()
            .or("id > 10")
            .endWhere()
            .selectBeans(User.class);

        assertEquals(2, filteredUsers.size());
    }

    @Test
    public void delete() {
        final User lastUser = sqlBuilder.select().where("id = 2").selectBean(User.class);

        assertNotNull("user with id 2 not found", lastUser);
        sqlBuilder.delete().deleteBean(lastUser);

        assertEquals(1, sqlBuilder.select().selectBeans(User.class).size());
    }

    @Test
    public void mapping() {
        List<User> mappedResults = sqlBuilder.select()
            .sql("select sex,count(*) from users group by sex order by sex desc")
            .map("gender", "username")
            .map(2, "birthyear")
            .selectBeans(User.class);

        assertEquals('M', mappedResults.get(0).getGender());
        assertEquals(1, mappedResults.get(0).getBirthYear());
        assertEquals('F', mappedResults.get(1).getGender());
        assertEquals(1, mappedResults.get(1).getBirthYear());
    }

    @Test(expected = IncorrectResultSizeException.class)
    public void errorHandling() {
        sqlBuilder.select()
            .from("users")
            .selectBean(User.class);
    }
	
	@Test
	public void customHandler() {
		final List<AbstractMap.SimpleEntry<Integer,String>> userEntries = sqlBuilder.select()
			.sql("select id,username from users")
			.select(new BeanListRowHandler<AbstractMap.SimpleEntry<Integer,String>>() {
				@Override
				public AbstractMap.SimpleEntry<Integer, String> mapSetToListItem(@NotNull ResultSet resultSet) throws SQLException {
					return new AbstractMap.SimpleEntry<>(
						resultSet.getObject(Integer.class, 1),
						resultSet.getJdbcResultSet().getString(2)
					);
				}
			});

		assertEquals(2, userEntries.size());
		assertEquals("javauser a", userEntries.get(0).getValue());
	}

    @Test
    public void customBooleanType() {
        sqlBuilder.getConfiguration().registerToObjectMapper(new ToObjectMapper() {
            @Nullable
            @Override
            public Object toObject(@NotNull ToObjectMappingParameters params) throws SQLException {
                final String text = params.getResultSet().getString(params.getIndex());
                if (text == null) {
                    return null;
                }
                return "X".equalsIgnoreCase(text);
            }

            @Override
            public boolean handles(@NotNull Class<?> targetType) {
                return Boolean.class.equals(targetType);
            }
        });

        assertEquals("X should mark the spot",
                Boolean.TRUE,
                sqlBuilder.select()
                .sql("select 'X'")
                .selectField(null, Boolean.class)
        );

        assertEquals("the truth is not out there",
                null,
                sqlBuilder.select()
                .sql("select null")
                .selectField(null, Boolean.class)
        );
    }

    @Test
    public void selectOption() {
        final Select select = sqlBuilder.select()
                .from("users u");

        select.selectField("count(*)", Integer.class);

        select
                .selectOption("u.*")
                .selectBeans(User.class);

    }

    @Test
    public void excludeFields() {
        final List<User> usersWithoutUsername = sqlBuilder.select()
                .from("users u")
                .excludeFields("username")
                .selectBeans(User.class);

        assertNull("username should not be set", usersWithoutUsername.get(0).getUsername());

        final List<User> usersWithUsername = sqlBuilder.select()
                .from("users u")
                .includeFields("username")
                .selectBeans(User.class);

        assertNotNull("username should be set", usersWithUsername.get(0).getUsername());
    }

    @Test
    public void nullConditionParameter() {
        final String username = null;
        final List<User> users = sqlBuilder.select()
                .from("users u")
                .where("username = ?", username)
                .selectBeans(User.class);

        assertTrue("no users should have a username of NULL", users.isEmpty());
    }

    @Test
    public void handlesBooleanIsGettersSetters() {
        final User a = sqlBuilder.select()
                .from("users u")
                .where("username = ?", "javauser a")
                .selectBean(User.class);
        assertTrue("javauser a should be a super user", a.isSuperUser());
        assertTrue("javauser a should be active", a.isActive());

        final User b = sqlBuilder.select()
                .from("users u")
                .where("username = ?", "javauser b")
                .selectBean(User.class);
        assertFalse("javauser b should not be a super user", b.isSuperUser());
        assertFalse("javauser a should not be active", b.isActive());
    }

    @Test
    public void mapsBooleanValuesToChar() {
        final BooleanCharMapper booleanCharMapper = new BooleanCharMapper("X", "_");
        final SqlBuilder customSqlBuilder = new SqlBuilderImpl(dataSource);
        customSqlBuilder.getConfiguration().registerToSQLMapper(booleanCharMapper);
        customSqlBuilder.getConfiguration().registerToObjectMapper(booleanCharMapper);
        customSqlBuilder.update().updateStatement("create table truths (column char(1))");
        customSqlBuilder.update().updateStatement("insert into truths values (?)", false);
        final Boolean truth = customSqlBuilder.select().from("truths").selectField(Boolean.class);
        final String truthText = customSqlBuilder.select().from("truths").selectField(String.class);
        assertNotNull(truth);
        assertFalse(truth);
        assertEquals(truthText, "_");
    }
}
