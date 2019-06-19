ALTER TABLE CONNECTED_CLOUDS MODIFY (IP VARCHAR2(128));
/* Add AUTHENTICATED Column to CONNECTED_CLOUDS */
declare column_exists exception;
pragma exception_init(column_exists,-01430);
begin execute immediate 
'ALTER TABLE CONNECTED_CLOUDS
ADD (ROLE VARCHAR2(128))';
exception when column_exists then null;
end;
/ 
COMMENT ON COLUMN 
"INTERCLOUD"."CONNECTED_CLOUDS"."ROLE" is 
'Role of the connected cloud';
/