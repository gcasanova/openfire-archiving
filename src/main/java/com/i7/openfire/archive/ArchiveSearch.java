package com.i7.openfire.archive;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.xmpp.packet.JID;

import com.google.common.collect.Lists;

/**
 * Defines a search query for use with an {@link ArchiveSearcher}
 */
public class ArchiveSearch {

	/**
	 * An integer value that represents NULL. The actual value is
	 * Integer.MAX_VALUE - 123 (an arbitrary number that has a very low
	 * probability of actually being selected by a user as a valid value).
	 */
	public static final int NULL_INT = Integer.MAX_VALUE - 123;

	private List<JID> participants = Lists.newArrayList();

	/**
	 * Start of conversation has to be bigger or equal to this value (if set)
	 */
	private long timestampMin;
	/**
	 * Start of conversation has to be smaller or equal to this value (if set)
	 */
	private long timestampMax;

	/**
	 * Specified timestamp has to be between created and updated values
	 */
	private long includeTimestamp;
	private int startIndex = 0;
	private int numResults = 15;
	private SortOrder sortOrder;

	/**
	 * Constructs a new archive search, sorted on date descending.
	 */
	public ArchiveSearch() {
		this.sortOrder = SortOrder.descending;
	}

	/**
	 * Returns the participants that this search covers. If no participants are
	 * specified (via an empty collection), then this search will match against
	 * both users. If a single participant is specified, this search will match
	 * against the other participant.
	 *
	 * @return the participants that this search covers.
	 */
	public List<JID> getParticipants() {
		return participants;
	}

	/**
	 * Sets the participants that this search covers. If no participants are
	 * specified then this search will match against both users. If a single
	 * participant is specified, this search will match against the other
	 * participant.
	 *
	 * @param participants
	 *            the participants that this search covers.
	 */
	public void setParticipants(JID... participants) {
		if (participants == null) {
			this.participants = Collections.emptyList();
		} else {
			if (participants.length > 2) {
				throw new IllegalArgumentException("Not possible to search on more than " + "two participants.");
			}
			// Enforce using the bare JID.
			for (int i = 0; i < participants.length; i++) {
				participants[i] = new JID(participants[i].toBareJID());
			}
			this.participants = Arrays.asList(participants);
		}
	}

	/**
	 * Sets the participants that this search covers. If no participants are
	 * specified then this search will match against both users. If a single
	 * participant is specified, this search will match against the other
	 * participant.
	 *
	 * @param participants
	 *            the participants that this search covers.
	 */
	public void setParticipants(String... participants) {
		if (participants == null) {
			this.participants = Collections.emptyList();
		} else {
			if (participants.length > 2) {
				throw new IllegalArgumentException("Not possible to search on more than " + "two participants.");
			}
			this.participants = Arrays.asList(participants).stream().map(JID::new).collect(Collectors.toList());
		}
	}

	/**
	 * Returns the lower boundary for conversations that will be returned by the
	 * search. If this value has not been set, the method will return
	 * <tt>null</tt>.
	 *
	 * @return a long value (timestamp) representing the lower bound to search
	 *         on or <tt>null</tt> if there is no lower bound.
	 */
	public long getTimestampMin() {
		return timestampMin;
	}

	/**
	 * Sets the lower boundary for conversations that will be returned by the
	 * search. A value of <tt>null</tt> indicates that there should be no lower
	 * boundary.
	 *
	 * @param timestampMin
	 *            a long value (timestamp) representing the lower bound to
	 *            search on or <tt>null</tt> if there is no lower bound.
	 */
	public void setTimestampMin(long timestampMin) {
		this.timestampMin = timestampMin;
	}

	/**
	 * Returns the the upper boundary for conversations that will be returned by
	 * the search. If this value has not been set, the method will return
	 * <tt>null</tt>.
	 *
	 * @return a long value (timestamp) representing the upper bound to search
	 *         on or <tt>null</tt> if there is no upper bound.
	 */
	public long getTimestampMax() {
		return timestampMax;
	}

	/**
	 * Sets the upper boundary for conversations that will be returned by the
	 * search. A value of <tt>null</tt> indicates that there should be no upper
	 * boundary.
	 *
	 * @param timestampMax
	 *            a long value (timestamp) representing the upper bound to
	 *            search on or <tt>null</tt> if there is no upper bound.
	 */
	public void setTimestampMax(long timestampMax) {
		this.timestampMax = timestampMax;
	}

	/**
	 * Returns the timestamp to use for filtering conversations. This timestamp
	 * has to be between the time when the conversation was created and was last
	 * updated.
	 *
	 * @return timestamp between the time when the conversation started and was
	 *         last updated.
	 */
	public long getIncludeTimestamp() {
		return includeTimestamp;
	}

	/**
	 * Set the timestamp to use for filtering conversations. This timestamp has
	 * to be between the time when the conversation started and was last
	 * updated.
	 *
	 * @param includeTimestamp
	 *            timestamp between the time when the conversation started and
	 *            was last updated.
	 */
	public void setIncludeTimestamp(long includeTimestamp) {
		this.includeTimestamp = includeTimestamp;
	}

	/**
	 * Returns the sort order, which will be {@link SortOrder#ascending
	 * ascending} or {@link SortOrder#descending descending}.
	 *
	 * @return the sort order.
	 */
	public SortOrder getSortOrder() {
		return this.sortOrder;
	}

	/**
	 * Sets the sort type, which will be {@link SortOrder#ascending ascending}
	 * or {@link SortOrder#descending descending}.
	 *
	 * @param sortOrder
	 *            the order that results will be sorted in.
	 */
	public void setSortOrder(SortOrder sortOrder) {
		this.sortOrder = sortOrder;
	}

	/**
	 * Returns the max number of results that should be returned. The default
	 * value for is NULL_INT, which means there will be no limit on the number
	 * of results. This method can be used in combination with
	 * setStartIndex(int) to perform pagination of results.
	 *
	 * @return the max number of results to return.
	 * @see #setStartIndex(int)
	 */
	public int getNumResults() {
		return numResults;
	}

	/**
	 * Sets the limit on the number of results to be returned.
	 *
	 * @param numResults
	 *            the number of results to return.
	 */
	public void setNumResults(int numResults) {
		if (numResults < 0) {
			throw new IllegalArgumentException("numResults cannot be less than 0.");
		}
		this.numResults = numResults;
	}

	/**
	 * Returns the index of the first result to return.
	 *
	 * @return the index of the first result which should be returned.
	 */
	public int getStartIndex() {
		return startIndex;
	}

	/**
	 * Sets the index of the first result to return. For example, if the start
	 * index is set to 20, the Iterator returned will start at the 20th result
	 * in the query. This method can be used in combination with
	 * setNumResults(int) to perform pagination of results.
	 *
	 * @param startIndex
	 *            the index of the first result to return.
	 */
	public void setStartIndex(int startIndex) {
		if (startIndex < 0) {
			throw new IllegalArgumentException("A start index less than 0 is not valid.");
		}
		this.startIndex = startIndex;
	}

	/**
	 * The sort order of search results. The default sort order is descending.
	 * Note that if if the sort field is {@link SortField#relevance} (for a
	 * query string search), then the sort order is irrelevant. Relevance
	 * searches will always display the most relevant results first.
	 */
	public enum SortOrder {
		ascending, descending
	}
}
