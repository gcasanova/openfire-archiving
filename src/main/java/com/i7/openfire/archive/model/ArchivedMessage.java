package com.i7.openfire.archive.model;

import org.xmpp.packet.JID;

import com.i7.openfire.archive.enums.MessageStatus;
import com.i7.openfire.archive.model.builders.ArchivedMessageBuilder;

public class ArchivedMessage {

	private JID to;
	private JID from;
	private String body;
	private MessageStatus status;

	private String id;
	private long createdAt;
	private long updatedAt;
	private String conversationID;

	public ArchivedMessage(String id, JID to, JID from, String body, long createdAt, long updatedAt,
			String conversationID, MessageStatus status) {

		this.id = id;
		this.to = to;
		this.from = from;
		this.body = body;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.conversationID = conversationID;
	}

	public String getId() {
		return id;
	}

	public JID getTo() {
		return to;
	}

	public JID getFrom() {
		return from;
	}

	public String getBody() {
		return body;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public long getUpdatedAt() {
		return updatedAt;
	}

	public String getConversationID() {
		return conversationID;
	}

	public MessageStatus getStatus() {
		return status;
	}

	public static ArchivedMessageBuilder builder() {
		return new ArchivedMessageBuilder();
	}
}
