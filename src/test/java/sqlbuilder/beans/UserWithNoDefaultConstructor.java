package sqlbuilder.beans;

import sqlbuilder.meta.Table;

@Table(name = "users")
public class UserWithNoDefaultConstructor {
	private Long id;

	public UserWithNoDefaultConstructor(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
