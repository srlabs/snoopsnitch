package de.srlabs.msd.analysis;


public class SMS {
	private long timestamp;
	private long id;
	private int mcc;
	private int mnc;
	private int lac;
	private int cid;
	private String sender;
	public enum Type {
		SILENT_SMS,
		BINARY_SMS
		// TODO: Define the required types here
	}
	private Type type;
	public SMS() {
	}
	public SMS(long timestamp, long id, int mcc, int mnc, int lac, int cid,
			String sender, Type type) {
		super();
		this.timestamp = timestamp;
		this.id = id;
		this.mcc = mcc;
		this.mnc = mnc;
		this.lac = lac;
		this.cid = cid;
		this.sender = sender;
		this.type = type;
	}

	/**
	 * Timestamp when the silent SMS was received, in millis since 1970
	 * @return
	 */
	public long getTimestamp() {
		return timestamp;
	}
	/**
	 * id column of the silent sms in table session_info, can be used to retrieve the SMS again using get(long id)
	 * @return
	 */
	public long getId() {
		return id;
	}
	/**
	 * MCC when the SMS was received
	 * @return
	 */
	public int getMcc() {
		return mcc;
	}
	/**
	 * MNC when the SMS was received
	 * @return
	 */
	public int getMnc() {
		return mnc;
	}
	public String getSender() {
		return sender;
	}
	/**
	 * Gets the type of this silent/binary SMS
	 * @return
	 */
	public Type getType() {
		return type;
	}
	public int getLac() {
		return lac;
	}
	public int getCid() {
		return cid;
	}
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("SilentSms: ID=" + id + " TYPE=" + type.name());
		// TODO: Add more fields
		return result.toString();
	}
	
}
