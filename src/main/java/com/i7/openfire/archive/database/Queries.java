package com.i7.openfire.archive.database;

public class Queries {

	public static final String INSERT_CONVERSATION = "INSERT INTO ofConversation(id, participantOneJID, participantTwoJID, createdAt, updatedAt, messageCount) VALUES (?,?,?,?,?,0)";
	public static final String INSERT_MESSAGE = "INSERT INTO ofMessage(id, conversationID, fromJID, toJID, statusCode, body, createdAt, updatedAt) VALUES (?,?,?,?,?,?,?,?)";

	public static final String LOAD_CONVERSATION = "SELECT participantOneJID, participantTwoJID, createdAt, updatedAt, messageCount FROM ofConversation WHERE id=?";
	public static final String LOAD_MESSAGE = "SELECT fromJID, toJID, statusCode, createdAt, updatedAt, body FROM ofMessage WHERE id=?";

	public static final String UPDATE_CONVERSATION = "UPDATE ofConversation SET updatedAt=?, messageCount=? WHERE id=?";
	public static final String UPDATE_MESSAGE = "UPDATE ofMessage SET statusCode=?, updatedAt=? WHERE id=?";

	public static final String CONVERSATION_COUNT = "SELECT COUNT(*) FROM ofConversation";
	public static final String MESSAGE_COUNT = "SELECT COUNT(*) FROM ofMessage";

	public static final String DELETE_CONVERSATION = "DELETE FROM ofMessage WHERE conversationID=?";
	public static final String DELETE_CONVERSATION_2 = "DELETE FROM ofConversation WHERE id=?";
	
	public static final String SEARCH_CONVERSATIONS = "SELECT id FROM ofConversation WHERE participantOneJID=? OR participantTwoJID=? ORDER BY updatedAt LIMIT ?, ?";
	public static final String SEARCH_CONVERSATION = "SELECT id FROM ofConversation WHERE (participantOneJID=? AND participantTwoJID=?) OR (participantOneJID=? AND participantTwoJID=?)";

	public static final String SEARCH_MESSAGES = "SELECT id, conversationID, fromJID, toJID, statusCode, createdAt, updatedAt, body FROM ofMessage WHERE conversationID=? ORDER BY createdAt LIMIT ?, ?";
	public static final String SEARCH_MESSAGES_LAST = "SELECT id, conversationID, fromJID, toJID, statusCode, createdAt, updatedAt, body FROM ofMessage WHERE (fromJID=? OR toJID=?) AND createdAt > ? AND createdAt <= ? GROUP BY conversationID ORDER BY createdAt LIMIT ?";

	public static final String COUNT_MESSAGES = "SELECT COUNT FROM ofMessage WHERE conversationID=?";
}
