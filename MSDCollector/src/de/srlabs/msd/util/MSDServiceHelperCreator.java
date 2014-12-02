package de.srlabs.msd.util;

import java.util.Calendar;
import java.util.Vector;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import de.srlabs.msd.analysis.SMS;
import de.srlabs.msd.qdmon.MsdServiceHelper;
import de.srlabs.msd.qdmon.MsdServiceCallback;


public class MSDServiceHelperCreator
{
	// Attributes
	private static MSDServiceHelperCreator _instance = null;
	private MsdServiceHelper msdServiceHelper;
	private int rectWidth;
	
	// Methods
	public MSDServiceHelperCreator (Context context, MsdServiceCallback callback) 
	{

		msdServiceHelper = new MsdServiceHelper(context, callback, false);
	}
	
	public static MSDServiceHelperCreator getInstance (Context context, MsdServiceCallback callback)
	{
		if (_instance == null)
		{
			_instance = new MSDServiceHelperCreator (context, callback);
		}
		
		return _instance;
	}
	
	public static MSDServiceHelperCreator getInstance (MsdServiceCallback callback)
	{
		if (_instance == null)
		{
			_instance = new MSDServiceHelperCreator (null, callback);
		}
		
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
		/*
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		
		return msdServiceHelper.getSMS(cal.getTimeInMillis(), Calendar.getInstance().getTimeInMillis()).size();
		*/
		
		return msdServiceHelper.getData().getSMS(TimeSpace.getTimeSpaceMonth().getStartTime(), 
				TimeSpace.getTimeSpaceMonth().getEndTime()).size();
	}
	
	public int[] getThreatsSmsMonth ()
	{
		int[] smsMonth = new int[4];
		Calendar calStart = Calendar.getInstance();
		Calendar calEnd = Calendar.getInstance();
		
		long timeSpan = (TimeSpace.getTimeSpaceMonth().getEndTime() - TimeSpace.getTimeSpaceMonth().getStartTime()) / 4;
		
		for (int i=0; i<smsMonth.length; i++)
		{
			smsMonth[i] = msdServiceHelper.getData().getSMS(calStart.getTimeInMillis() - timeSpan, calEnd.getTimeInMillis()).size();
			calStart.setTimeInMillis(calStart.getTimeInMillis() - timeSpan);
			calEnd.setTimeInMillis(calEnd.getTimeInMillis() - timeSpan);
		}
		
		return smsMonth;
	}
	
	public int getThreatsSmsWeekSum ()
	{
		/*
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -7);
		
		return msdServiceHelper.getSMS(cal.getTimeInMillis(), Calendar.getInstance().getTimeInMillis()).size();
		*/
		
		return msdServiceHelper.getData().getSMS(TimeSpace.getTimeSpaceWeek().getStartTime(), 
				TimeSpace.getTimeSpaceWeek().getEndTime()).size();
	}
	
	public int[] getThreatsSmsWeek ()
	{
		int[] smsWeek = new int[7];
		Calendar calStart = Calendar.getInstance();
		Calendar calEnd = Calendar.getInstance();
		long timeSpan = (TimeSpace.getTimeSpaceWeek().getEndTime() - TimeSpace.getTimeSpaceWeek().getStartTime()) / 7;
		
		for (int i=0; i<smsWeek.length; i++)
		{
			smsWeek[i] = msdServiceHelper.getData().getSMS(calStart.getTimeInMillis() - timeSpan, calEnd.getTimeInMillis()).size();
			calStart.setTimeInMillis(calStart.getTimeInMillis() - timeSpan);
			calEnd.setTimeInMillis(calEnd.getTimeInMillis() - timeSpan);
		}
		
		return smsWeek;
	}
	
