package de.srlabs.snoopsnitch;

public class EncryptedFileWriterError extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Throwable e = null;

	public EncryptedFileWriterError (String message, Throwable e)
	{
		super(message);
		this.e = e;
	}

	public EncryptedFileWriterError (Throwable e)
	{
		this.e = e;
	}

	public EncryptedFileWriterError (String message)
	{
		super(message);
	}
	
	public Throwable getE() {
		return e;
	}

}