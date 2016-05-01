package com.i7.openfire.archive.cluster;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.util.cache.ClusterTask;
import org.jivesoftware.util.cache.ExternalizableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.i7.openfire.archive.ConversationEvent;
import com.i7.openfire.archive.ConversationManager;
import com.i7.openfire.archive.plugin.ArchivingPlugin;

/**
 * Task that sends conversation events to the senior cluster member.
 */
public class SendConversationEventsTask implements ClusterTask<Void> {
	private static final Logger Log = LoggerFactory.getLogger(SendConversationEventsTask.class);

	private List<ConversationEvent> events;

	/**
	 * Do not use this constructor. It only exists for serialization purposes.
	 */
	public SendConversationEventsTask() {
	}

	public SendConversationEventsTask(List<ConversationEvent> events) {
		this.events = events;
	}

	@Override
	public Void getResult() {
		return null;
	}

	@Override
	public void run() {
		ArchivingPlugin plugin = (ArchivingPlugin) XMPPServer.getInstance().getPluginManager()
				.getPlugin(ArchivingPlugin.NAME);

		ConversationManager conversationManager = plugin.getArchiveInterceptor().getConversationManager();
		for (ConversationEvent event : events) {
			try {
				event.run(conversationManager);
			} catch (Exception e) {
				Log.error("Error while processing chat archiving event", e);
			}
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		ExternalizableUtil.getInstance().writeExternalizableCollection(out, events);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		events = new ArrayList<ConversationEvent>();
		ExternalizableUtil.getInstance().readExternalizableCollection(in, events, getClass().getClassLoader());
	}
}
