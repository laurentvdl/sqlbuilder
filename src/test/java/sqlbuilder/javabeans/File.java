package sqlbuilder.javabeans;

import java.util.Set;

import sqlbuilder.meta.Column;
import sqlbuilder.meta.Id;
import sqlbuilder.meta.Table;

/**
 * @author Laurent Van der Linden
 */
@Table(name = "files")
public class File {
    @Id @Column(name = "ID")
    private Long id;
    private Long userid;
    private String name;
    private Set<Attribute> attributes;

    public File() {
    }

    public File(Long userid, String name) {
        this.userid = userid;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserid() {
        return userid;
    }

    public void setUserid(Long userid) {
        this.userid = userid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<Attribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        File file = (File) o;

        if (id != null ? !id.equals(file.id) : file.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "File{" +
            "id=" + id +
            ", userid=" + userid +
            ", name='" + name + '\'' +
            '}';
    }
}
