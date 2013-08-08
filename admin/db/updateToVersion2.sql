CREATE TABLE users (
    user_id             INTEGER NOT NULL,
    screen_name         CHARACTER VARYING(128) NOT NULL,
    followerUpdated     DATE NOT NULL
);

CREATE INDEX u_id ON users (user_id);
CREATE INDEX u_name ON users (screen_name);

INSERT INTO users(user_id, screen_name, followerUpdated)
  SELECT DISTINCT user_id, screen_name, date('1-1-1980') FROM oauth_tokens;

UPDATE meta_info
  SET version = 2;
