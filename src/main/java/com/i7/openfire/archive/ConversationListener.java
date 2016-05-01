package com.i7.openfire.archive;

import com.i7.openfire.archive.model.Conversation;

public interface ConversationListener {

	public void conversationCreated(Conversation conversation);

	public void conversationUpdated(Conversation conversation, long timestamp);
}