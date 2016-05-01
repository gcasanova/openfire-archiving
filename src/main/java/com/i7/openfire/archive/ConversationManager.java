package com.i7.openfire.archive;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.XMPPServerInfo;
import org.jivesoftware.openfire.cluster.ClusterManager;
import org.jivesoftware.openfire.component.ComponentEventListener;
import org.jivesoftware.openfire.component.InternalComponentManager;
import org.jivesoftware.util.JiveConstants;
import org.jivesoftware.util.NotFoundException;
import org.jivesoftware.util.TaskEngine;
import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import com.i7.openfire.archive.cluster.GetConversationCountTask;
import com.i7.openfire.archive.cluster.GetConversationTask;
import com.i7.openfire.archive.cluster.GetConversationsTask;
import com.i7.openfire.archive.config.Properties;
import com.i7.openfire.archive.database.Queries;
import com.i7.openfire.archive.enums.MessageStatus;
import com.i7.openfire.archive.model.ArchivedMessage;
import com.i7.openfire.archive.model.Conversation;
import com.i7.openfire.archive.plugin.ArchivingPlugin;
import com.i7.openfire.archive.tasks.ArchivingTask;

public class ConversationManager implements ComponentEventListener, Startable {
	private static final Logger log = LoggerFactory.getLogger(ConversationManager.class);

	public static final String CONVERSATIONS_KEY = "conversations";

	private Properties properties;

	private TimerTask maxAgeTask;
	private TimerTask cleanupTask;
	private TimerTask archiveTask;
	
	private TaskEngine taskEngine;
	private List<String> gateways;
	private XMPPServerInfo serverInfo;
	private Queue<ArchivedMessage> messageQueue;
	private Queue<Conversation> conversationQueue;
	private Queue<ArchivedMessage> messageUpdatedQueue;
	private Queue<Conversation> conversationUpdatedQueue;
	private ConversationEventsQueue conversationEventsQueue;
	private Collection<ConversationListener> conversationListeners;
	private Map<String, Conversation> conversations = new ConcurrentHashMap<String, Conversation>();

	public ConversationManager(TaskEngine taskEngine) {
		this.taskEngine = taskEngine;
		this.properties = Properties.getInstance();
		this.gateways = new CopyOnWriteArrayList<String>();
		this.serverInfo = XMPPServer.getInstance().getServerInfo();
		this.conversationEventsQueue = new ConversationEventsQueue(taskEngine);
	}

	@Override
	public void start() {
		messageQueue = new ConcurrentLinkedQueue<>();
		conversationQueue = new ConcurrentLinkedQueue<>();
		messageUpdatedQueue = new ConcurrentLinkedQueue<>();
		conversationUpdatedQueue = new ConcurrentLinkedQueue<>();
		conversationListeners = new CopyOnWriteArraySet<>();

		// Schedule a task to do conversation archiving.
		archiveTask = new TimerTask() {
			@Override
			public void run() {
				new ArchivingTask(messageQueue, conversationQueue, messageUpdatedQueue, conversationUpdatedQueue).run();
			}
		};
		taskEngine.scheduleAtFixedRate(archiveTask, JiveConstants.MINUTE, JiveConstants.MINUTE);

		// Schedule a task to do conversation cleanup.
		cleanupTask = new TimerTask() {
			@Override
			public void run() {
				for (String key : conversations.keySet()) {
					Conversation conversation = conversations.get(key);
					long now = System.currentTimeMillis();
					if ((now - conversation.getUpdatedAt() > properties.getIdleTime())) {
						removeConversation(key);
					}
				}
			}
		};
		taskEngine.scheduleAtFixedRate(cleanupTask, JiveConstants.MINUTE * 5, JiveConstants.MINUTE * 5);

		// Schedule a task to do conversation purging.
		maxAgeTask = new TimerTask() {
			@Override
			public void run() {
				if (properties.getMaxAge() > 0) {
					// Delete conversations older than maxAge days
					Connection con = null;
					PreparedStatement pstmt1 = null;
					PreparedStatement pstmt2 = null;
					try {
						con = DbConnectionManager.getConnection();
						pstmt1 = con.prepareStatement(Queries.DELETE_CONVERSATION_1);
						pstmt2 = con.prepareStatement(Queries.DELETE_CONVERSATION_2);
						Date now = new Date();
						Date maxAgeDate = new Date(now.getTime() - properties.getMaxAge());
						ArchiveSearch search = new ArchiveSearch();
						search.setTimestampMax(maxAgeDate.getTime());
						ArchivingPlugin plugin = (ArchivingPlugin) XMPPServer.getInstance().getPluginManager()
								.getPlugin(ArchivingPlugin.NAME);

						ArchiveSearcher archiveSearcher = plugin.getArchiveSearcher();
						Collection<Conversation> conversations = archiveSearcher.search(search);
						int conversationDeleted = 0;
						for (Conversation conversation : conversations) {
							log.debug("Deleting: " + conversation.getId() + " created: " + conversation.getCreatedAt()
									+ " older than: " + maxAgeDate);
							pstmt1.setString(1, conversation.getId());
							pstmt1.execute();
							pstmt2.setString(1, conversation.getId());
							pstmt2.execute();
							conversationDeleted++;
						}
						if (conversationDeleted > 0) {
							log.info("Deleted " + conversationDeleted + " conversations with date older than: "
									+ maxAgeDate);
						}
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					} finally {
						DbConnectionManager.closeConnection(pstmt1, con);
						DbConnectionManager.closeConnection(pstmt2, con);
					}
				}
			}
		};
		taskEngine.scheduleAtFixedRate(maxAgeTask, JiveConstants.MINUTE, JiveConstants.MINUTE);
		InternalComponentManager.getInstance().addListener(this);
	}