	public int[] getThreatsSmsDay ()
	{		
		int[] smsDay = new int[6];
		Calendar calStart = Calendar.getInstance();
		Calendar calEnd = Calendar.getInstance();
		long timeSpan = (TimeSpace.getTimeSpaceDay().getEndTime() - TimeSpace.getTimeSpaceDay().getStartTime()) / 6;
		
		for (int i=0; i<smsDay.length; i++)
		{
			smsDay[i] = msdServiceHelper.getData().getSMS(calStart.getTimeInMillis() - timeSpan, calEnd.getTimeInMillis()).size();
			calStart.setTimeInMillis(calStart.getTimeInMillis() - timeSpan);
			calEnd.setTimeInMillis(calEnd.getTimeInMillis() - timeSpan);
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
		Calendar calStart = Calendar.getInstance();
		Calendar calEnd = Calendar.getInstance();
		long timeSpan = (TimeSpace.getTimeSpaceHour().getEndTime() - TimeSpace.getTimeSpaceHour().getStartTime()) / 12;
		
		for (int i=0; i<smsHour.length; i++)
		{
			smsHour[i] = msdServiceHelper.getData().getSMS(calStart.getTimeInMillis() - timeSpan, calEnd.getTimeInMillis()).size();
			calStart.setTimeInMillis(calStart.getTimeInMillis() - timeSpan);
			calEnd.setTimeInMillis(calEnd.getTimeInMillis() - timeSpan);
		}
		
		return smsHour;
	}
	
	public int getThreatsSmsHourSum ()
	{
		/*
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR, -1);
		
		return msdServiceHelper.getSMS(cal.getTimeInMillis(), Calendar.getInstance().getTimeInMillis()).size();
		*/
		
		return msdServiceHelper.getData().getSMS(TimeSpace.getTimeSpaceHour().getStartTime(), 
				TimeSpace.getTimeSpaceHour().getEndTime()).size();
	}
	
	public int getThreatsImsiMonthSum ()
	{
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		
		return msdServiceHelper.getData().getImsiCatchers(cal.getTimeInMillis(), Calendar.getInstance().getTimeInMillis()).size();
	}
	
	public int[] getThreatsImsiMonth ()
	{
		int[] imsiMonth = new int[4];
		Calendar calStart = Calendar.getInstance();
		Calendar calEnd = Calendar.getInstance();
		long timeSpan = (TimeSpace.getTimeSpaceMonth().getEndTime() - TimeSpace.getTimeSpaceMonth().getStartTime()) / 4;
		
		for (int i=0; i<imsiMonth.length; i++)
		{
			imsiMonth[i] = msdServiceHelper.getData().getImsiCatchers(calStart.getTimeInMillis() - timeSpan, calEnd.getTimeInMillis()).size();
			calStart.setTimeInMillis(calStart.getTimeInMillis() - timeSpan);
			calEnd.setTimeInMillis(calEnd.getTimeInMillis() - timeSpan);
		}
		
		return imsiMonth;
	}
	
	public int getThreatsImsiWeekSum ()
	{
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -7);
		
		return msdServiceHelper.getData().getImsiCatchers(cal.getTimeInMillis(), Calendar.getInstance().getTimeInMillis()).size();
	}
	
	public int[] getThreatsImsiWeek ()
	{
		int[] imsiWeek = new int[7];
		Calendar calStart = Calendar.getInstance();
		Calendar calEnd = Calendar.getInstance();
		long timeSpan = (TimeSpace.getTimeSpaceWeek().getEndTime() - TimeSpace.getTimeSpaceWeek().getStartTime()) / 7;
		
		for (int i=0; i<imsiWeek.length; i++)
		{
			imsiWeek[i] = msdServiceHelper.getData().getImsiCatchers(calStart.getTimeInMillis() - timeSpan, calEnd.getTimeInMillis()).size();
			calStart.setTimeInMillis(calStart.getTimeInMillis() - timeSpan);
			calEnd.setTimeInMillis(calEnd.getTimeInMillis() - timeSpan);
		}
		
		return imsiWeek;
	}
	
	public int[] getThreatsImsiDay ()
	{		
		int[] imsiDay = new int[6];
		Calendar calStart = Calendar.getInstance();
		Calendar calEnd = Calendar.getInstance();
		long timeSpan = (TimeSpace.getTimeSpaceDay().getEndTime() - TimeSpace.getTimeSpaceDay().getStartTime()) / 6;
		
		for (int i=0; i<imsiDay.length; i++)
		{
			imsiDay[i] = msdServiceHelper.getData().getImsiCatchers(calStart.getTimeInMillis() - timeSpan, calEnd.getTimeInMillis()).size();
			calStart.setTimeInMillis(calStart.getTimeInMillis() - timeSpan);
			calEnd.setTimeInMillis(calEnd.getTimeInMillis() - timeSpan);
		}
		
		return imsiDay;
	}
	
	public int getThreatsImsiDaySum ()
	{
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		
		return msdServiceHelper.getData().getImsiCatchers(cal.getTimeInMillis(), Calendar.getInstance().getTimeInMillis()).size();
	}
	
	public int[] getThreatsImsiHour ()
	{
		int[] imsiHour = new int[12];
		Calendar calStart = Calendar.getInstance();
		Calendar calEnd = Calendar.getInstance();
		long timeSpan = (TimeSpace.getTimeSpaceHour().getEndTime() - TimeSpace.getTimeSpaceHour().getStartTime()) / 4;
		
		for (int i=0; i<imsiHour.length; i++)
		{
			imsiHour[i] = msdServiceHelper.getData().getImsiCatchers(calStart.getTimeInMillis() - timeSpan, calEnd.getTimeInMillis()).size();
			calStart.setTimeInMillis(calStart.getTimeInMillis() - timeSpan);
			calEnd.setTimeInMillis(calEnd.getTimeInMillis() - timeSpan);
		}
		
		return imsiHour;
	}
	
	public int getThreatsImsiHourSum ()
	{
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR, -1);
		
		return msdServiceHelper.getData().getImsiCatchers(cal.getTimeInMillis(), Calendar.getInstance().getTimeInMillis()).size();
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
}
