comment on table "INTERCLOUD"."CONNECTED_CLOUDS"  is 'Records the connection with other clouds.';
/
ALTER TABLE CONNECTED_CLOUDS
  MODIFY CLOUD_NAME VARCHAR2(128);
/
comment on column "INTERCLOUD"."CONNECTED_CLOUDS"."ID" is 'Integer key, generated by Oracle';
comment on column "INTERCLOUD"."CONNECTED_CLOUDS"."CLOUD_NAME" is 'Name of the connected cloud';
comment on column "INTERCLOUD"."CONNECTED_CLOUDS"."IP" is 'IP address of the cloud when it was last connected';
comment on column "INTERCLOUD"."CONNECTED_CLOUDS"."LAST_CONNECTED" is 'The time of last connection';
comment on column "INTERCLOUD"."CONNECTED_CLOUDS"."LAST_UPDATE" is 'Last update time of the record, auto updated';
/

CREATE TABLE REQ_TRACK 
(
  ID NUMBER NOT NULL 
, TARGET_CLOUD VARCHAR2(128) NOT NULL 
, REQUEST VARCHAR2(4000) NOT NULL 
, RETURN VARCHAR2(4000) 
, LAST_UPDATE DATE 
, CONSTRAINT REQ_TRACK_PK PRIMARY KEY 
  (
    ID 
  )
  ENABLE 
);
/
comment on table "INTERCLOUD"."REQ_TRACK"  is 'Tracks the status of requests.';
/
COMMENT ON COLUMN REQ_TRACK.ID IS 'The request ID';
COMMENT ON COLUMN REQ_TRACK.TARGET_CLOUD IS 'The target cloud';
COMMENT ON COLUMN REQ_TRACK.REQUEST IS 'The request message';
COMMENT ON COLUMN REQ_TRACK.RETURN IS 'The returned message (i.e. the response or exception message)';
COMMENT ON COLUMN REQ_TRACK.LAST_UPDATE IS 'Last update time of the record, auto updated';
/

ALTER TABLE OS_OTHERS_OBJECTS
  ADD Last_get_attempt DATE;
ALTER TABLE OS_OTHERS_OBJECTS
  MODIFY (
  	OBJECT_NAME VARCHAR2(512), 
  	OWNER_CLOUD VARCHAR2(128), 
  	OWNER_OBJECT_NAME VARCHAR2(512), 
  	OBJECT_PATH VARCHAR2(4000) NULL
  	);
/
comment on table "INTERCLOUD"."OS_OTHERS_OBJECTS"  is 'Records the transfer of other''s data object.';
/
comment on column "INTERCLOUD"."OS_OTHERS_OBJECTS"."ID" is 'The request ID';
comment on column "INTERCLOUD"."OS_OTHERS_OBJECTS"."OWNER_CLOUD" is 'The cloud which owns the data';
comment on column "INTERCLOUD"."OS_OTHERS_OBJECTS"."OBJECT_NAME" is 'Name of the data object here';
comment on column "INTERCLOUD"."OS_OTHERS_OBJECTS"."OWNER_OBJECT_NAME" is 'The object name received when the owner transferred the object';
comment on column "INTERCLOUD"."OS_OTHERS_OBJECTS"."OBJECT_PATH" is 'The location/path to the object here';
comment on column "INTERCLOUD"."OS_OTHERS_OBJECTS"."DIGEST" is 'The encrypted data''s digest';
comment on column "INTERCLOUD"."OS_OTHERS_OBJECTS"."CREATE_TIME" is 'The time when the object was initially stored';
comment on column "INTERCLOUD"."OS_OTHERS_OBJECTS"."UPDATE_TIME" is 'The time when the object was updated here';
comment on column "INTERCLOUD"."OS_OTHERS_OBJECTS"."LAST_UPDATE" is 'Last update time of the record, auto updated';
comment on column "INTERCLOUD"."OS_OTHERS_OBJECTS"."LAST_GET_ATTEMPT" is 'The time of latest GET request';
/

ALTER TABLE OS_OWN_OBJECTS
  ADD (
  	Last_get_attempt DATE, 
  	Status VARCHAR2(4000)
  	);
ALTER TABLE OS_OWN_OBJECTS
  MODIFY (
  	OBJECT_NAME VARCHAR2(512), 
  	STORAGE_CLOUD VARCHAR2(128), 
  	STORAGE_OBJECT_NAME VARCHAR2(512)
  	);
/
comment on table "INTERCLOUD"."OS_OWN_OBJECTS"  is 'Records the transfer of cloud''s own data object.';
/
comment on column "INTERCLOUD"."OS_OWN_OBJECTS"."ID" is 'The request ID';
comment on column "INTERCLOUD"."OS_OWN_OBJECTS"."OBJECT_NAME" is 'Name of the data object here';
comment on column "INTERCLOUD"."OS_OWN_OBJECTS"."STORAGE_CLOUD" is 'The cloud which has stored the data';
comment on column "INTERCLOUD"."OS_OWN_OBJECTS"."STORAGE_OBJECT_NAME" is 'The object name returned from the storage cloud';
comment on column "INTERCLOUD"."OS_OWN_OBJECTS"."DIGEST" is 'The encrypted data''s digest';
comment on column "INTERCLOUD"."OS_OWN_OBJECTS"."CREATE_TIME" is 'The time when the object was initially stored';
comment on column "INTERCLOUD"."OS_OWN_OBJECTS"."UPDATE_TIME" is 'The time when the object was updated in the storage cloud';
comment on column "INTERCLOUD"."OS_OWN_OBJECTS"."LAST_UPDATE" is 'Last update time of the record, auto updated';
comment on column "INTERCLOUD"."OS_OWN_OBJECTS"."LAST_GET_ATTEMPT" is 'The time of latest GET request';
comment on column "INTERCLOUD"."OS_OWN_OBJECTS"."STATUS" is 'The current status of transfer';
/

CREATE OR REPLACE TRIGGER BI_CONNECTED_CLOUDS 
BEFORE INSERT OR UPDATE ON CONNECTED_CLOUDS              
  for each row 
begin  
  if :NEW."ID" is null then
    select "CONNECTED_CLOUDS_SEQ".nextval into :NEW."ID" from dual;
  end if;
  :NEW."LAST_UPDATE" := SYSDATE;
end;
/
CREATE OR REPLACE TRIGGER BI_OS_OTHERS_OBJECTS  
BEFORE INSERT OR UPDATE ON OS_OTHERS_OBJECTS              
  for each row 
begin  
  if :NEW."ID" is null then
    select "OS_OTHERS_OBJECTS_SEQ".nextval into :NEW."ID" from dual;
  end if;
  :NEW."LAST_UPDATE" := SYSDATE;
end;
/
CREATE OR REPLACE TRIGGER BI_OS_OWN_OBJECTS  
BEFORE INSERT OR UPDATE ON OS_OWN_OBJECTS              
  for each row 
begin  
  if :NEW."ID" is null then
    select "OS_OWN_OBJECTS_SEQ".nextval into :NEW."ID" from dual;
  end if;
  :NEW."LAST_UPDATE" := SYSDATE;
end;
/
CREATE sequence "REQ_TRACK_SEQ" 
/
CREATE OR REPLACE TRIGGER BI_REQ_TRACK 
BEFORE INSERT OR UPDATE ON REQ_TRACK 
  for each row 
begin  
  if :NEW."ID" is null then
    select "REQ_TRACK_SEQ".nextval into :NEW."ID" from dual;
  end if;
  :NEW."LAST_UPDATE" := SYSDATE;
end;
/
