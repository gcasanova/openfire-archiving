package com.i7.openfire.archive.xep.xep0313;

import java.text.ParseException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.disco.ServerFeaturesProvider;
import org.jivesoftware.openfire.session.LocalClientSession;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError;

import com.i7.openfire.archive.model.ArchivedMessage;
import com.i7.openfire.archive.xep.AbstractIQHandler;
import com.i7.openfire.archive.xep.xep0059.XmppResultSet;
import com.thoughtworks.xstream.XStream;

/**
 * XEP-0313 IQ Query Handler
 */
public class IQQueryHandler extends AbstractIQHandler implements ServerFeaturesProvider {
	private static final Logger log = LoggerFactory.getLogger(IQQueryHandler.class);

	private static final String NAMESPACE = "urn:xmpp:mam:0";
	private static final String MODULE_NAME = "Message Archive Management Query Handler";

	XMPPDateTimeFormat xmppDateTimeFormat = new XMPPDateTimeFormat();

	protected IQQueryHandler() {
		super(MODULE_NAME, "query", NAMESPACE);
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		LocalClientSession session = (LocalClientSession) sessionManager.getSession(packet.getFrom());

		// If no session was found then answer with an error (if possible)
		if (session == null) {
			log.error("Error during resource binding. Session not found in " + sessionManager.getPreAuthenticatedKeys()
					+ " for key " + packet.getFrom());
			return buildErrorResponse(packet);
		}

		if (packet.getType().equals(IQ.Type.get)) {
			sendSupportedFieldsResult(packet, session);
			return null;
		}

		// Default to user's own archive
		JID archiveJid = packet.getFrom();

		if (packet.getElement().attribute("to") != null) {
			archiveJid = new JID(packet.getElement().attribute("to").getStringValue());
			// Only allow queries to users own archives
			if (!archiveJid.toBareJID().equals(packet.getFrom().toBareJID())) {
				return buildForbiddenResponse(packet);
			}
		}

		final QueryRequest queryRequest = new QueryRequest(packet.getChildElement(), archiveJid);
		Collection<ArchivedMessage> archivedMessages = retrieveMessages(queryRequest);

		for (ArchivedMessage archivedMessage : archivedMessages) {
			sendMessageResult(session, queryRequest, archivedMessage);
		}

		sendFinalMessage(session, queryRequest);

		sendAcknowledgementResult(packet, session);

		return null;
	}

	/**
	 * Create error response to send to client
	 * 
	 * @param packet
	 * @return
	 */
	private IQ buildErrorResponse(IQ packet) {
		IQ reply = IQ.createResultIQ(packet);
		reply.setChildElement(packet.getChildElement().createCopy());
		reply.setError(PacketError.Condition.internal_server_error);
		return reply;
	}

	/**
	 * Create error response due to forbidden request
	 * 
	 * @param packet
	 *            Received request
	 * @return
	 */
	private IQ buildForbiddenResponse(IQ packet) {
		IQ reply = IQ.createResultIQ(packet);
		reply.setChildElement(packet.getChildElement().createCopy());
		reply.setError(PacketError.Condition.forbidden);
		return reply;
	}

	/**
	 * Retrieve messages matching query request from server archive
	 * 
	 * @param queryRequest
	 * @return
	 */
	private Collection<ArchivedMessage> retrieveMessages(QueryRequest queryRequest) {

		String endField = null;
		String withField = null;
		String startField = null;

		DataForm dataForm = queryRequest.getDataForm();
		if (dataForm != null) {
			if (dataForm.getField("with") != null) {
				withField = dataForm.getField("with").getFirstValue();
			}
			if (dataForm.getField("start") != null) {
				startField = dataForm.getField("start").getFirstValue();
			}
			if (dataForm.getField("end") != null) {
				endField = dataForm.getField("end").getFirstValue();
			}
		}

		Date endDate = null;
		Date startDate = null;

		try {
			if (startField != null) {
				startDate = xmppDateTimeFormat.parseString(startField);
			}
			if (endField != null) {
				endDate = xmppDateTimeFormat.parseString(endField);
			}
		} catch (ParseException e) {
			log.error("Error parsing query date filters.", e);
		}

		String conversationId = getPersistenceManager().getConversation(queryRequest.getArchive().toBareJID(),
				withField);
		return getPersistenceManager().getMessages(conversationId, startDate, endDate, queryRequest.getResultSet());
	}

