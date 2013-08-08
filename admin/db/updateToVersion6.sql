SELECT version from meta_info;
\prompt "Did you see 5 above? If no, press Ctrl+c now!" correctVersion


CREATE TABLE stat_counter (
    client_id           INTEGER NOT NULL,
    tweet_count         INTEGER NOT NULL,
    reply_count         INTEGER NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE stat_clients (
    client_id           INTEGER NOT NULL,
    name                CHARACTER VARYING NOT NULL,
    url                 CHARACTER VARYING NOT NULL,
    total_tweet_count   bigint NOT NULL,
    total_reply_count   bigint NOT NULL
);

UPDATE meta_info
  SET version = 6;
