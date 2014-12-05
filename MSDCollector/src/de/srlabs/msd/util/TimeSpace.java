package de.srlabs.msd.util;

import java.util.Calendar;

import android.nfc.cardemulation.OffHostApduService;

public class TimeSpace 
{
	public enum Times 
	{
		Month,
		Week,
		Day,
		Hour;
		
		// Attributes
		private long startTime;
		private long endTime;
		
		// Constructor
		public long getStartTime ()
		{
			return startTime;
		}
		
		private void setStartTime (long startTime)
		{
			this.startTime = startTime;
		}
		
		public long getEndTime ()
		{
			return endTime;
		}
		
		private void setEndTime (long endTime)
		{
			this.endTime = endTime;
		}
	}
	
	static Times getTimeSpaceMonth ()
	{
		Calendar calEnd = Calendar.getInstance();
		calEnd.add(Calendar.DATE, -8);
		calEnd.set(Calendar.HOUR_OF_DAY, 23);
		calEnd.set(Calendar.MINUTE, 59);
		calEnd.set(Calendar.SECOND, 59);
		
		Calendar calStart = (Calendar) calEnd.clone();
		calStart.add(Calendar.DATE, -28);
		
		Times.Month.setStartTime(calStart.getTimeInMillis());
		Times.Month.setEndTime(calEnd.getTimeInMillis());
		
		return Times.Month;
	}
	
	static Times getTimeSpaceWeek ()
	{
		Calendar calEnd = Calendar.getInstance();
		calEnd.add(Calendar.DATE, -1);
		calEnd.set(Calendar.HOUR_OF_DAY, 23);
		calEnd.set(Calendar.MINUTE, 59);
		calEnd.set(Calendar.SECOND, 59);
		
		Calendar calStart = (Calendar) calEnd.clone();
		calStart.add(Calendar.DATE, -7);
		
		Times.Week.setStartTime(calStart.getTimeInMillis());
		Times.Week.setEndTime(calEnd.getTimeInMillis());
		
		return Times.Week;
	}
	
	static Times getTimeSpaceDay ()
	{
    	Calendar calEnd = Calendar.getInstance();
    	int offset = 5;
    	
    	if ((Calendar.MINUTE)%5 != 0)
    	{
        	offset = 5-(calEnd.get(Calendar.MINUTE)%5);	
    	}
    	
    	calEnd.add(Calendar.MINUTE, offset);
		Calendar calStart = (Calendar) calEnd.clone();
		calStart.add(Calendar.HOUR, -24);
		
		Times.Day.setStartTime(calStart.getTimeInMillis());
		Times.Day.setEndTime(calEnd.getTimeInMillis());
		
		return Times.Day;
	}
	
	static Times getTimeSpaceHour ()
	{
    	Calendar calEnd = Calendar.getInstance();
    	int offsetMin = 5;
    	int offsetSec = 0;
    	
    	if ((Calendar.MINUTE)%5 != 0)
    	{
        	offsetMin = 5-(calEnd.get(Calendar.MINUTE)%5);	
    	}
    	
    	if ((Calendar.SECOND)%5 != 0)
    	{
        	offsetSec = 60-(calEnd.get(Calendar.SECOND)%5);	
    	}
    	
    	calEnd.add(Calendar.MINUTE, offsetMin);
    	calEnd.add(Calendar.SECOND, offsetSec);
		Calendar calStart = (Calendar) calEnd.clone();
		calStart.add(Calendar.HOUR, -1);

		
		Times.Hour.setStartTime(calStart.getTimeInMillis());
		Times.Hour.setEndTime(calEnd.getTimeInMillis());
		
		return Times.Hour;
	}
}
