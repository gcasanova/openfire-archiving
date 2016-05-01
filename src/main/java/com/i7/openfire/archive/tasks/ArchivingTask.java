package com.i7.openfire.archive.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Queue;

import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.i7.openfire.archive.database.Queries;
import com.i7.openfire.archive.model.ArchivedMessage;
import com.i7.openfire.archive.model.Conversation;

/**
 * A task that persists conversation meta-data and messages to the database.
 */
public class ArchivingTask implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(ArchivingTask.class);

	private static boolean archivingRunning = false;

	private Queue<ArchivedMessage> messageQueue;
	private Queue<Conversation> conversationQueue;
	private Queue<ArchivedMessage> messageUpdatedQueue;
	private Queue<Conversation> conversationUpdatedQueue;

	public ArchivingTask(Queue<ArchivedMessage> messageQueue, Queue<Conversation> conversationQueue,
			Queue<ArchivedMessage> messageUpdatedQueue, Queue<Conversation> conversationUpdatedQueue) {

		this.messageQueue = messageQueue;
		this.conversationQueue = conversationQueue;
		this.messageUpdatedQueue = messageUpdatedQueue;
		this.conversationUpdatedQueue = conversationUpdatedQueue;
	}

	@Override
	public void run() {
		synchronized (this) {
			if (archivingRunning) {
				return;
			}
			archivingRunning = true;
		}

		if (!messageQueue.isEmpty() || !messageUpdatedQueue.isEmpty() || !conversationUpdatedQueue.isEmpty()) {
			Connection con = null;
			PreparedStatement pstmt = null;
			try {
				con = DbConnectionManager.getConnection();

				int count = 0;
				Conversation conversation;
				pstmt = con.prepareStatement(Queries.INSERT_CONVERSATION);
				while ((conversation = conversationQueue.poll()) != null) {
					pstmt.setString(1, conversation.getId());
					pstmt.setString(2, conversation.getParticipantOne());
					pstmt.setString(3, conversation.getParticipantTwo());
					pstmt.setLong(4, conversation.getCreatedAt());
					pstmt.setLong(5, conversation.getUpdatedAt());
					pstmt.setInt(6, conversation.getMessageCount());
					if (DbConnectionManager.isBatchUpdatesSupported()) {
						pstmt.addBatch();
					} else {
						pstmt.execute();
					}
					// Only batch up to 500 items at a time.
					if (count % 500 == 0 && DbConnectionManager.isBatchUpdatesSupported()) {
						pstmt.executeBatch();
					}
					count++;
				}
				if (DbConnectionManager.isBatchUpdatesSupported()) {
					pstmt.executeBatch();
				}
				DbConnectionManager.closeStatement(pstmt);

				count = 0;
				ArchivedMessage message;
				pstmt = con.prepareStatement(Queries.INSERT_MESSAGE);
				while ((message = messageQueue.poll()) != null) {
					pstmt.setString(1, message.getId());
					pstmt.setString(2, message.getConversationID());
					pstmt.setString(3, message.getFrom().toBareJID());
					pstmt.setString(4, message.getTo().toBareJID());
					pstmt.setInt(5, message.getStatus().getValue());
					DbConnectionManager.setLargeTextField(pstmt, 6, message.getBody());
					pstmt.setString(6, message.getBody());
					pstmt.setLong(7, message.getCreatedAt());
					pstmt.setLong(8, message.getUpdatedAt());

					if (DbConnectionManager.isBatchUpdatesSupported()) {
						pstmt.addBatch();
					} else {
						pstmt.execute();
					}
					// Only batch up to 500 items at a time.
					if (count % 500 == 0 && DbConnectionManager.isBatchUpdatesSupported()) {
						pstmt.executeBatch();
					}
					count++;
				}
				if (DbConnectionManager.isBatchUpdatesSupported()) {
					pstmt.executeBatch();
				}
				DbConnectionManager.closeStatement(pstmt);

				count = 0;
				pstmt = con.prepareStatement(Queries.UPDATE_CONVERSATION);
				while ((conversation = conversationUpdatedQueue.poll()) != null) {
					pstmt.setLong(1, conversation.getUpdatedAt());
					pstmt.setInt(2, conversation.getMessageCount());
					pstmt.setString(3, conversation.getId());
					if (DbConnectionManager.isBatchUpdatesSupported()) {
						pstmt.addBatch();
					} else {
						pstmt.execute();
					}
					// Only batch up to 500 items at a time.
					if (count % 500 == 0 && DbConnectionManager.isBatchUpdatesSupported()) {
						pstmt.executeBatch();
					}
					count++;
				}
				if (DbConnectionManager.isBatchUpdatesSupported()) {
					pstmt.executeBatch();
				}
				DbConnectionManager.closeStatement(pstmt);

				count = 0;
				pstmt = con.prepareStatement(Queries.UPDATE_MESSAGE);
				while ((message = messageQueue.poll()) != null) {
					pstmt.setInt(1, message.getStatus().getValue());
					pstmt.setLong(2, message.getUpdatedAt());
					pstmt.setString(3, message.getId());

					if (DbConnectionManager.isBatchUpdatesSupported()) {
						pstmt.addBatch();
					} else {
						pstmt.execute();
					}
					// Only batch up to 500 items at a time.
					if (count % 500 == 0 && DbConnectionManager.isBatchUpdatesSupported()) {
						pstmt.executeBatch();
					}
					count++;
				}
				if (DbConnectionManager.isBatchUpdatesSupported()) {
					pstmt.executeBatch();
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			} finally {
				DbConnectionManager.closeConnection(pstmt, con);
			}
		}
		// Set archiving running back to false.
		archivingRunning = false;
	}
}
