DROP TABLE IF EXISTS files;
CREATE TABLE files(
  _id INTEGER PRIMARY KEY,			-- File ID, incremental and unique in the db
  filename CHAR(64) NOT NULL UNIQUE, 		-- The filename of the file to upload, file must be located in context.getFilesDir()
  start_time datetime NOT NULL,			-- Start time of this file
  end_time datetime NOT NULL,			-- End time of this file
  file_type INTEGER NOT NULL,			-- Type of the file, can be one of the following: 0 (Debug log), 1 (Encrypted Qdmon dump)
  sms INTEGER NOT NULL,				-- Actually a boolean, indicates whether a binary/silent SMS was detected in this dump
  imsi_catcher INTEGER NOT NULL,		-- Actually a boolean, indicates whether an IMSI catcher was detected in this dump
  crash INTEGER NOT NULL,  			-- Actually a boolean, indicates whether a debug logfile contains a crash (Uncaught Exception or other fatal error)
  state INTEGER NOT NULL			-- State of the file, one of the constants defined in DumpFile.java (RECORDING, AVAILABLE, PENDING, UPLOADED, DELETED, RECORDING_PENDING)
);
