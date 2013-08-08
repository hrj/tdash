SELECT version from meta_info;
\prompt "Did you see 6 above? If no, press Ctrl+c now!" correctVersion


CREATE TABLE stat_history (
    client_id           INTEGER NOT NULL,
    rank                INTEGER NOT NULL,
    tweet_perc          FLOAT NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

UPDATE meta_info
  SET version = 7;
