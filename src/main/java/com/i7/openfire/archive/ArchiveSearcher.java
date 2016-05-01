package com.i7.openfire.archive;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.jivesoftware.database.CachedPreparedStatement;
import org.jivesoftware.database.DbConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.i7.openfire.archive.database.Queries;
import com.i7.openfire.archive.model.Conversation;

/**
 * Searches archived conversations. If conversation archiving is not enabled,
 * this class does nothing.
 */
public class ArchiveSearcher implements Startable {
	private static final Logger Log = LoggerFactory.getLogger(ArchiveSearch.class);

	private ConversationManager conversationManager;

	public ArchiveSearcher(ConversationManager conversationManager) {
		this.conversationManager = conversationManager;
	}

	@Override
	public void start() {
		// do nothing for now
	}

	@Override
	public void stop() {
		conversationManager = null;
	}

	public Collection<Conversation> search(ArchiveSearch search) {
		CachedPreparedStatement cachedPstmt = new CachedPreparedStatement();
		int numResults = search.getNumResults();

		// Set the database query string.
		if (search.getParticipants().size() == 1) {
			cachedPstmt.setSQL(Queries.SEARCH_CONVERSATIONS);

			int startIndex = search.getStartIndex();

			String jid = search.getParticipants().get(0).toString();
			cachedPstmt.addString(jid);
			cachedPstmt.addString(jid);
			cachedPstmt.addInt(startIndex);
			cachedPstmt.addInt(numResults);

		} else if (search.getParticipants().size() == 2) {
			cachedPstmt.setSQL(Queries.SEARCH_CONVERSATION);

			String jid1 = search.getParticipants().get(0).toString();
			String jid2 = search.getParticipants().get(1).toString();

			cachedPstmt.addString(jid1);
			cachedPstmt.addString(jid2);
			cachedPstmt.addString(jid2);
			cachedPstmt.addString(jid1);
		}

		List<String> conversationIDs = new ArrayList<>();

		// Get all matching conversations from the database.
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
			while (rs.next() && count < numResults) {
				conversationIDs.add(rs.getString(1));
				count++;
			}
			rs.close();
		} catch (SQLException sqle) {
			Log.error(sqle.getMessage(), sqle);
		} finally {
			DbConnectionManager.closeConnection(pstmt, con);
		}
		return new DatabaseQueryResults(conversationIDs);
	}

	/**
	 * Returns Hits from a database search against archived conversations as a
	 * Collection of Conversation objects.
	 */
	private class DatabaseQueryResults extends AbstractCollection<Conversation> {

		private List<String> conversationIDs;

		/**
		 * Constructs a new query results object.
		 *
		 * @param conversationIDs
		 *            the list of conversation IDs.
		 */
		public DatabaseQueryResults(List<String> conversationIDs) {
			this.conversationIDs = conversationIDs;
		}

		@Override
		public Iterator<Conversation> iterator() {
			final Iterator<String> convIterator = conversationIDs.iterator();
			return new Iterator<Conversation>() {

				private Conversation nextElement = null;

				@Override
				public boolean hasNext() {
					if (nextElement == null) {
						nextElement = getNextElement();
						if (nextElement == null) {
							return false;
						}
					}
					return true;
				}

				@Override
				public Conversation next() {
					Conversation element;
					if (nextElement != null) {
						element = nextElement;
						nextElement = null;
					} else {
						element = getNextElement();
						if (element == null) {
							throw new NoSuchElementException();
						}
					}
					return element;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				private Conversation getNextElement() {
					if (!convIterator.hasNext()) {
						return null;
					}
					while (convIterator.hasNext()) {
						try {
							String conversationID = convIterator.next();
							return conversationManager.getConversation(conversationID);
						} catch (Exception e) {
							Log.error(e.getMessage(), e);
						}
					}
					return null;
				}
			};
		}

		@Override
		public int size() {
			return conversationIDs.size();
		}
	}
}
