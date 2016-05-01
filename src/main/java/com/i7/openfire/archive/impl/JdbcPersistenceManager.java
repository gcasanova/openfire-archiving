package com.i7.openfire.archive.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jivesoftware.database.CachedPreparedStatement;
import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import com.i7.openfire.archive.ConversationManager;
import com.i7.openfire.archive.PersistenceManager;
import com.i7.openfire.archive.config.Properties;
import com.i7.openfire.archive.database.Queries;
import com.i7.openfire.archive.enums.MessageStatus;
import com.i7.openfire.archive.model.ArchivedMessage;
import com.i7.openfire.archive.xep.xep0059.XmppResultSet;

/**
 * Manages database persistence.
 */
public class JdbcPersistenceManager implements PersistenceManager {
	private static final Logger log = LoggerFactory.getLogger(ConversationManager.class);

	public static final int CONVERSATIONS_PAGE_LIMIT = 15;

	@Override
	public List<ArchivedMessage> getConversationsLastMessage(String participant, Date start, Date end) {
		CachedPreparedStatement cachedPstmt = new CachedPreparedStatement();
		cachedPstmt.setSQL(Queries.SEARCH_MESSAGES_LAST);
		cachedPstmt.addString(participant);
		cachedPstmt.addString(participant);
		cachedPstmt.addLong(start.getTime());
		cachedPstmt.addLong(end.getTime());
		cachedPstmt.addInt(CONVERSATIONS_PAGE_LIMIT);

		List<ArchivedMessage> messages = new ArrayList<>();

		// Get all matching messages from the database.
		Connection con = null;
		PreparedStatement pstmt = null;
		try {
			con = DbConnectionManager.getConnection();
			pstmt = DbConnectionManager.createScrollablePreparedStatement(con, cachedPstmt.getSQL());
			cachedPstmt.setParams(pstmt);

			ResultSet rs = pstmt.executeQuery();

			// Keep reading results until the result set is exhausted or
			// we come to the end of the block.
			int count = 0;
			ArchivedMessage message;
			while (rs.next() && count < CONVERSATIONS_PAGE_LIMIT) {
				message = ArchivedMessage.builder().id(rs.getString(1)).conversationID(rs.getString(2))
						.from(new JID(rs.getString(3))).to(new JID(rs.getString(4)))
						.status(MessageStatus.findByValue(rs.getInt(5))).createdAt(rs.getLong(6))
						.updatedAt(rs.getLong(7)).body(rs.getString(8)).build();

				if (message != null)
					messages.add(message);
			}
			rs.close();
		} catch (SQLException sqle) {
			log.error(sqle.getMessage(), sqle);
		} finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}
		return messages;
	}

	@Override
	public String getConversation(String participantOne, String participantTwo) {
		CachedPreparedStatement cachedPstmt = new CachedPreparedStatement();
		cachedPstmt.setSQL(Queries.SEARCH_CONVERSATION);
		cachedPstmt.addString(participantOne);
		cachedPstmt.addString(participantTwo);
		cachedPstmt.addString(participantTwo);
		cachedPstmt.addString(participantOne);

		Connection con = null;
		String conversationId = null;
		PreparedStatement pstmt = null;
		try {
			con = DbConnectionManager.getConnection();
			pstmt = DbConnectionManager.createScrollablePreparedStatement(con, cachedPstmt.getSQL());
			cachedPstmt.setParams(pstmt);

			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				conversationId = rs.getString(1);
			} else {
				log.debug("Conversation not found for participants: %s, %s", participantOne, participantTwo);
			}
			rs.close();
		} catch (SQLException sqle) {
			log.error(sqle.getMessage(), sqle);
		} finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}
		return conversationId;
	}

	@Override
	public List<ArchivedMessage> getMessages(String conversationId, Date startDate, Date endDate, XmppResultSet xmppResultSet) {
		int limit = 0, offset = 0;
		List<ArchivedMessage> messages = new ArrayList<>();
		
		if (conversationId != null) {
			if (xmppResultSet != null) {
				limit = xmppResultSet.getMax() != null ? xmppResultSet.getMax() : Properties.getInstance().getMaxMessages();
				int count = countMessages(conversationId, startDate, endDate);
				boolean reverse = false;

				xmppResultSet.setCount(count);
				if (xmppResultSet.getIndex() != null) {
					offset = xmppResultSet.getIndex();
				} else if (xmppResultSet.getAfter() != null) {
					offset = countMessagesBefore(conversationId, startDate, endDate, xmppResultSet.getAfter());
					offset += 1;
				} else if (xmppResultSet.getBefore() != null) {
					int messagesBeforeCount = countMessagesBefore(conversationId, startDate, endDate,
							xmppResultSet.getBefore());
					offset = messagesBeforeCount;
					offset -= limit;

					// Reduce result limit to number of results before (if less than
					// a page remaining)
					if (messagesBeforeCount < limit) {
						limit = messagesBeforeCount;
					}

					reverse = true;
					if (offset < 0) {
						offset = 0;
					}
				}
				xmppResultSet.setFirstIndex(offset);

				if (isLastPage(offset, count, limit, reverse)) {
					xmppResultSet.setComplete(true);
				}
			}

			CachedPreparedStatement cachedPstmt = new CachedPreparedStatement();
			cachedPstmt.setSQL(Queries.SEARCH_MESSAGES);
			cachedPstmt.addString(conversationId);
			cachedPstmt.addInt(limit);
			cachedPstmt.addInt(offset);

			// Get all matching messages from the database.
			Connection con = null;
			PreparedStatement pstmt = null;
			try {
				con = DbConnectionManager.getConnection();
				pstmt = DbConnectionManager.createScrollablePreparedStatement(con, cachedPstmt.getSQL());
				cachedPstmt.setParams(pstmt);

				ResultSet rs = pstmt.executeQuery();

				// Keep reading results until the result set is exhausted or
				// we come to the end of the block.
				int count = 0;
				ArchivedMessage message;
				while (rs.next() && count < limit) {
					message = ArchivedMessage.builder().id(rs.getString(1)).conversationID(rs.getString(2))
							.from(new JID(rs.getString(3))).to(new JID(rs.getString(4)))
							.status(MessageStatus.findByValue(rs.getInt(5))).createdAt(rs.getLong(6))
							.updatedAt(rs.getLong(7)).body(rs.getString(8)).build();

					if (message != null)
						messages.add(message);
				}
				rs.close();
			} catch (SQLException sqle) {
				log.error(sqle.getMessage(), sqle);
			} finally {
				DbConnectionManager.closeConnection(pstmt, con);
			}
		}
		return messages;
	}

	private Integer countMessages(String conversationId, Date startDate, Date endDate) {
		StringBuilder querySB = new StringBuilder(Queries.COUNT_MESSAGES);

		if (startDate != null) {
			querySB.append(" AND createdAt >= ?");
		}
		if (endDate != null) {
			querySB.append(" AND createdAt <= ?");
		}

		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con = DbConnectionManager.getConnection();
			pstmt = con.prepareStatement(querySB.toString());
			bindMessageParameters(conversationId, startDate, endDate, pstmt);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				return 0;
			}
		} catch (SQLException sqle) {
			log.error("Error counting conversations", sqle);
			return 0;
		} finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
	}

	private Integer countMessagesBefore(String conversationId, Date startDate, Date endDate, Long before) {
		StringBuilder querySB = new StringBuilder(Queries.COUNT_MESSAGES);

		if (startDate != null) {
			querySB.append(" AND createdAt >= ?");
		}
		if (endDate != null) {
			querySB.append(" AND createdAt <= ?");
		}
		querySB.append(" AND createdAt < ?");

		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			int parameterIndex;
			con = DbConnectionManager.getConnection();
			pstmt = con.prepareStatement(querySB.toString());
			parameterIndex = bindMessageParameters(conversationId, startDate, endDate, pstmt);
			pstmt.setLong(parameterIndex, before);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				return 0;
			}
		} catch (SQLException sqle) {
			log.error("Error counting conversations", sqle);
			return 0;
		} finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
	}

	private int bindMessageParameters(String conversationId, Date startDate, Date endDate, PreparedStatement pstmt)
			throws SQLException {

		int parameterIndex = 1;

		pstmt.setString(parameterIndex++, conversationId);
		if (startDate != null) {
			pstmt.setLong(parameterIndex++, dateToMillis(startDate));
		}
		if (endDate != null) {
			pstmt.setLong(parameterIndex++, dateToMillis(endDate));
		}
		return parameterIndex;
	}

	private Long dateToMillis(Date date) {
		return date == null ? null : date.getTime();
	}

	/**
	 * Determines whether a result page is the last of a set.
	 *
	 * @param firstItemIndex
	 *            index (in whole set) of first item in page.
	 * @param resultCount
	 *            total number of results in set.
	 * @param pageSize
	 *            number of results in a page.
	 * @param reverse
	 *            whether paging is being performed in reverse (back to front)
	 * @return whether results are from last page.
	 */
	private boolean isLastPage(int firstItemIndex, int resultCount, int pageSize, boolean reverse) {
		if (reverse) {
			// Index of first item in last page always 0 when reverse
			if (firstItemIndex == 0) {
				return true;
			}
		} else {
			if ((firstItemIndex + pageSize) >= resultCount) {
				return true;
			}
		}

		return false;
	}
}
