package com.i7.openfire.archive;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jivesoftware.util.cache.ExternalizableUtil;
import org.xmpp.packet.JID;

import com.i7.openfire.archive.enums.MessageStatus;

/**
 * Conversation events are only used when running in a cluster as a way to send
 * to the senior cluster member information about a conversation that is taking
 * place in this cluster node.
 */
public class ConversationEvent implements Externalizable {

	private String body;
	private String messageId;

	private JID sender;
	private JID receiver;

	private long createdAt;
	private MessageStatus status;

	/**
	 * Do not use this constructor. It only exists for serialization purposes.
	 */
	private ConversationEvent() {
	}

	public void run(ConversationManager conversationManager) {
		if (MessageStatus.SENT.getValue() == status.getValue()) {
			conversationManager.processMessage(messageId, sender, receiver, body, createdAt);
		} else {
			conversationManager.processMessageUpdate(messageId, sender, receiver, status, createdAt);
		}
	}

	public static ConversationEvent chatMessageReceived(String messageId, JID sender, JID receiver, String body,
			MessageStatus status, long createdAt) {

		ConversationEvent event = new ConversationEvent();
		event.messageId = messageId;
		event.sender = sender;
		event.receiver = receiver;
		event.body = body;
		event.status = status;
		event.createdAt = createdAt;
		return event;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		ExternalizableUtil.getInstance().writeLong(out, createdAt);
		ExternalizableUtil.getInstance().writeInt(out, status.getValue());

		ExternalizableUtil.getInstance().writeBoolean(out, sender != null);
		if (sender != null) {
			ExternalizableUtil.getInstance().writeSerializable(out, sender);
		}
		ExternalizableUtil.getInstance().writeBoolean(out, receiver != null);
		if (receiver != null) {
			ExternalizableUtil.getInstance().writeSerializable(out, receiver);
		}
		ExternalizableUtil.getInstance().writeBoolean(out, body != null);
		if (body != null) {
			ExternalizableUtil.getInstance().writeSafeUTF(out, body);
		}
		ExternalizableUtil.getInstance().writeBoolean(out, messageId != null);
		if (messageId != null) {
			ExternalizableUtil.getInstance().writeSafeUTF(out, messageId);
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		createdAt = ExternalizableUtil.getInstance().readLong(in);
		status = MessageStatus.findByValue(ExternalizableUtil.getInstance().readInt(in));

		if (ExternalizableUtil.getInstance().readBoolean(in)) {
			sender = (JID) ExternalizableUtil.getInstance().readSerializable(in);
		}
		if (ExternalizableUtil.getInstance().readBoolean(in)) {
			receiver = (JID) ExternalizableUtil.getInstance().readSerializable(in);
		}
		if (ExternalizableUtil.getInstance().readBoolean(in)) {
			body = ExternalizableUtil.getInstance().readSafeUTF(in);
		}
		if (ExternalizableUtil.getInstance().readBoolean(in)) {
			messageId = ExternalizableUtil.getInstance().readSafeUTF(in);
		}
	}
}