	@Override
	public void stop() {
		archiveTask.cancel();
		archiveTask = null;

		// Archive anything remaining in the queue before quitting.
		new ArchivingTask(messageQueue, conversationQueue, messageUpdatedQueue, conversationUpdatedQueue).run();

		conversationQueue.clear();
		conversationQueue = null;

		conversationUpdatedQueue.clear();
		conversationUpdatedQueue = null;

		messageQueue.clear();
		messageQueue = null;

		messageUpdatedQueue.clear();
		messageUpdatedQueue = null;

		conversationListeners.clear();
		conversationListeners = null;

		serverInfo = null;
		InternalComponentManager.getInstance().removeListener(this);
	}

	public ConversationEventsQueue getConversationEventsQueue() {
		return conversationEventsQueue;
	}

	/**
	 * Returns the count of active conversations.
	 *
	 * @return the count of active conversations.
	 */
	public int getConversationCount() {
		if (ClusterManager.isSeniorClusterMember()) {
			return conversations.size();
		}
		return (Integer) CacheFactory.doSynchronousClusterTask(new GetConversationCountTask(),
				ClusterManager.getSeniorClusterMember().toByteArray());
	}

	/**
	 * Returns a conversation by ID.
	 *
	 * @param conversationID
	 *            the ID of the conversation.
	 * @return the conversation.
	 * @throws NotFoundException
	 *             if the conversation could not be found.
	 */
	public Conversation getConversation(String conversationId) throws NotFoundException {
		if (ClusterManager.isSeniorClusterMember()) {
			// Search through the currently active conversations.
			for (Conversation conversation : conversations.values()) {
				if (conversation.getId().equals(conversationId)) {
					return conversation;
				}
			}
			// It might be an archived conversation, attempt to load it.
			return loadFromDb(conversationId);
		} else {
			// Get this info from the senior cluster member when cluster mode
			Conversation conversation = (Conversation) CacheFactory.doSynchronousClusterTask(
					new GetConversationTask(conversationId), ClusterManager.getSeniorClusterMember().toByteArray());

			if (conversation == null) {
				throw new NotFoundException("Conversation not found: " + conversationId);
			}
			return conversation;
		}
	}

	/**
	 * Returns the set of active conversations.
	 *
	 * @return the active conversations.
	 */
	@SuppressWarnings("unchecked")
	public Collection<Conversation> getConversations() {
		if (ClusterManager.isSeniorClusterMember()) {
			List<Conversation> conversationList = new ArrayList<Conversation>(conversations.values());
			// Sort the conversations by creation date.
			Collections.sort(conversationList, new Comparator<Conversation>() {
				@Override
				public int compare(Conversation c1, Conversation c2) {
					long thisTime = c1.getCreatedAt();
					long anotherTime = c2.getCreatedAt();

					return (thisTime < anotherTime ? -1 : (thisTime == anotherTime ? 0 : 1));
				}
			});
			return conversationList;
		} else {
			// Get this info from the senior cluster member when running in a
			// cluster
			return (Collection<Conversation>) CacheFactory.doSynchronousClusterTask(new GetConversationsTask(),
					ClusterManager.getSeniorClusterMember().toByteArray());
		}
	}

