package sqlbuilder.javabeans;

import java.util.Set;

import sqlbuilder.meta.Id;
import sqlbuilder.meta.Table;

/**
 * @author Laurent Van der Linden
 */
@Table(name = "users")
public class User {
    @Id
    private Long id;
    private String username;
    private int birthYear;
    private char gender;
    private Set<File> files;

    public User() {
    }

    public User(String username, int birthYear, char gender) {
        this.username = username;
        this.birthYear = birthYear;
        this.gender = gender;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(int birthYear) {
        this.birthYear = birthYear;
    }

    public char getGender() {
        return gender;
    }

    public void setGender(char gender) {
        this.gender = gender;
    }

    public Set<File> getFiles() {
        return files;
    }

    public void setFiles(Set<File> files) {
        this.files = files;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (id != null ? !id.equals(user.id) : user.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "JavaUser{" +
            "id=" + id +
            ", username='" + username + '\'' +
            ", birthYear=" + birthYear +
            ", gender=" + gender +
            '}';
    }
}
