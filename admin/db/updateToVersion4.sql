CREATE TABLE followers (
    user_id             INTEGER NOT NULL,
    follower_id         INTEGER NOT NULL
);

CREATE INDEX f_userid ON followers (user_id);

UPDATE meta_info
  SET version = 4;
