SELECT version from meta_info;
\prompt "Did you see 7 above? If no, press Ctrl+c now!" correctVersion


ALTER TABLE stat_counter ADD COLUMN retweet_count INTEGER DEFAULT 0;
ALTER TABLE stat_clients ADD COLUMN total_retweet_count bigint DEFAULT 0;

UPDATE meta_info
  SET version = 8;
