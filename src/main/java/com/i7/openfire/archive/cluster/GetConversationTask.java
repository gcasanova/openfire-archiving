package com.i7.openfire.archive.cluster;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.util.NotFoundException;
import org.jivesoftware.util.cache.ClusterTask;
import org.jivesoftware.util.cache.ExternalizableUtil;

import com.i7.openfire.archive.ConversationManager;
import com.i7.openfire.archive.model.Conversation;
import com.i7.openfire.archive.plugin.ArchivingPlugin;

/**
 * Task that returns the specified conversation or <tt>null</tt> if not found.
 */
public class GetConversationTask implements ClusterTask<Conversation> {
	private String conversationID;
	private Conversation conversation;

	public GetConversationTask() {
	}

	public GetConversationTask(String conversationID) {
		this.conversationID = conversationID;
	}

	@Override
	public Conversation getResult() {
		return conversation;
	}

	@Override
	public void run() {
		ArchivingPlugin plugin = (ArchivingPlugin) XMPPServer.getInstance().getPluginManager()
				.getPlugin(ArchivingPlugin.NAME);

		ConversationManager conversationManager = plugin.getArchiveInterceptor().getConversationManager();
		try {
			conversation = conversationManager.getConversation(conversationID);
		} catch (NotFoundException e) {
			// Ignore. The requester of this task will throw this exception in
			// his JVM
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		ExternalizableUtil.getInstance().writeBoolean(out, conversationID != null);
		if (conversationID != null) {
			ExternalizableUtil.getInstance().writeSafeUTF(out, conversationID);
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		if (ExternalizableUtil.getInstance().readBoolean(in)) {
			conversationID = ExternalizableUtil.getInstance().readSafeUTF(in);
		}
	}
}
