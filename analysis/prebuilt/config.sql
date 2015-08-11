--  Configuration
DROP TABLE IF EXISTS config;
CREATE TABLE config
(
		--  The maximum value for the cell reselect offset CRO we
		--  consider unsuspicious.
		cro_max     int,

		--  The minimum value for the cell reselect timeout we
		--  consider unsuspicious.
		t3212_min   int,

		--  The number of milliseconds a traffic channel can at most
		--  be idle (i.e. without anything useful happening) under
		--  normal circumstances.
		delta_tch   int,

		--  Delay between CIPHER MODE COMMAND and CIPHER MODE COMPLETE
		--  that is considered unsuspicious.
		delta_cmcp  int,

		--  Maximum number of paging groups we consider unsuspicious.
		--  See R2 on how this is used.
		n_norm      int,

		--  Time in seconds we assume is needed to move from one place
		--  an ARFCN is used by a cell to another place where the same
		--  ARFCN is used for a *different* cell.
		delta_arfcn int,

		--  The minimum type 1 paging rate we consider unsuspicious when
		--  no call is placed.
		min_pag1_rate float,

		--  Minimum score we treat an analysis result as a catcher
		catcher_min_score float,

		--  Maximum time delta of a location entry in seconds to be
		--  considered related to some session entry
		loc_max_delta,

		--  Maximum time delta of a cell_info entry in seconds to be
		--  considered related to some session entry
		cell_info_max_delta,

		--  Maximum time delta of a neighbor info entry in seconds to be
		--  considered related to some cell_info entry
		neig_max_delta
);

INSERT INTO config VALUES
(
		-- cro_max
		-- According to the X-GOLD analysis, CRO values of 40 are very common
		-- (> 20%), but only 2.5% of the samples have a CRO value of >40.
		-- Cf. X-GOLD_cro.pdf
		40,

		-- t3212_min
		-- The minimum T3212 values observed in the X-GOLD data is 10, this
		-- aligns with our other measurements. Cf. X-GOLD_t3212.pdf
		10,

		-- delta_tch
		-- According to data, session hardly ever last longer than 8
		-- seconds. Cf. GSMMap_orphaned_time.pdf
		-- FIXME: Further analysis is required on the other reasons for or
		-- orphaned traffic channels. We need to look at the session data.
		8000,

		-- delta_cmcp
		-- According to GSMMap data, only about 2% of cipher setups take
		-- longer than than 2 seconds. Cf. GSMMap_cipher_times.pdf
		2000,

		-- n_norm
		-- Number of paging groups does not seem to be a very good indicator
		-- for an IMSI catcher, as operators seem to tend to have many paging
		-- groups, too. In the E-Plus network we see 72 paging groups often.
		-- In X-GOLD sample we find 5% of sample with 80 paging groups.
		-- Cf. *_paging_groups.pdf
		80,

		-- delta_arfcn
		-- This is unsupported right now, and we need other sample data.
		0,

		-- min_pag1_rate
		-- It looks like this is no good indicator for an IMSI catcher, as
		-- even our self-made catcher as a paging rate of significanty > 0
		-- (around 0.8) which is normal in some networks. We need to check
		-- whether we really measure the right thing. Also, if a call is
		-- placed there are no pagings and if the call starts somewhere in
		-- the middle of our sampling interval, the value can be low (but
		-- above 0).
		0.0,

		-- catcher_min_score
		3.1,

		-- loc_max_delta
		600,

		-- cell_info_max_delta
		-- Consider everything occuring within an hour (+/- 1800 seconds)
		1800,

		-- neig_max_delta
		600
);
