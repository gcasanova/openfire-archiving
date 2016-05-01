package com.i7.openfire.archive.plugin;

import java.io.File;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.jivesoftware.util.PropertyEventListener;
import org.jivesoftware.util.TaskEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.i7.openfire.archive.ArchiveInterceptor;
import com.i7.openfire.archive.ArchiveSearcher;
import com.i7.openfire.archive.ConversationManager;
import com.i7.openfire.archive.PersistenceManager;
import com.i7.openfire.archive.config.Properties;
import com.i7.openfire.archive.impl.JdbcPersistenceManager;

public class ArchivingPlugin implements Plugin {
	private static final Logger log = LoggerFactory.getLogger(ArchivingPlugin.class);
	
	public static final String NAME = "archiving";

	private boolean enabled = true;
	private boolean shuttingDown = false;

	private ArchiveSearcher archiveSearcher;
	private PersistenceManager persistenceManager;
	private ArchiveInterceptor archiveInterceptor;
	private PropertyEventListener propertyListener;

	private static ArchivingPlugin instance;

	public ArchivingPlugin() {
		instance = this;
	}

	public static ArchivingPlugin getInstance() {
		return instance;
	}

	@Override
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		log.debug("Initializing plugin");
		enabled = Properties.getInstance().isEnabled();
		
		// Listen for any changes to properties.
		propertyListener = Properties.getInstance();
		PropertyEventDispatcher.addListener(propertyListener);

		persistenceManager = new JdbcPersistenceManager();
		shuttingDown = false;

		// Make sure that the archiving folder exists under the home directory
		File dir = new File(JiveGlobals.getHomeDirectory() + File.separator + NAME);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		ConversationManager conversationManager = new ConversationManager(TaskEngine.getInstance());

		archiveSearcher = new ArchiveSearcher(conversationManager);
		archiveInterceptor = new ArchiveInterceptor(conversationManager);

		archiveSearcher.start();
		archiveInterceptor.start();
	}

	@Override
	public void destroyPlugin() {
		log.debug("Destroying plugin");
		shuttingDown = true;
		
		PropertyEventDispatcher.removeListener(propertyListener);
		propertyListener = null;

		archiveSearcher.stop();
		archiveInterceptor.stop();
		
		instance = null;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public boolean isShuttingDown() {
		return shuttingDown;
	}

	public ArchiveSearcher getArchiveSearcher() {
		return archiveSearcher;
	}

	public ArchiveInterceptor getArchiveInterceptor() {
		return archiveInterceptor;
	}

	public PersistenceManager getPersistenceManager() {
		return persistenceManager;
	}
}
