INSERT INTO meta_info (version) VALUES (4);

SELECT version, 'This should be 4' from meta_info;

CREATE TABLE up_comments (
    user_id             INTEGER NOT NULL,
    upload_id           INTEGER NOT NULL,
    comment_txt         CHARACTER VARYING (256) NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX comm_upload_id ON up_comments (upload_id);

UPDATE meta_info
  SET version = 5;
