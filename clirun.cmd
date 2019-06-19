echo exit | sqlplus / as sysdba @sql/setpw.sql
echo exit | sqlplus INTERCLOUD/\"p@ssw0rd\"@XE @sql/import.sql
echo exit | sqlplus INTERCLOUD/\"p@ssw0rd\"@XE @sql/update_20160905.sql
echo exit | sqlplus INTERCLOUD/\"p@ssw0rd\"@XE @sql/update_20161128.sql
echo exit | sqlplus INTERCLOUD/\"p@ssw0rd\"@XE @sql/update_20161221.sql
echo exit | sqlplus INTERCLOUD/\"p@ssw0rd\"@XE @sql/update_20170104.sql
echo exit | sqlplus INTERCLOUD/\"p@ssw0rd\"@XE @sql/update_20170327.sql
echo exit | sqlplus INTERCLOUD/\"p@ssw0rd\"@XE @sql/update_20170720.sql
