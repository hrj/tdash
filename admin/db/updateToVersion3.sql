ALTER TABLE oauth_tokens DROP COLUMN screen_name;

UPDATE meta_info
  SET version = 3;
