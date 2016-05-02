package com.i7.openfire.archive.xep.xep0136;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.disco.ServerFeaturesProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import com.google.common.collect.Lists;
import com.i7.openfire.archive.model.ArchivedMessage;
import com.i7.openfire.archive.xep.AbstractIQHandler;
import com.i7.openfire.archive.xep.xep0059.XmppResultSet;
import com.i7.openfire.archive.xep.xep0313.IQQueryHandler;
import com.thoughtworks.xstream.XStream;

/**
 * Message Archiving List Handler.
 */
public class IQListHandler extends AbstractIQHandler implements ServerFeaturesProvider {
	private static final Logger log = LoggerFactory.getLogger(IQQueryHandler.class);

	private static final String NAMESPACE = "urn:xmpp:archive";
	private static final String NAMESPACE_MANAGE = "urn:xmpp:archive:manage";

	public IQListHandler() {
		super("Message Archiving List Handler", "list", NAMESPACE);
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		IQ reply = IQ.createResultIQ(packet);
		ListRequest listRequest = new ListRequest(packet.getChildElement());
		JID from = packet.getFrom();

		Element listElement = reply.setChildElement("list", NAMESPACE);
		Collection<ArchivedMessage> messages = list(from, listRequest);
		XmppResultSet resultSet = listRequest.getResultSet();

		for (ArchivedMessage message : messages) {
			addChatElement(listElement, message);
		}

		if (resultSet != null) {
			listElement.add(resultSet.createResultElement());
		}

		return reply;
	}

	private List<ArchivedMessage> list(JID from, ListRequest request) {
		return getPersistenceManager().getConversationsLastMessage(from.toBareJID(), request.getStart(),
				request.getEnd());
	}

	private Element addChatElement(Element listElement, ArchivedMessage message) {
		Element chatElement = listElement.addElement("chat");
		
		Document document;
		try {
			document = DocumentHelper.parseText(new XStream().toXML(message));
			chatElement.add(document.getRootElement());
		} catch (DocumentException e) {
			log.error("Failed to parse message stanza.", e);
			// If we can't parse then we have no message to send to client, abort
			return null;
		}
		return chatElement;
	}

	@Override
	public Iterator<String> getFeatures() {
		List<String> features = Lists.newArrayList();
		features.add(NAMESPACE_MANAGE);
		return features.iterator();
	}

}
