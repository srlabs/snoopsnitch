DROP TABLE IF EXISTS si_loc;
CREATE TABLE si_loc (
       id integer PRIMARY KEY,               -- Equal to the id in the corresponding session_info row
       valid integer,	  		     -- Actually a boolean with values 0/1, indicates whether the location is from within Constants.LOC_MAX_DELTA around the session timestamp
       latitude double,			     -- Coordinates
       longitude double			     -- Coordinates
);
