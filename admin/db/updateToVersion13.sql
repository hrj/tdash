SELECT version from meta_info;
\prompt "Did you see 12 above? If no, press Ctrl+c now!" correctVersion

CREATE TABLE android_oauth_tokens (
    user_id             INTEGER NOT NULL,
    oauth_token         CHARACTER VARYING NOT NULL,
    oauth_token_secret  CHARACTER VARYING NOT NULL,
    created_at          DATE NOT NULL,
    oauth_verifier      CHARACTER VARYING
);

UPDATE meta_info
  SET version = 13;