	/**
	 * Returns the total number of conversations that have been archived to the
	 * database. The archived conversation may only be the meta-data, or it
	 * might include messages as well if message archiving is turned on.
	 *
	 * @return the total number of archived conversations.
	 */
	public int getArchivedConversationCount() {
		int conversationCount = 0;
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con = DbConnectionManager.getConnection();
			pstmt = con.prepareStatement(Queries.CONVERSATION_COUNT);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				conversationCount = rs.getInt(1);
			}
		} catch (SQLException sqle) {
			log.error(sqle.getMessage(), sqle);
		} finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
		return conversationCount;
	}

	/**
	 * Adds a conversation listener, which will be notified of newly created
	 * conversations, conversations ending, and updates to conversations.
	 *
	 * @param listener
	 *            the conversation listener.
	 */
	public void addConversationListener(ConversationListener listener) {
		conversationListeners.add(listener);
	}

	/**
	 * Removes a conversation listener.
	 *
	 * @param listener
	 *            the conversation listener.
	 */
	public void removeConversationListener(ConversationListener listener) {
		conversationListeners.remove(listener);
	}

	/**
	 * Processes an incoming message of a one-to-one chat. The message will
	 * be mapped to a conversation and then queued for storage if archiving is
	 * turned on.
	 */
	void processMessage(String messageId, JID sender, JID receiver, String body, long createdAt) {
		String conversationKey = getConversationKey(sender, receiver);
		synchronized (conversationKey.intern()) {
			Conversation conversation = conversations.get(conversationKey);
			if (conversation == null) {
				// Conversation not found among active ones, search in persistence
				ArchiveSearch search = new ArchiveSearch();
				search.setParticipants(sender, receiver);
				ArchivingPlugin plugin = (ArchivingPlugin) XMPPServer.getInstance().getPluginManager()
						.getPlugin(ArchivingPlugin.NAME);

				ArchiveSearcher archiveSearcher = plugin.getArchiveSearcher();
				Collection<Conversation> conversations = archiveSearcher.search(search);

				// Create a new conversation if necessary.
				if (!conversations.isEmpty() && conversations.iterator().hasNext()) {
					conversation = conversations.iterator().next();
				} else {
					// Make sure that the user joined the conversation before a
					// message was received
					conversation = new Conversation(conversationKey, sender.toBareJID(), receiver.toBareJID(),
							createdAt - 1, createdAt - 1);

					// Notify listeners of the newly created conversation.
					for (ConversationListener listener : conversationListeners) {
						listener.conversationCreated(conversation);
					}

					// save new conversation in db, queue if it fails
					try {
						insertIntoDb(conversation);
					} catch (SQLException e) {
						conversationQueue.add(conversation);
					}
				}
				this.conversations.put(conversationKey, conversation);
			}

			// Record the newly received message.
			conversation.messageReceived(createdAt);
			conversationUpdatedQueue.add(conversation);

			messageQueue.add(new ArchivedMessage(messageId, receiver, sender, body, createdAt, createdAt,
					conversation.getId(), MessageStatus.SENT));

			// Notify listeners of the conversation update.
			for (ConversationListener listener : conversationListeners) {
				listener.conversationUpdated(conversation, createdAt);
			}
		}
	}

	/**
	 * Processes an incoming message status update of a one-to-one chat.
	 */
	void processMessageUpdate(String messageId, JID sender, JID receiver, MessageStatus status, long updatedAt) {
		String conversationKey = getConversationKey(sender, receiver);
		synchronized (conversationKey.intern()) {
			Conversation conversation = conversations.get(conversationKey);
			if (conversation == null) {
				ArchiveSearch search = new ArchiveSearch();
				search.setParticipants(sender, receiver);
				ArchivingPlugin plugin = (ArchivingPlugin) XMPPServer.getInstance().getPluginManager()
						.getPlugin(ArchivingPlugin.NAME);

				ArchiveSearcher archiveSearcher = plugin.getArchiveSearcher();
				Collection<Conversation> conversations = archiveSearcher.search(search);

				if (conversations.isEmpty() || !conversations.iterator().hasNext()) {
					// Conversation not found!!!
					log.error("Conversation between %s and %s not found when message %s status update was received!",
							sender.toBareJID(), receiver.toBareJID(), messageId);
					return;
				}
				conversation = conversations.iterator().next();
				this.conversations.put(conversationKey, conversation);
			}
			
			messageUpdatedQueue.add(new ArchivedMessage(messageId, receiver, sender, "", updatedAt, updatedAt,
					conversation.getId(), status));
		}
	}

	private void removeConversation(String key) {
		conversations.remove(key);
	}

	/**
	 * Returns true if the specified message should be processed by the
	 * conversation manager. Only CHAT type messages between two users or gateways 
	 * are processed.
	 *
	 * @param message
	 *            the message to analyze.
	 * @return true if the specified message should be processed by the
	 *         conversation manager.
	 */
	boolean isConversation(Message message) {
		if (Message.Type.chat == message.getType()) {
			return isConversationJID(message.getFrom()) && isConversationJID(message.getTo());
		}
		return false;
	}

	/**
	 * Returns true if the specified JID should be recorded in a conversation.
	 *
	 * @param jid
	 *            the JID.
	 * @return true if the JID should be recorded in a conversation.
	 */
	private boolean isConversationJID(JID jid) {
		// Ignore conversations when there is no jid
		if (jid == null) {
			return false;
		}
		XMPPServer server = XMPPServer.getInstance();
		if (jid.getNode() == null) {
			return false;
		}

		// Always accept local JIDs or JIDs related to gateways
		// (this filters our components, MUC, pubsub, etc. except gateways).
		if (server.isLocal(jid) || gateways.contains(jid.getDomain())) {
			return true;
		}

		// If not a local JID, always record it.
		if (!jid.getDomain().endsWith(serverInfo.getXMPPDomain())) {
			return true;
		}

		// Otherwise return false.
		return false;
	}

	/**
	 * Returns a unique key for a conversation between two JID's. The order of
	 * two JID parameters is irrelevant; the same key will be returned.
	 */
	String getConversationKey(JID jid1, JID jid2) {
		StringBuilder builder = new StringBuilder();
		if (jid1.compareTo(jid2) < 0) {
			builder.append(jid1.toBareJID()).append("_").append(jid2.toBareJID());
		} else {
			builder.append(jid2.toBareJID()).append("_").append(jid1.toBareJID());
		}
		return builder.toString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void componentInfoReceived(IQ iq) {
		// Check if the component is a gateway
		boolean gatewayFound = false;
		Element childElement = iq.getChildElement();
		for (Iterator<Element> it = childElement.elementIterator("identity"); it.hasNext();) {
			Element identity = it.next();
			if ("gateway".equals(identity.attributeValue("category"))) {
				gatewayFound = true;
			}
		}
		// If component is a gateway then keep track of the component
		if (gatewayFound) {
			gateways.add(iq.getFrom().getDomain());
		}
	}

	@Override
	public void componentRegistered(JID componentJID) {
		// Do nothing
	}

	@Override
	public void componentUnregistered(JID componentJID) {
		// Remove stored information about this component
		gateways.remove(componentJID.getDomain());
	}

	/**
	 * Inserts a new conversation into the database.
	 * 
	 * @throws SQLException
	 *             if an error occurs inserting the conversation.
	 */
	private void insertIntoDb(Conversation conversation) throws SQLException {
		Connection con = null;
		boolean abortTransaction = false;
		try {
			con = DbConnectionManager.getTransactionConnection();
			PreparedStatement pstmt = con.prepareStatement(Queries.INSERT_CONVERSATION);
			pstmt.setString(1, conversation.getId());
			pstmt.setString(2, conversation.getParticipantOne());
			pstmt.setString(3, conversation.getParticipantTwo());
			pstmt.setLong(4, conversation.getCreatedAt());
			pstmt.setLong(5, conversation.getUpdatedAt());
			pstmt.executeUpdate();
			pstmt.close();
		} catch (SQLException sqle) {
			abortTransaction = true;
			throw sqle;
		} finally {
			DbConnectionManager.closeTransactionConnection(con, abortTransaction);
		}
	}

	private Conversation loadFromDb(String conversationId) throws NotFoundException {
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con = DbConnectionManager.getConnection();
			pstmt = con.prepareStatement(Queries.LOAD_CONVERSATION);
			pstmt.setString(1, conversationId);
			rs = pstmt.executeQuery();
			if (!rs.next()) {
				throw new NotFoundException("Conversation not found: " + conversationId);
			}
			Conversation conversation = new Conversation(conversationId);
			conversation.setParticipantOne(rs.getString(1) == null ? null : rs.getString(1));
			conversation.setParticipantTwo(rs.getString(2) == null ? null : rs.getString(2));
			conversation.setCreatedAt(rs.getLong(3));
			conversation.setUpdatedAt(rs.getLong(4));
			conversation.setMessageCount(rs.getInt(5));
			rs.close();
			pstmt.close();

			return conversation;
		} catch (SQLException sqle) {
			log.error(sqle.getMessage(), sqle);
			return null;
		} finally {
			DbConnectionManager.closeConnection(rs, pstmt, con);
		}
	}
}
