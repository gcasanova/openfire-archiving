package com.i7.openfire.archive.model.builders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import com.i7.openfire.archive.ConversationManager;
import com.i7.openfire.archive.enums.MessageStatus;
import com.i7.openfire.archive.model.ArchivedMessage;

public class ArchivedMessageBuilder {
	private static final Logger log = LoggerFactory.getLogger(ConversationManager.class);

	private JID to;
	private JID from;
	private String body;
	private MessageStatus status;

	private String id;
	private long createdAt;
	private long updatedAt;
	private String conversationID;

	public ArchivedMessageBuilder id(String id) {
		this.id = id;
		return this;
	}

	public ArchivedMessageBuilder to(JID to) {
		this.to = to;
		return this;
	}

	public ArchivedMessageBuilder from(JID from) {
		this.from = from;
		return this;
	}

	public ArchivedMessageBuilder body(String body) {
		this.body = body;
		return this;
	}

	public ArchivedMessageBuilder status(MessageStatus status) {
		this.status = status;
		return this;
	}

	public ArchivedMessageBuilder createdAt(long createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public ArchivedMessageBuilder updatedAt(long updatedAt) {
		this.updatedAt = updatedAt;
		return this;
	}

	public ArchivedMessageBuilder conversationID(String conversationID) {
		this.conversationID = conversationID;
		return this;
	}

	public ArchivedMessage build() {
		if ((id != null && id.length() > 0) && (conversationID != null && conversationID.length() > 0)
				&& (body != null && body.length() > 0) && from != null && to != null && status != null && createdAt > 0
				&& updatedAt > 0) {

			return new ArchivedMessage(id, to, from, body, createdAt, updatedAt, conversationID, status);
		}

		log.error(
				"ArchivedMessage cannot be built. id: %s, conversationID: %s, from: %s, to: %s, status: %s, createdAt: %d, updatedAt: %d, body: %s",
				id, conversationID, from.toString(), to.toString(), status.toString(), createdAt, updatedAt, body);
		return null;
	}
}