	/**
	 * Send result packet to client acknowledging query.
	 * 
	 * @param packet
	 *            Received query packet
	 * @param session
	 *            Client session to respond to
	 */
	private void sendAcknowledgementResult(IQ packet, LocalClientSession session) {
		IQ result = IQ.createResultIQ(packet);
		session.process(result);
	}

	/**
	 * Send final message back to client following query.
	 * 
	 * @param session
	 *            Client session to respond to
	 * @param queryRequest
	 *            Received query request
	 */
	private void sendFinalMessage(LocalClientSession session, final QueryRequest queryRequest) {

		Message finalMessage = new Message();
		finalMessage.setTo(session.getAddress());
		Element fin = finalMessage.addChildElement("fin", NAMESPACE);
		if (queryRequest.getQueryid() != null) {
			fin.addAttribute("queryid", queryRequest.getQueryid());
		}

		XmppResultSet resultSet = queryRequest.getResultSet();
		if (resultSet != null) {
			fin.add(resultSet.createResultElement());

			if (resultSet.isComplete()) {
				fin.addAttribute("complete", "true");
			}
		}

		session.process(finalMessage);
	}

	/**
	 * Send archived message to requesting client
	 * 
	 * @param session
	 *            Client session that send message to
	 * @param queryRequest
	 *            Query request made by client
	 * @param archivedMessage
	 *            Message to send to client
	 * @return
	 */
	private void sendMessageResult(LocalClientSession session, QueryRequest queryRequest,
			ArchivedMessage archivedMessage) {

		Message messagePacket = new Message();
		messagePacket.setTo(session.getAddress());

		Element result = messagePacket.addChildElement("result", NAMESPACE);
		result.addAttribute("id", archivedMessage.getId());
		if (queryRequest.getQueryid() != null) {
			result.addAttribute("queryid", queryRequest.getQueryid());
		}

		Element forwarded = result.addElement("forwarded", "urn:xmpp:forward:0");
		Element delay = forwarded.addElement("delay", "urn:xmpp:delay");

		delay.addAttribute("stamp",
				XMPPDateTimeFormat.format(Date.from(Instant.ofEpochMilli(archivedMessage.getCreatedAt()))));

		Document document;
		try {
			document = DocumentHelper.parseText(new XStream().toXML(archivedMessage));
			forwarded.add(document.getRootElement());
		} catch (DocumentException e) {
			log.error("Failed to parse message stanza.", e);
			// If we can't parse then we have no message to send to client, abort
			return;
		}
		session.process(messagePacket);
	}

	/**
	 * Declare DataForm fields supported by the MAM implementation on this
	 * server
	 * 
	 * @param packet
	 *            Incoming query (form field request) packet
	 * @param session
	 *            Session with client
	 */
	private void sendSupportedFieldsResult(IQ packet, LocalClientSession session) {

		IQ result = IQ.createResultIQ(packet);

		Element query = result.setChildElement("query", NAMESPACE);

		DataForm form = new DataForm(DataForm.Type.form);
		form.addField("FORM_TYPE", null, FormField.Type.hidden);
		form.getField("FORM_TYPE").addValue(NAMESPACE);
		form.addField("with", null, FormField.Type.jid_single);
		form.addField("start", null, FormField.Type.text_single);
		form.addField("end", null, FormField.Type.text_single);

		query.add(form.getElement());

		session.process(result);
	}

	@Override
	public Iterator<String> getFeatures() {
		return Collections.singleton(NAMESPACE).iterator();
	}

}
