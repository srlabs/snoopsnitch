package de.srlabs.snoopsnitch.util;

import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import de.srlabs.snoopsnitch.BaseActivity;
import de.srlabs.snoopsnitch.analysis.AnalysisEvent;
import de.srlabs.snoopsnitch.analysis.Event;
import de.srlabs.snoopsnitch.qdmon.MsdServiceCallback;
import de.srlabs.snoopsnitch.qdmon.MsdServiceHelper;
import de.srlabs.snoopsnitch.qdmon.StateChangedReason;
import de.srlabs.snoopsnitch.upload.FileState;


public class MSDServiceHelperCreator implements MsdServiceCallback
{
	// Attributes
	private static MSDServiceHelperCreator _instance = null;
	private MsdServiceHelper msdServiceHelper;
	private int rectWidth;
	private Activity currentActivity;
	private boolean autostartRecordingPending = false;
	
	// Methods
	private MSDServiceHelperCreator (Context context, boolean autostartRecording) 
	{
		msdServiceHelper = new MsdServiceHelper(context, this);
		this.autostartRecordingPending = autostartRecording;
	}
	
	public static MSDServiceHelperCreator getInstance (Context context, boolean autostartRecording)
	{
		if (_instance == null)
		{
			_instance = new MSDServiceHelperCreator (context, autostartRecording);
		}
		
		return _instance;
	}
	
	public static MSDServiceHelperCreator getInstance ()
	{
		return _instance;
	}
	
	public void destroy ()
	{
		_instance = null;
	}
	
	public MsdServiceHelper getMsdServiceHelper ()
	{
		return msdServiceHelper;
	}
	
	private boolean[] vectorToUploadedBooleanArray(Vector<? extends AnalysisEvent> events){
		boolean[] result = new boolean[events.size()];
		for(int i=0;i<events.size();i++)
			result[i] = events.get(i).getUploadState() == FileState.STATE_UPLOADED;
		return result;
	}
	public boolean[] getThreatsSmsMonthSum ()
	{
		return vectorToUploadedBooleanArray(msdServiceHelper.getData().getEvent(TimeSpace.getTimeSpaceMonth().getStartTime(),
				TimeSpace.getTimeSpaceMonth().getEndTime()));
	}
	
	public boolean[][] getThreatsSmsMonth ()
	{
		boolean[][] smsMonth = new boolean[4][];
		long calStart = TimeSpace.getTimeSpaceMonth().getStartTime();
		long calEnd = TimeSpace.getTimeSpaceMonth().getEndTime();
		
		long timeSpan = (calEnd - calStart) / 4;
		
		for (int i=0; i<smsMonth.length; i++)
		{
			smsMonth[i] = vectorToUploadedBooleanArray(msdServiceHelper.getData().getEvent(calEnd - timeSpan, calEnd));
			calEnd -= timeSpan;
		}
		
		return smsMonth;
	}
	
	public boolean[] getThreatsSmsWeekSum ()
	{
		return vectorToUploadedBooleanArray(msdServiceHelper.getData().getEvent(TimeSpace.getTimeSpaceWeek().getStartTime(),
				TimeSpace.getTimeSpaceWeek().getEndTime()));
	}
	
	public boolean[][] getThreatsSmsWeek ()
	{
		boolean[][] smsWeek = new boolean[7][];
		long calEnd = TimeSpace.getTimeSpaceWeek().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceWeek().getEndTime() - TimeSpace.getTimeSpaceWeek().getStartTime()) / 7;
		
		for (int i=0; i<smsWeek.length; i++)
		{
			smsWeek[i] = vectorToUploadedBooleanArray(msdServiceHelper.getData().getEvent(calEnd - timeSpan, calEnd));
			calEnd = (calEnd - timeSpan);
		}
		
