-- Anonymize sensible fields in session_info

update session_info set imsi = substr(imsi,1,6)||"<ANON>";	-- Strip IMSIs

update session_info set imei = substr(imei,1,6)||"<ANON>";	-- Strip IMEIs

update session_info set msisdn = substr(msisdn,1,6)||"<ANON>";	-- Strip phone numbers

-- Anonymize sensible fields in sms_meta

update sms_meta set data = "<ANON>"			-- Strip data if:
where	(alphabet = 1 or alphabet = 2) and		-- text SMS only
	length > 0 and ota = 0 and			-- not empty or OTA
	src_port = 0 and dst_port = 0 and		-- no port addressing
	(pid != 64) and					-- not possibly silent
	(dcs < 192 or dcs >= 208);			-- not certainly silent

update sms_meta set data = "<WAPPUSH>"			-- Strip data if:
where	from_network and				-- message comes from network
	dst_port = 2948 and				-- addresses WAP-PUSH
	length > 6;					-- not empty

update sms_meta set msisdn = substr(msisdn,1,6)||"<ANON>"	-- Strip phone number if:
	-- a number is stored
where	length(msisdn) > 0 and
	-- marked anon before
	data = "<ANON>";

