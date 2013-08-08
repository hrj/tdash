SELECT version from meta_info;
\prompt "Did you see 8 above? If no, press Ctrl+c now!" correctVersion

CREATE INDEX idx_clientid_history ON stat_history (client_id);
CREATE INDEX idx_createdat_history ON stat_history (created_at);

UPDATE meta_info
  SET version = 9;
