package de.srlabs.snoopsnitch.util;

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

		// End time, not including all smaller time spaces
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

		Times.Month.setEndTime(getTimeSpaceWeek().getStartTime());

		Calendar calStart = Calendar.getInstance();
		calStart.setTimeInMillis(Times.Month.getEndTime());
		calStart.add(Calendar.DATE, -28);
		Times.Month.setStartTime(calStart.getTimeInMillis());

		return Times.Month;
	}
	
	static Times getTimeSpaceWeek ()
	{
		//  Set end time of week period to start of this day
		Calendar endTime = Calendar.getInstance();
		endTime.setTimeInMillis(getTimeSpaceDay().getStartTime());
		endTime.set(Calendar.HOUR_OF_DAY, 23);
		endTime.set(Calendar.MINUTE, 59);
		endTime.set(Calendar.SECOND, 59);
		Times.Week.setEndTime(endTime.getTimeInMillis());

		Calendar calStart = Calendar.getInstance();
		calStart.setTimeInMillis(Times.Week.getEndTime());
		calStart.add(Calendar.DATE, -7);
		Times.Week.setStartTime(calStart.getTimeInMillis());

		return Times.Week;
	}
	
	static Times getTimeSpaceDay ()
	{
		Times.Day.setEndTime(getTimeSpaceHour().getStartTime());

		Calendar calStart = Calendar.getInstance();
		calStart.setTimeInMillis(Times.Day.getEndTime());
		calStart.add(Calendar.HOUR, -24);
		Times.Day.setStartTime(calStart.getTimeInMillis());

		return Times.Day;
	}
	
	static Times getTimeSpaceHour ()
	{
    	Calendar calEnd = Calendar.getInstance();
    	int offset = 5;
    	
    	if ((Calendar.MINUTE)%5 != 0)
    	{
        	offset = 5-(calEnd.get(Calendar.MINUTE)%5);	
    	}
    	
    	calEnd.add(Calendar.MINUTE, offset);
		Calendar calStart = (Calendar) calEnd.clone();
		calStart.add(Calendar.HOUR, -1);

		
		Times.Hour.setStartTime(calStart.getTimeInMillis());
		Times.Hour.setEndTime(calEnd.getTimeInMillis());
		
		return Times.Hour;
	}
}
