package sqlbuilder.beans;

import java.util.Set;

import sqlbuilder.meta.Column;
import sqlbuilder.meta.Id;
import sqlbuilder.meta.Table;

/**
 * @author Laurent Van der Linden
 */
@Table(name = "public.users")
public class User {
    @Id @Column(name = "id")
    private Long id;
    private String username;
    private int birthYear;
    @Column(name = "sex")
    private char gender;
    private Set<File> files;
    private Long parent_id;
    private User parent;
    private Boolean superUser;
    private boolean active;

    public User() {
    }

    public User(String username, int birthYear, char gender, Long parent_id, Boolean superUser, Boolean active) {
        this.username = username;
        this.birthYear = birthYear;
        this.gender = gender;
        this.parent_id = parent_id;
        this.superUser = superUser;
        this.active = active;
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

    public Long getParent_id() {
        return parent_id;
    }

    public void setParent_id(Long parent_id) {
        this.parent_id = parent_id;
    }

    public User getParent() {
        return parent;
    }

    public void setParent(User parent) {
        this.parent = parent;
    }

    public Boolean isSuperUser() {
        return superUser;
    }

    public void setSuperUser(Boolean superUser) {
        this.superUser = superUser;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
