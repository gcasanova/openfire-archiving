package com.i7.openfire.archive;

import java.util.Date;
import java.util.List;

import com.i7.openfire.archive.model.ArchivedMessage;
import com.i7.openfire.archive.xep.xep0059.XmppResultSet;

/**
 * Manages database persistence.
 */
public interface PersistenceManager {

	/**
	 * Searches for the last messages of last updated conversations.
	 */
	List<ArchivedMessage> getConversationsLastMessage(String participant, Date start, Date end);

	/**
	 * Searches for conversation by participants.
	 */
	String getConversation(String participantOne, String participantTwo);

	/**
	 * Searches for messages by conversation id.
	 */
	List<ArchivedMessage> getMessages(String conversationId, Date startDate, Date endDate, XmppResultSet xmppResultSet);
}
