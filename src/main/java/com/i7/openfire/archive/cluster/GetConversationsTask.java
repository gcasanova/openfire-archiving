package com.i7.openfire.archive.cluster;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.util.cache.ClusterTask;

import com.i7.openfire.archive.ConversationManager;
import com.i7.openfire.archive.model.Conversation;
import com.i7.openfire.archive.plugin.ArchivingPlugin;

/**
 * Task that will return current conversations taking place in the senior
 * cluster member. All conversations in the cluster are kept in the senior
 * cluster member.
 */
public class GetConversationsTask implements ClusterTask<Collection<Conversation>> {
	private Collection<Conversation> conversations;

	@Override
	public Collection<Conversation> getResult() {
		return conversations;
	}

	@Override
	public void run() {
		ArchivingPlugin plugin = (ArchivingPlugin) XMPPServer.getInstance().getPluginManager()
				.getPlugin(ArchivingPlugin.NAME);

		ConversationManager conversationManager = plugin.getArchiveInterceptor().getConversationManager();
		conversations = conversationManager.getConversations();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		// Do nothing
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		// Do nothing
	}
}
