SELECT version from meta_info;
\prompt "Did you see 10 above? If no, press Ctrl+c now!" correctVersion

ALTER TABLE up_comments ALTER COLUMN comment_txt TYPE CHARACTER VARYING (1024);

UPDATE meta_info
  SET version = 11;
