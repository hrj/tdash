SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

CREATE TABLE  meta_info (
    version integer NOT NULL
);

ALTER TABLE meta_info OWNER TO postgres;

REVOKE ALL ON TABLE meta_info FROM PUBLIC;
REVOKE ALL ON TABLE meta_info FROM postgres;
GRANT ALL ON TABLE meta_info TO postgres;
GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE meta_info TO PUBLIC;

CREATE TABLE oauth_tokens (
    user_id             INTEGER NOT NULL,
    screen_name         CHARACTER VARYING(128) NOT NULL,
    oauth_token         CHARACTER VARYING NOT NULL,
    oauth_token_secret  CHARACTER VARYING NOT NULL,
    created_at          DATE NOT NULL
);

CREATE INDEX o_tokens ON oauth_tokens (oauth_token);

ALTER TABLE public.oauth_tokens OWNER TO postgres;

CREATE TABLE uploads (
    id          INTEGER NOT NULL,
    user_id     INTEGER NOT NULL,
    descr       CHARACTER VARYING (512) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    image_name  CHARACTER VARYING(1024) NOT NULL,
    image_type  CHARACTER VARYING(128) NOT NULL,
    view_count  INTEGER,
    view_logged_count INTEGER NOT NULL,
    cloud       BOOLEAN,
    safe        BOOLEAN
);

ALTER TABLE ONLY uploads
    ADD CONSTRAINT "uploadsId" PRIMARY KEY (id);

CREATE INDEX u_userId ON uploads (user_id);
CREATE INDEX u_created_at ON uploads (created_at);

ALTER TABLE public.uploads OWNER TO postgres;

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;
