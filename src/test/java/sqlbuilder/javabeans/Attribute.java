package sqlbuilder.javabeans;

/**
 * @author Laurent Van der Linden
 */
public class Attribute {
    public final static String TABLE = "attributes";

    private Long id;
    private Long fileid;
    private String name;
    private String value;

    public Attribute() {
    }

    public Attribute(Long fileid, String name, String value) {
        this.fileid = fileid;
        this.name = name;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getFileid() {
        return fileid;
    }

    public void setFileid(Long fileid) {
        this.fileid = fileid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (id != null ? !id.equals(attribute.id) : attribute.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Attribute{" +
            "id=" + id +
            ", fileid=" + fileid +
            ", name='" + name + '\'' +
            ", value='" + value + '\'' +
            '}';
    }
}
