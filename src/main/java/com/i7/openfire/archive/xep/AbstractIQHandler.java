package com.i7.openfire.archive.xep;

import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.handler.IQHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import com.i7.openfire.archive.PersistenceManager;
import com.i7.openfire.archive.plugin.ArchivingPlugin;

/**
 * Abstract base class for XEP-specific IQ Handlers.
 */
public abstract class AbstractIQHandler extends IQHandler {

	private final IQHandlerInfo info;

	protected AbstractIQHandler(String moduleName, String elementName, String namespace) {
		super(moduleName);
		this.info = new IQHandlerInfo(elementName, namespace);
	}

	@Override
	public final IQHandlerInfo getInfo() {
		return info;
	}

	protected PersistenceManager getPersistenceManager() {
		return ArchivingPlugin.getInstance().getPersistenceManager();
	}

	protected IQ error(Packet packet, PacketError.Condition condition) {
		IQ reply;

		reply = new IQ(IQ.Type.error, packet.getID());
		reply.setFrom(packet.getTo());
		reply.setTo(packet.getFrom());
		reply.setError(condition);
		return reply;
	}
}
