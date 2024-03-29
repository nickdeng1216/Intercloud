

  CREATE TABLE "INTERCLOUD"."VM_OTHERS_VMS" 
   (	"ID" NUMBER, 
	"VMNAME" VARCHAR2(20 BYTE), 
	"OWNER_CLOUD" VARCHAR2(20 BYTE), 
	"OWNER_VMNAME" VARCHAR2(20 BYTE), 
	"CREATE_TIME" DATE, 
	"UPDATE_TIME" DATE
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;

   COMMENT ON COLUMN "INTERCLOUD"."VM_OTHERS_VMS"."ID" IS 'The request protocol ID';
   COMMENT ON COLUMN "INTERCLOUD"."VM_OTHERS_VMS"."VMNAME" IS 'The name of the VM here';
   COMMENT ON COLUMN "INTERCLOUD"."VM_OTHERS_VMS"."OWNER_CLOUD" IS 'The cloud which owns the VM';
   COMMENT ON COLUMN "INTERCLOUD"."VM_OTHERS_VMS"."OWNER_VMNAME" IS 'The VM name received when the owner transfer the VM';
REM INSERTING into INTERCLOUD.VM_OTHERS_VMS
SET DEFINE OFF;
--------------------------------------------------------
--  Constraints for Table VM_OTHERS_VMS
--------------------------------------------------------

  ALTER TABLE "INTERCLOUD"."VM_OTHERS_VMS" MODIFY ("ID" NOT NULL ENABLE);
--------------------------------------------------------
--  DDL for Trigger VM_OTHERS_VMS_TRIGGER
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "INTERCLOUD"."VM_OTHERS_VMS_TRIGGER" 
BEFORE INSERT ON VM_OTHERS_VMS              
  for each row 
begin  
 
  :NEW."CREATE_TIME" := SYSDATE;
end;
/
ALTER TRIGGER "INTERCLOUD"."VM_OTHERS_VMS_TRIGGER" ENABLE;
--------------------------------------------------------
--  DDL for Trigger VM_OTHERS_VMS_TRIGGER_1
--------------------------------------------------------

  CREATE OR REPLACE TRIGGER "INTERCLOUD"."VM_OTHERS_VMS_TRIGGER_1" 
BEFORE INSERT OR UPDATE ON VM_OTHERS_VMS              
  for each row 
begin  
 
  :NEW."UPDATE_TIME" := SYSDATE;
end;
/
ALTER TRIGGER "INTERCLOUD"."VM_OTHERS_VMS_TRIGGER_1" ENABLE;







  CREATE TABLE "INTERCLOUD"."VM_OWN_VMS" 
   (	"ID" NUMBER NOT NULL ENABLE, 
	"VMNAME" VARCHAR2(20 BYTE) NOT NULL ENABLE, 
	"STORAGE_CLOUD" VARCHAR2(20 BYTE), 
	"STORAGE_VMNAME" VARCHAR2(20 BYTE) NOT NULL ENABLE, 
	"CREATE_TIME" DATE, 
	"UPDATE_TIME" DATE
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;

   COMMENT ON COLUMN "INTERCLOUD"."VM_OWN_VMS"."ID" IS 'The request protocol ID';
   COMMENT ON COLUMN "INTERCLOUD"."VM_OWN_VMS"."VMNAME" IS 'The name of the VM here';
   COMMENT ON COLUMN "INTERCLOUD"."VM_OWN_VMS"."STORAGE_CLOUD" IS 'The cloud which hosts the VM';
   COMMENT ON COLUMN "INTERCLOUD"."VM_OWN_VMS"."STORAGE_VMNAME" IS 'The VM name returned from the cloud';

  CREATE OR REPLACE TRIGGER "INTERCLOUD"."VM_OWN_VMS_TRIGGER" 
BEFORE INSERT ON VM_OWN_VMS              
  for each row 
begin  
 
  :NEW."CREATE_TIME" := SYSDATE;
end;
/
ALTER TRIGGER "INTERCLOUD"."VM_OWN_VMS_TRIGGER" ENABLE;

  CREATE OR REPLACE TRIGGER "INTERCLOUD"."VM_OWN_VMS_TRIGGER_1" 
BEFORE INSERT OR UPDATE ON VM_OWN_VMS              
  for each row 
begin  
 
  :NEW."UPDATE_TIME" := SYSDATE;
end;
/
ALTER TRIGGER "INTERCLOUD"."VM_OWN_VMS_TRIGGER_1" ENABLE;