SELECT version from meta_info;
\prompt "Did you see 11 above? If no, press Ctrl+c now!" correctVersion


ALTER TABLE oauth_tokens ADD COLUMN oauth_verifier CHARACTER VARYING DEFAULT '';

UPDATE meta_info
  SET version = 12;
