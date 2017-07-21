package com.github.syuchan1005.hanashiaite;

import java.util.List;

/**
 * Created by syuchan on 2017/01/11.
 */
public class CommitModel {
	private String commitType;
	private String id;
	private String displayId = "";
	private Author author;
	private long authorTimestamp;
	private String message;
	private List<Parent> parents = null;
	private Properties properties;

	public String getCommitType() {
		return commitType;
	}

	public void setCommitType(String commitType) {
		this.commitType = commitType;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDisplayId() {
		return displayId;
	}

	public void setDisplayId(String displayId) {
		this.displayId = displayId;
	}

	public Author getAuthor() {
		return author;
	}

	public void setAuthor(Author author) {
		this.author = author;
	}

	public long getAuthorTimestamp() {
		return authorTimestamp;
	}

	public void setAuthorTimestamp(long authorTimestamp) {
		this.authorTimestamp = authorTimestamp;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<Parent> getParents() {
		return parents;
	}

	public void setParents(List<Parent> parents) {
		this.parents = parents;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	static class Author {
		private String name;
		private String emailAddress;
		private String avatarUrl;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getEmailAddress() {
			return emailAddress;
		}

		public void setEmailAddress(String emailAddress) {
			this.emailAddress = emailAddress;
		}

		public String getAvatarUrl() {
			return avatarUrl;
		}

		public void setAvatarUrl(String avatarUrl) {
			this.avatarUrl = avatarUrl;
		}
	}

	static class Parent {
		private String id;
		private String displayId;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getDisplayId() {
			return displayId;
		}

		public void setDisplayId(String displayId) {
			this.displayId = displayId;
		}

	}

	static class Properties {
		private List<String> jiraKey = null;

		public List<String> getJiraKey() {
			return jiraKey;
		}

		public void setJiraKey(List<String> jiraKey) {
			this.jiraKey = jiraKey;
		}

	}
}
