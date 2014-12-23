package de.srlabs.snoopsnitch.util;

import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import de.srlabs.snoopsnitch.BaseActivity;
import de.srlabs.snoopsnitch.analysis.Event;
import de.srlabs.snoopsnitch.qdmon.MsdServiceCallback;
import de.srlabs.snoopsnitch.qdmon.MsdServiceHelper;
import de.srlabs.snoopsnitch.qdmon.StateChangedReason;


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
		msdServiceHelper = new MsdServiceHelper(context, this, false);
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
	
	public int getThreatsSmsMonthSum ()
	{
		return msdServiceHelper.getData().getEvent(TimeSpace.getTimeSpaceMonth().getStartTime(), 
				TimeSpace.getTimeSpaceMonth().getEndTime()).size();
	}
	
	public int[] getThreatsSmsMonth ()
	{
		int[] smsMonth = new int[4];
		long calStart = TimeSpace.getTimeSpaceMonth().getStartTime();
		long calEnd = TimeSpace.getTimeSpaceMonth().getEndTime();
		
		long timeSpan = (calEnd - calStart) / 4;
		
		for (int i=0; i<smsMonth.length; i++)
		{
			smsMonth[i] = msdServiceHelper.getData().getEvent(calEnd - timeSpan,  calEnd).size();
			calEnd -= timeSpan;
		}
		
		return smsMonth;
	}
	
	public int getThreatsSmsWeekSum ()
	{
		return msdServiceHelper.getData().getEvent(TimeSpace.getTimeSpaceWeek().getStartTime(), 
				TimeSpace.getTimeSpaceWeek().getEndTime()).size();
	}
	
	public int[] getThreatsSmsWeek ()
	{
		int[] smsWeek = new int[7];
		long calEnd = TimeSpace.getTimeSpaceWeek().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceWeek().getEndTime() - TimeSpace.getTimeSpaceWeek().getStartTime()) / 7;
		
		for (int i=0; i<smsWeek.length; i++)
		{
			smsWeek[i] = msdServiceHelper.getData().getEvent(calEnd - timeSpan, calEnd).size();
			calEnd = (calEnd - timeSpan);
		}
		
		return smsWeek;
	}
	
	public int[] getThreatsSmsDay ()
	{		
		int[] smsDay = new int[6];
		long calEnd = TimeSpace.getTimeSpaceDay().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceDay().getEndTime() - TimeSpace.getTimeSpaceDay().getStartTime()) / 6;
		
		for (int i=0; i<smsDay.length; i++)
		{
			smsDay[i] = msdServiceHelper.getData().getEvent(calEnd - timeSpan, calEnd).size();
			calEnd = (calEnd - timeSpan);
		}
		
		return smsDay;
	}
	
	public int getThreatsSmsDaySum ()
	{	
		return msdServiceHelper.getData().getEvent(TimeSpace.getTimeSpaceDay().getStartTime(), 
				TimeSpace.getTimeSpaceDay().getEndTime()).size();
	}
	
	public int[] getThreatsSmsHour ()
	{
		int[] smsHour = new int[12];
		long calEnd = TimeSpace.getTimeSpaceHour().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceHour().getEndTime() - TimeSpace.getTimeSpaceHour().getStartTime()) / 12;
		
		for (int i=0; i<smsHour.length; i++)
		{
			smsHour[i] = msdServiceHelper.getData().getEvent(calEnd - timeSpan, calEnd).size();
			calEnd -= timeSpan;
		}
		
		return smsHour;
	}
	
	public int getThreatsSmsHourSum ()
	{
		return msdServiceHelper.getData().getEvent(TimeSpace.getTimeSpaceHour().getStartTime(), 
				TimeSpace.getTimeSpaceHour().getEndTime()).size();
	}
	
	public int getThreatsImsiMonthSum ()
	{	
		return msdServiceHelper.getData().getImsiCatchers(TimeSpace.getTimeSpaceMonth().getStartTime(), 
				TimeSpace.getTimeSpaceMonth().getEndTime()).size();
	}
	
	public int[] getThreatsImsiMonth ()
	{
		int[] imsiMonth = new int[4];
		long calEnd = TimeSpace.getTimeSpaceMonth().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceMonth().getEndTime() - TimeSpace.getTimeSpaceMonth().getStartTime()) / 4;
		
		for (int i=0; i<imsiMonth.length; i++)
		{
			imsiMonth[i] = msdServiceHelper.getData().getImsiCatchers(calEnd - timeSpan, calEnd).size();
			calEnd -= timeSpan;
		}
		
		return imsiMonth;
	}
	
	public int getThreatsImsiWeekSum ()
	{		
		return msdServiceHelper.getData().getImsiCatchers(TimeSpace.getTimeSpaceWeek().getStartTime(), 
				TimeSpace.getTimeSpaceWeek().getEndTime()).size();
	}
	
	public int[] getThreatsImsiWeek ()
	{
		int[] imsiWeek = new int[7];
		long calEnd = TimeSpace.getTimeSpaceWeek().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceWeek().getEndTime() - TimeSpace.getTimeSpaceWeek().getStartTime()) / 7;
		
		for (int i=0; i<imsiWeek.length; i++)
		{
			imsiWeek[i] = msdServiceHelper.getData().getImsiCatchers(calEnd - timeSpan, calEnd).size();
			calEnd -= timeSpan;
		}
		
		return imsiWeek;
	}
	
	public int[] getThreatsImsiDay ()
	{		
		int[] imsiDay = new int[6];
		long calEnd = TimeSpace.getTimeSpaceDay().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceDay().getEndTime() - TimeSpace.getTimeSpaceDay().getStartTime()) / 6;
		
		for (int i=0; i<imsiDay.length; i++)
		{
			imsiDay[i] = msdServiceHelper.getData().getImsiCatchers(calEnd - timeSpan, calEnd).size();
			calEnd = (calEnd - timeSpan);
		}
		
		return imsiDay;
	}
	
	public int getThreatsImsiDaySum ()
	{		
		return msdServiceHelper.getData().getImsiCatchers(TimeSpace.getTimeSpaceDay().getStartTime(), 
				TimeSpace.getTimeSpaceDay().getEndTime()).size();
	}
	
	public int[] getThreatsImsiHour ()
	{
		int[] imsiHour = new int[12];
		long calEnd = TimeSpace.getTimeSpaceHour().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceHour().getEndTime() - TimeSpace.getTimeSpaceHour().getStartTime()) / 12;
		
		for (int i=0; i<imsiHour.length; i++)
		{
			imsiHour[i] = msdServiceHelper.getData().getImsiCatchers(calEnd - timeSpan, calEnd).size();
			calEnd = (calEnd - timeSpan);
		}
		
		return imsiHour;
	}
	
	public int getThreatsImsiHourSum ()
	{
		return msdServiceHelper.getData().getImsiCatchers(TimeSpace.getTimeSpaceHour().getStartTime(), 
				TimeSpace.getTimeSpaceHour().getEndTime()).size();
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