		return smsWeek;
	}
	
	public boolean[][] getThreatsSmsDay ()
	{		
		boolean[][] smsDay = new boolean[6][];
		long calEnd = TimeSpace.getTimeSpaceDay().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceDay().getEndTime() - TimeSpace.getTimeSpaceDay().getStartTime()) / 6;
		
		for (int i=0; i<smsDay.length; i++)
		{
			smsDay[i] = vectorToUploadedBooleanArray(msdServiceHelper.getData().getEvent(calEnd - timeSpan, calEnd));
			calEnd = (calEnd - timeSpan);
		}
		
		return smsDay;
	}
	
	public boolean[] getThreatsSmsDaySum ()
	{	
		return vectorToUploadedBooleanArray(msdServiceHelper.getData().getEvent(TimeSpace.getTimeSpaceDay().getStartTime(),
				TimeSpace.getTimeSpaceDay().getEndTime()));
	}
	
	public boolean[][] getThreatsSmsHour ()
	{
		boolean[][] smsHour = new boolean[12][];
		long calEnd = TimeSpace.getTimeSpaceHour().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceHour().getEndTime() - TimeSpace.getTimeSpaceHour().getStartTime()) / 12;
		
		for (int i=0; i<smsHour.length; i++)
		{
			smsHour[i] = vectorToUploadedBooleanArray(msdServiceHelper.getData().getEvent(calEnd - timeSpan, calEnd));
			calEnd -= timeSpan;
		}
		
		return smsHour;
	}
	
	public boolean[] getThreatsSmsHourSum ()
	{
		return vectorToUploadedBooleanArray(msdServiceHelper.getData().getEvent(TimeSpace.getTimeSpaceHour().getStartTime(),
				TimeSpace.getTimeSpaceHour().getEndTime()));
	}
	
	public boolean[] getThreatsImsiMonthSum ()
	{	
		return vectorToUploadedBooleanArray(msdServiceHelper.getData().getImsiCatchers(TimeSpace.getTimeSpaceMonth().getStartTime(),
				TimeSpace.getTimeSpaceMonth().getEndTime()));
	}
	
	public boolean[][] getThreatsImsiMonth ()
	{
		boolean[][] imsiMonth = new boolean[4][];
		long calEnd = TimeSpace.getTimeSpaceMonth().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceMonth().getEndTime() - TimeSpace.getTimeSpaceMonth().getStartTime()) / 4;
		
		for (int i=0; i<imsiMonth.length; i++)
		{
			imsiMonth[i] = vectorToUploadedBooleanArray(msdServiceHelper.getData().getImsiCatchers(calEnd - timeSpan, calEnd));
			calEnd -= timeSpan;
		}
		
		return imsiMonth;
	}
	
	public boolean[] getThreatsImsiWeekSum ()
	{		
		return vectorToUploadedBooleanArray(msdServiceHelper.getData().getImsiCatchers(TimeSpace.getTimeSpaceWeek().getStartTime(),
				TimeSpace.getTimeSpaceWeek().getEndTime()));
	}
	
	public boolean[][] getThreatsImsiWeek ()
	{
		boolean[][] imsiWeek = new boolean[7][];
		long calEnd = TimeSpace.getTimeSpaceWeek().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceWeek().getEndTime() - TimeSpace.getTimeSpaceWeek().getStartTime()) / 7;
		
		for (int i=0; i<imsiWeek.length; i++)
		{
			imsiWeek[i] = vectorToUploadedBooleanArray(msdServiceHelper.getData().getImsiCatchers(calEnd - timeSpan, calEnd));
			calEnd -= timeSpan;
		}
		
		return imsiWeek;
	}
	
	public boolean[][] getThreatsImsiDay ()
	{		
		boolean[][] imsiDay = new boolean[6][];
		long calEnd = TimeSpace.getTimeSpaceDay().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceDay().getEndTime() - TimeSpace.getTimeSpaceDay().getStartTime()) / 6;
		
		for (int i=0; i<imsiDay.length; i++)
		{
			imsiDay[i] = vectorToUploadedBooleanArray(msdServiceHelper.getData().getImsiCatchers(calEnd - timeSpan, calEnd));
			calEnd = (calEnd - timeSpan);
		}
		
		return imsiDay;
	}
	
	public boolean[] getThreatsImsiDaySum ()
	{		
		return vectorToUploadedBooleanArray(msdServiceHelper.getData().getImsiCatchers(TimeSpace.getTimeSpaceDay().getStartTime(),
				TimeSpace.getTimeSpaceDay().getEndTime()));
	}
	
	public boolean[][] getThreatsImsiHour ()
	{
		boolean[][] imsiHour = new boolean[12][];
		long calEnd = TimeSpace.getTimeSpaceHour().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceHour().getEndTime() - TimeSpace.getTimeSpaceHour().getStartTime()) / 12;
		
		for (int i=0; i<imsiHour.length; i++)
		{
			imsiHour[i] = vectorToUploadedBooleanArray(msdServiceHelper.getData().getImsiCatchers(calEnd - timeSpan, calEnd));
			calEnd = (calEnd - timeSpan);
		}
		
		return imsiHour;
	}
	
	public boolean[] getThreatsImsiHourSum ()
	{
		return vectorToUploadedBooleanArray(msdServiceHelper.getData().getImsiCatchers(TimeSpace.getTimeSpaceHour().getStartTime(),
				TimeSpace.getTimeSpaceHour().getEndTime()));
	}
	
	public Vector<Event> getEventOfType (Event.Type type, long startTime, long endTime)
	{
		Vector<Event> event = msdServiceHelper.getData().getEvent(startTime, endTime);
		
		for (Event s : msdServiceHelper.getData().getEvent(startTime, endTime)) 
		{
			if (type != Event.Type.INVALID_EVENT && !s.getType().equals(type))
			{
				event.remove(s);
			}
		}
		
		return event;
	}
	
	public void setRectWidth (int rectWidth)
	{
		this.rectWidth = rectWidth;
	}
	
	public int getRectWidth ()
	{
		return rectWidth;
	}
	
	public void setCurrentActivity (Activity activity)
	{
		this.currentActivity = activity;
	}

	@Override
	public void stateChanged(StateChangedReason reason) 
	{
		if(autostartRecordingPending && msdServiceHelper.isConnected()){
			if(!msdServiceHelper.isRecording())
				msdServiceHelper.startRecording();
			autostartRecordingPending = false;
		}
		try
		{
			((BaseActivity) currentActivity).stateChanged(reason);
		}
		catch (Exception e)
		{
			// TODO: Log output...
		}
	}

	@Override
	public void internalError(String msg) 
	{
		if (currentActivity instanceof BaseActivity)
		{
			((BaseActivity) currentActivity).internalError(msg);	
		}
	}
}
