/* CREATE */

CREATE table "OS_OWN_OBJECTS" (
    "ID"                  NUMBER,
    "OBJECT_NAME"         VARCHAR2(100) NOT NULL,
    "STORAGE_CLOUD"       VARCHAR2(45) NOT NULL,
    "STORAGE_OBJECT_NAME" VARCHAR2(100) NOT NULL,
    "DIGEST"              CHAR(64) NOT NULL,
    "CREATE_TIME"         DATE NOT NULL,
    "UPDATE_TIME"         DATE,
    "LAST_UPDATE"         DATE,
    constraint  "OS_OWN_OBJECTS_PK" primary key ("ID")
)
/

CREATE sequence "OS_OWN_OBJECTS_SEQ" 
/

CREATE OR REPLACE trigger "BI_OS_OWN_OBJECTS"  
  before insert on "OS_OWN_OBJECTS"              
  for each row 
begin  
  if :NEW."ID" is null then
    select "OS_OWN_OBJECTS_SEQ".nextval into :NEW."ID" from dual;
  end if;
  :NEW."LAST_UPDATE" := SYSDATE;
end;
/  

CREATE table "OS_OTHERS_OBJECTS" (
    "ID"                NUMBER,
    "OBJECT_NAME"       VARCHAR2(100) NOT NULL,
    "OWNER_CLOUD"       VARCHAR2(45) NOT NULL,
    "OWNER_OBJECT_NAME" VARCHAR2(100) NOT NULL,
    "OBJECT_PATH"       VARCHAR2(200) NOT NULL,
    "DIGEST"            CHAR(64) NOT NULL,
    "CREATE_TIME"       DATE NOT NULL,
    "UPDATE_TIME"       DATE,
    "LAST_UPDATE"       DATE,
    constraint  "OS_OTHERS_OBJECTS_PK" primary key ("ID")
)
/

CREATE sequence "OS_OTHERS_OBJECTS_SEQ" 
/

CREATE OR REPLACE trigger "BI_OS_OTHERS_OBJECTS"  
  before insert on "OS_OTHERS_OBJECTS"              
  for each row 
begin  
  if :NEW."ID" is null then
    select "OS_OTHERS_OBJECTS_SEQ".nextval into :NEW."ID" from dual;
  end if;
  :NEW."LAST_UPDATE" := SYSDATE;
end;
/   

CREATE table "CONNECTED_CLOUDS" (
    "ID"             NUMBER,
    "CLOUD_NAME"     VARCHAR2(45) NOT NULL,
    "IP"             NUMBER NOT NULL,
    "LAST_CONNECTED" DATE,
    "LAST_UPDATE"    DATE,
    constraint  "CONNECTED_CLOUDS_PK" primary key ("ID")
)
/

CREATE sequence "CONNECTED_CLOUDS_SEQ" 
/

CREATE OR REPLACE trigger "BI_CONNECTED_CLOUDS"  
  before insert on "CONNECTED_CLOUDS"              
  for each row 
begin  
  if :NEW."ID" is null then
    select "CONNECTED_CLOUDS_SEQ".nextval into :NEW."ID" from dual;
  end if;
  :NEW."LAST_UPDATE" := SYSDATE;
end;
/   

/* INSERT */

INSERT INTO "INTERCLOUD"."OS_OWN_OBJECTS" 
(OBJECT_NAME, STORAGE_CLOUD, STORAGE_OBJECT_NAME, DIGEST, CREATE_TIME) VALUES 
('CHINESE_BOOK', 'Cloud9', 'CHINESE_BOOK_9', 'v1dS4iiAFm6PyKvMEeA9v1dS4iiAFm6PyKvMEeA9v1dS4iiAFm6PyKvMEeA91234', 
  TO_DATE('2016-07-27 16:29:53', 'YYYY-MM-DD HH24:MI:SS'))
/
INSERT INTO "INTERCLOUD"."OS_OWN_OBJECTS" 
(OBJECT_NAME, STORAGE_CLOUD, STORAGE_OBJECT_NAME, DIGEST, CREATE_TIME) VALUES 
('ENGLISH_BOOK', 'Cloud8', 'ENGLISH_BOOK_7', 'ZzkKOsaCCpESHfneDeT9p2qyVrcKiHq8804STPU66umBcEnKYAbzHLoOnDs0N1AU', 
TO_DATE('2016-06-02 16:34:01', 'YYYY-MM-DD HH24:MI:SS'))
/
INSERT INTO "INTERCLOUD"."OS_OWN_OBJECTS" 
(OBJECT_NAME, STORAGE_CLOUD, STORAGE_OBJECT_NAME, DIGEST, CREATE_TIME) VALUES 
('MATH_BOOK', 'Cloud8', 'MATH_BOOK_12', 'g1o7oXRlvjkAKv7hgJS8DUNetmXcH0Y3kSlauovZuYLGk3RubOZUnxnPSPIfjE2V', 
  TO_DATE('2015-07-01 16:35:01', 'YYYY-MM-DD HH24:MI:SS'))
/

INSERT INTO "INTERCLOUD"."OS_OTHERS_OBJECTS" 
(OBJECT_NAME, OWNER_CLOUD, OWNER_OBJECT_NAME, OBJECT_PATH, DIGEST, CREATE_TIME) VALUES 
('HISTORY_BOOK_0', 'Cloud7', 'HISTORY_BOOK', '/cloud7/history_book_0.pdf', 'y8X8pvpwp4Sr40HMbKewkqCcoAZKhDn7hlOpSTGefVnIPv4Yh5EaIGvNWNTJ9qir', 
  TO_DATE('2015-11-12 16:38:54', 'YYYY-MM-DD HH24:MI:SS'))
/
INSERT INTO "INTERCLOUD"."OS_OTHERS_OBJECTS" 
(OBJECT_NAME, OWNER_CLOUD, OWNER_OBJECT_NAME, OBJECT_PATH, DIGEST, CREATE_TIME) VALUES 
('MUSIC_BOOK_6', 'Cloud10', 'MUSIC_BOOK', '/cloud10/music_book_10.pdf', 'fx6OwQFWLc5fMVQ9vUVHXiFokn7lV1luurPy6PQlQWJhPxc5wfxyP3qg2JymYfuZ', 
  TO_DATE('2016-01-27 16:40:08', 'YYYY-MM-DD HH24:MI:SS'))
/