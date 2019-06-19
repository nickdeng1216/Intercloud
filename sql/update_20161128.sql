declare column_exists exception;
pragma exception_init(column_exists,-01430);
begin execute immediate 
'ALTER TABLE OS_OWN_OBJECTS
ADD (DATA_SECURITY_LEVEL NUMBER)';
exception when column_exists then null;
end;
/ 
declare column_exists exception;
pragma exception_init(column_exists,-01430);
begin execute immediate 'ALTER TABLE OS_OTHERS_OBJECTS
ADD (DATA_SECURITY_LEVEL NUMBER)';
exception when column_exists then null;
end;
/ 
COMMENT ON COLUMN "INTERCLOUD"."OS_OWN_OBJECTS"."DATA_SECURITY_LEVEL" is 'The security level set for the data';
COMMENT ON COLUMN "INTERCLOUD"."OS_OTHERS_OBJECTS"."DATA_SECURITY_LEVEL" is 'The security level set for the data';
/ 
BEGIN
    BEGIN
         EXECUTE IMMEDIATE 'DROP TABLE ID_MAP';
    EXCEPTION
         WHEN OTHERS THEN
                IF SQLCODE != -942 THEN
                     RAISE;
                END IF;
    END;
    EXECUTE IMMEDIATE 'CREATE TABLE 
    ID_MAP (
        PID NUMBER NOT NULL, 
        RID NUMBER,
		STATUS NUMBER)';
END;
/ 
COMMENT ON TABLE ID_MAP IS 'The mapping of procedure ID and request ID';
COMMENT ON COLUMN ID_MAP.PID IS 'Procedure ID';
COMMENT ON COLUMN ID_MAP.RID IS 'Request ID';
COMMENT ON COLUMN ID_MAP.STATUS IS 'Status of procedure';
/