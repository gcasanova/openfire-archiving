package com.i7.openfire.archive.config;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.util.JiveConstants;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PropertyEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class Properties implements PropertyEventListener {
	private static final Logger log = LoggerFactory.getLogger(Properties.class);

	private static volatile Properties instance;
	
	private static final long DEFAULT_MAX_AGE = 0;
	private static final long DEFAULT_IDLE_TIME = 0;
	private static final long DEFAULT_MAX_RETRIEVABLE = 15;
	
	private static final int DEFAULT_MAX_MESSAGES = 100;
	
	private boolean enabled;
	private boolean hotChatterEnabled;
	
    private long maxAge;
    private long idleTime;
    private long maxRetrievable;
    
    private int maxMessages;
    private int redisTimeOut;
    private int redisMaxRedirects;
    
    private Set<String> redisNodes;

    private Properties(){
    	enabled = JiveGlobals.getBooleanProperty(Conf.ENABLED.toString(), false);
    	hotChatterEnabled = JiveGlobals.getBooleanProperty(Conf.HOT_CHATTER_ENABLED.toString(), false);
    	
    	maxAge = JiveGlobals.getLongProperty(Conf.MAX_AGE.toString(), DEFAULT_MAX_AGE);
    	idleTime = JiveGlobals.getLongProperty(Conf.IDLE_TIME.toString(), DEFAULT_IDLE_TIME);
    	maxRetrievable = JiveGlobals.getLongProperty(Conf.MAX_RETRIEVABLE.toString(), DEFAULT_MAX_RETRIEVABLE);
    	
    	redisTimeOut = JiveGlobals.getIntProperty(Conf.REDIS_TIMEOUT.toString(), 0);
    	redisMaxRedirects = JiveGlobals.getIntProperty(Conf.REDIS_MAX_REDIRECTS.toString(), 0);
    	redisNodes = Sets.newHashSet(Arrays.asList(JiveGlobals.getProperty(Conf.REDIS_NODES.toString()).split(",")));
    	
    	maxMessages = JiveGlobals.getIntProperty(Conf.MAX_MESSAGES.toString(), DEFAULT_MAX_MESSAGES);
    }
    
    public static Properties getInstance() {
    	if (instance == null) {
    		synchronized (Properties.class) {
    			if (instance == null)
    				instance = new Properties();
			}
    	}
    	return instance;
    }
    
	public boolean isEnabled() {
		return enabled;
	}
	
	public boolean isHotChatterEnabled() {
		return hotChatterEnabled;
	}

	public long getMaxAge() {
		return maxAge;
	}

	public long getIdleTime() {
		return idleTime;
	}

	public long getMaxRetrievable() {
		return maxRetrievable;
	}
	
	public int getMaxMessages() {
		return maxMessages;
	}
	
	public Set<String> getRedisNodes() {
		return redisNodes;
	}
	
	public int getRedisTimeOut() {
		return redisTimeOut;
	}

	public int getRedisMaxRedirects() {
		return redisMaxRedirects;
	}

	@Override
	public void propertyDeleted(String property, Map<String, Object> params) {
		if (property.equals(Conf.IDLE_TIME.toString())) {
			idleTime = DEFAULT_IDLE_TIME * JiveConstants.MINUTE;
		} else if (property.equals(Conf.MAX_AGE.toString())) {
			maxAge = DEFAULT_MAX_AGE * JiveConstants.DAY;
		} else if (property.equals(Conf.MAX_RETRIEVABLE.toString())) {
			maxRetrievable = DEFAULT_MAX_RETRIEVABLE * JiveConstants.DAY;
		}
	}

	@Override
	public void propertySet(String property, Map<String, Object> params) {
		if (property.equals(Conf.IDLE_TIME.toString())) {
			String value = (String) params.get("value");
			try {
				idleTime = Integer.parseInt(value) * JiveConstants.MINUTE;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				idleTime = DEFAULT_IDLE_TIME * JiveConstants.MINUTE;
			}
		} else if (property.equals(Conf.MAX_AGE.toString())) {
			String value = (String) params.get("value");
			try {
				maxAge = Integer.parseInt(value) * JiveConstants.DAY;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				maxAge = DEFAULT_MAX_AGE * JiveConstants.DAY;
			}
		} else if (property.equals(Conf.MAX_RETRIEVABLE.toString())) {
			String value = (String) params.get("value");
			try {
				maxRetrievable = Integer.parseInt(value) * JiveConstants.DAY;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				maxRetrievable = DEFAULT_MAX_RETRIEVABLE * JiveConstants.DAY;
			}
		}
	}

	@Override
	public void xmlPropertyDeleted(String property, Map<String, Object> params) {
		// TODO Auto-generated method stub
	}

	@Override
	public void xmlPropertySet(String property, Map<String, Object> params) {
		// TODO Auto-generated method stub
	}
	
	private enum Conf {
		REDIS_NODES("i7.redis.nodes"),
		MAX_AGE("i7.archiving.max.age"),
		ENABLED ("i7.archiving.enabled"),
		REDIS_TIMEOUT("i7.redis.timeout"),
		IDLE_TIME("i7.archiving.idle.time"),
		MAX_MESSAGES("i7.archiving.max.messages"),
		REDIS_MAX_REDIRECTS("i7.redis.max.redirects"),
		MAX_RETRIEVABLE("i7.archiving.max.retrievable"),
		HOT_CHATTER_ENABLED("i7.archiving.hot.chatter.enabled");

        private final String value;

        Conf(String key) {
            this.value = key;
        }

        @Override
        public String toString(){
            return this.value;
        }
    }
}
