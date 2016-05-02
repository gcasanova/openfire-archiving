package com.i7.openfire.archive.model;

public class Conversation {

	private String id;
	private long createdAt;
	private long updatedAt;
	private int messageCount;
	private String participantOne;
	private String participantTwo;

	public Conversation(String id) {
		this.id = id;
	}

	public Conversation(String id, String participantOne, String participantTwo, long createdAt, long updatedAt) {
		this.id = id;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.participantOne = participantOne;
		this.participantTwo = participantTwo;
	}

	public String getId() {
		return id;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(long createdAt) {
		this.createdAt = createdAt;
	}

	public long getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(long updatedAt) {
		this.updatedAt = updatedAt;
	}

	public int getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(int messageCount) {
		this.messageCount = messageCount;
	}

	public String getParticipantOne() {
		return participantOne;
	}

	public void setParticipantOne(String participantOne) {
		this.participantOne = participantOne;
	}

	public String getParticipantTwo() {
		return participantTwo;
	}

	public void setParticipantTwo(String participantTwo) {
		this.participantTwo = participantTwo;
	}

	public synchronized void messageReceived(long updatedAt) {
		this.updatedAt = updatedAt;
		messageCount++;
	}
}
