/*!40101 SET storage_engine=MyISAM */;
/*!40101 SET character_set_client = utf8 */;
PRAGMA foreign_keys = ON; -- enable foreign key constraints

DROP TABLE IF EXISTS basictests;
CREATE TABLE basictests (
    uuid TEXT PRIMARY KEY NOT NULL , -- same as in JSON
    testType TEXT NOT NULL,-- test type
    exception TEXT,
    result INTEGER DEFAULT -1, -- 0 = false, 1 = true, 2 = inconclusive,
    filename TEXT,
    substringB64 TEXT,
    zipFile TEXT,
    zipItem TEXT,
    symbol TEXT,
    regex TEXT,
    signature TEXT,
    testClassName TEXT,
    vendor TEXT
);

DROP TABLE IF EXISTS basictest_chunks;
CREATE TABLE basictest_chunks (
    url TEXT PRIMARY KEY NOT NULL, -- same as chunk filename
    successful INTEGER DEFAULT 0 -- boolean
);