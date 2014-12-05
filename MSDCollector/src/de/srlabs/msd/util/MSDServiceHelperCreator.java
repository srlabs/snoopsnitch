package de.srlabs.msd.util;

import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
import de.srlabs.msd.BaseActivity;
import de.srlabs.msd.DashboardActivity;
import de.srlabs.msd.analysis.SMS;
import de.srlabs.msd.qdmon.MsdServiceCallback;
import de.srlabs.msd.qdmon.MsdServiceHelper;
import de.srlabs.msd.qdmon.StateChangedReason;


public class MSDServiceHelperCreator implements MsdServiceCallback
{
	// Attributes
	private static MSDServiceHelperCreator _instance = null;
	private MsdServiceHelper msdServiceHelper;
	private int rectWidth;
	private Context context;
	private Activity currentActivity;
	
	// Methods
	private MSDServiceHelperCreator (Context context) 
	{
		msdServiceHelper = new MsdServiceHelper(context, this, true);
		this.context = context;
	}
	
	public static MSDServiceHelperCreator getInstance (Context context)
	{
		if (_instance == null)
		{
			_instance = new MSDServiceHelperCreator (context);
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
		return msdServiceHelper.getData().getSMS(TimeSpace.getTimeSpaceMonth().getStartTime(), 
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
			smsMonth[i] = msdServiceHelper.getData().getSMS(calEnd - timeSpan,  calEnd).size();
			calEnd -= timeSpan;
		}
		
		return smsMonth;
	}
	
	public int getThreatsSmsWeekSum ()
	{
		return msdServiceHelper.getData().getSMS(TimeSpace.getTimeSpaceWeek().getStartTime(), 
				TimeSpace.getTimeSpaceWeek().getEndTime()).size();
	}
	
	public int[] getThreatsSmsWeek ()
	{
		int[] smsWeek = new int[7];
		long calStart = TimeSpace.getTimeSpaceWeek().getStartTime();
		long calEnd = TimeSpace.getTimeSpaceWeek().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceWeek().getEndTime() - TimeSpace.getTimeSpaceWeek().getStartTime()) / 7;
		
		for (int i=0; i<smsWeek.length; i++)
		{
			smsWeek[i] = msdServiceHelper.getData().getSMS(calEnd - timeSpan, calEnd).size();
			calEnd = (calEnd - timeSpan);
		}
		
		return smsWeek;
	}
	
	public int[] getThreatsSmsDay ()
	{		
		int[] smsDay = new int[6];
		long calStart = TimeSpace.getTimeSpaceDay().getStartTime();
		long calEnd = TimeSpace.getTimeSpaceDay().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceDay().getEndTime() - TimeSpace.getTimeSpaceDay().getStartTime()) / 6;
		
		for (int i=0; i<smsDay.length; i++)
		{
			smsDay[i] = msdServiceHelper.getData().getSMS(calEnd - timeSpan, calEnd).size();
			calEnd = (calEnd - timeSpan);
		}
		
		return smsDay;
	}
	
	public int getThreatsSmsDaySum ()
	{	
		/*
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		
		return msdServiceHelper.getSMS(cal.getTimeInMillis(), Calendar.getInstance().getTimeInMillis()).size();
		*/
		
		return msdServiceHelper.getData().getSMS(TimeSpace.getTimeSpaceDay().getStartTime(), 
				TimeSpace.getTimeSpaceDay().getEndTime()).size();
	}
	
	public int[] getThreatsSmsHour ()
	{
		int[] smsHour = new int[12];
		long calStart = TimeSpace.getTimeSpaceHour().getStartTime();
		long calEnd = TimeSpace.getTimeSpaceHour().getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceHour().getEndTime() - TimeSpace.getTimeSpaceHour().getStartTime()) / 12;
		
		for (int i=0; i<smsHour.length; i++)
		{
			smsHour[i] = msdServiceHelper.getData().getSMS(calEnd - timeSpan, calEnd).size();
			calEnd -= timeSpan;
		}
		
		return smsHour;
	}
	
	public int getThreatsSmsHourSum ()
	{
		return msdServiceHelper.getData().getSMS(TimeSpace.getTimeSpaceHour().getStartTime(), 
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
		long calStart = TimeSpace.getTimeSpaceMonth().getStartTime();
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
		return msdServiceHelper.getData().getImsiCatchers(TimeSpace.Times.Week.getStartTime(), 
				TimeSpace.Times.Week.getStartTime()).size();
	}
	
	public int[] getThreatsImsiWeek ()
	{
		int[] imsiWeek = new int[7];
		long calStart = TimeSpace.getTimeSpaceWeek().getStartTime();
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
		long calStart = TimeSpace.getTimeSpaceDay().getStartTime();
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
		return msdServiceHelper.getData().getImsiCatchers(TimeSpace.Times.Day.getStartTime(), 
				TimeSpace.Times.Day.getStartTime()).size();
	}
	
	public int[] getThreatsImsiHour ()
	{
		int[] imsiHour = new int[12];
		long calStart = TimeSpace.Times.Hour.getStartTime();
		long calEnd = TimeSpace.Times.Hour.getEndTime();
		long timeSpan = (TimeSpace.getTimeSpaceHour().getEndTime() - TimeSpace.getTimeSpaceHour().getStartTime()) / 4;
		
		for (int i=0; i<imsiHour.length; i++)
		{
			imsiHour[i] = msdServiceHelper.getData().getImsiCatchers(calEnd - timeSpan, calEnd).size();
			calEnd = (calEnd - timeSpan);
		}
		
		return imsiHour;
	}
	
	public int getThreatsImsiHourSum ()
	{
		return msdServiceHelper.getData().getImsiCatchers(TimeSpace.Times.Hour.getStartTime(), 
				TimeSpace.Times.Hour.getEndTime()).size();
	}
	
	public Vector<SMS> getSmsOfType (SMS.Type type, long startTime, long endTime)
	{
		Vector<SMS> sms = msdServiceHelper.getData().getSMS(startTime, endTime);
		
		for (SMS s : msdServiceHelper.getData().getSMS(startTime, endTime)) 
		{
			if (!s.getType().equals(type))
			{
				sms.remove(s);
			}
		}
		
		return sms;
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
