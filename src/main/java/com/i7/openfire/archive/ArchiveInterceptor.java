package com.i7.openfire.archive;

import java.time.Instant;

import org.dom4j.Element;
import org.jivesoftware.openfire.cluster.ClusterManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import com.i7.openfire.archive.enums.MessageStatus;

public class ArchiveInterceptor implements PacketInterceptor, Startable {
	private static final Logger log = LoggerFactory.getLogger(ArchiveInterceptor.class);

	private ConversationManager conversationManager;

	public ArchiveInterceptor(ConversationManager conversationManager) {
		this.conversationManager = conversationManager;
	}

	@Override
	public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed)
			throws PacketRejectedException {

		// Ignore any packets that haven't already been processed.
		if (!processed || !incoming) {
			return;
		}

		if (packet instanceof Message) {
			Message message = (Message) packet;
			// Only process messages that are between two users or gateways.
			if (conversationManager.isConversation(message)) {
				Element element = message.getChildElement("messageStatus", "com.i7.openfire");
				if (element != null) {
					String statusCode = element.getText();
					
					boolean isStatusUpdate = false;
					MessageStatus messageStatus = MessageStatus.findByValue(Integer.valueOf(statusCode));
					if (messageStatus != null && messageStatus.getValue() != MessageStatus.SENT.getValue()) {
						isStatusUpdate = true;
					} else if (messageStatus == null) {
						messageStatus = MessageStatus.SENT;
					}

					element = message.getChildElement("messageId", "com.i7.openfire");
					if (element != null) {
						String messageId = element.getText();

						// Process this event in the senior cluster member or local
						// JVM when not in a cluster
						if (ClusterManager.isSeniorClusterMember()) {
							if (isStatusUpdate) {
								conversationManager.processMessageUpdate(messageId, message.getFrom(), message.getTo(),
										messageStatus, Instant.now().toEpochMilli());

							} else if (message.getBody() != null) {
								conversationManager.processMessage(messageId, message.getFrom(), message.getTo(),
										message.getBody(), Instant.now().toEpochMilli());
							}
						} else {
							JID sender = message.getFrom();
							JID receiver = message.getTo();

							ConversationEventsQueue eventsQueue = conversationManager.getConversationEventsQueue();
							eventsQueue.addChatEvent(conversationManager.getConversationKey(sender, receiver),
									ConversationEvent.chatMessageReceived(messageId, sender, receiver, message.getBody(),
											messageStatus, Instant.now().toEpochMilli()));
						}
					} else {
						log.error("Message is missing messageId, message: {}", message.toString());
					}
				} else {
					log.error("Message is missing messageStatus, message: {}", message.toString());
				}
			}
		}
	}

	public ConversationManager getConversationManager() {
		return conversationManager;
	}

	@Override
	public void start() {
		InterceptorManager.getInstance().addInterceptor(this);
	}

	@Override
	public void stop() {
		InterceptorManager.getInstance().removeInterceptor(this);
		conversationManager = null;
	}
}
