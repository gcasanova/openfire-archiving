INSERT INTO ofVersion (name, version) VALUES ('archiving', 1);

CREATE TABLE ofConversation (
  id        			VARCHAR(401)  NOT NULL,
  participantOneJID 	VARCHAR(200)  NOT NULL,
  participantTwoJID 	VARCHAR(200)  NOT NULL,
  createdAt             BIGINT        NOT NULL,
  updatedAt          	BIGINT        NOT NULL,
  messageCount          INT           NOT NULL,
  PRIMARY KEY (id),
  INDEX ofConversation_created_idx (createdAt),
  INDEX ofConversation_updated_idx (updatedAt)
);

CREATE TABLE ofMessage (
   id		 		 VARCHAR(450)	  NOT NULL,
   conversationID    VARCHAR(401)     NOT NULL,
   fromJID           VARCHAR(200)     NOT NULL,
   toJID             VARCHAR(200)     NOT NULL,
   statusCode		 TINYINT		  DEFAULT 0,
   createdAt         BIGINT           NOT NULL,
   updatedAt         BIGINT           NOT NULL,
   body              TEXT			  NOT NULL,
   PRIMARY KEY (id),
   INDEX ofMessageArchive_con_idx (conversationID),
   INDEX ofMessageArchive_fromjid_idx (fromJID),
   INDEX ofMessageArchive_tojid_idx (toJID)
);
