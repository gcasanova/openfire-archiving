package com.i7.openfire.archive.xep.xep0313;

import org.jivesoftware.openfire.XMPPServer;

import com.google.common.collect.Lists;
import com.i7.openfire.archive.xep.AbstractXepSupport;

/**
 * Encapsulates support for
 * <a href="http://www.xmpp.org/extensions/xep-0313.html">XEP-0313</a>.
 */
public class Xep0313Support extends AbstractXepSupport {

	private static final String NAMESPACE = "urn:xmpp:mam:0";

	public Xep0313Support(XMPPServer server) {
		super(server, NAMESPACE, NAMESPACE, "XEP-0313 IQ Dispatcher");

		this.iqHandlers = Lists.newArrayList();
		iqHandlers.add(new IQQueryHandler());
	}

}
