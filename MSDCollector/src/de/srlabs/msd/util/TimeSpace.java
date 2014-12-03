package de.srlabs.msd.util;

import java.util.Calendar;

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
		Calendar calStart = Calendar.getInstance();
		calStart.add(Calendar.DATE, -35);
		Calendar calEnd = Calendar.getInstance();
		calEnd.add(Calendar.DATE, -8);
		
		Times.Month.setStartTime(calStart.getTimeInMillis());
		Times.Month.setEndTime(calEnd.getTimeInMillis());
		
		return Times.Month;
	}
	
	static Times getTimeSpaceWeek ()
	{
		Calendar calStart = Calendar.getInstance();
		calStart.add(Calendar.DATE, -8);
		Calendar calEnd = Calendar.getInstance();
		calEnd.add(Calendar.DATE, -1);
		
		Times.Week.setStartTime(calStart.getTimeInMillis());
		Times.Week.setEndTime(calEnd.getTimeInMillis());
		
		return Times.Week;
	}
	
	static Times getTimeSpaceDay ()
	{
		Calendar calStart = Calendar.getInstance();
		calStart.add(Calendar.HOUR, -25);
		Calendar calEnd = Calendar.getInstance();
		calEnd.add(Calendar.HOUR, -1);
		
		Times.Day.setStartTime(calStart.getTimeInMillis());
		Times.Day.setEndTime(calEnd.getTimeInMillis());
		
		return Times.Day;
	}
	
	static Times getTimeSpaceHour ()
	{
		Calendar calStart = Calendar.getInstance();
		calStart.add(Calendar.HOUR, -1);
		
		Times.Hour.setStartTime(calStart.getTimeInMillis());
		Times.Hour.setEndTime(Calendar.getInstance().getTimeInMillis());
		
		return Times.Hour;
	}
}
