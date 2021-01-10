package com.schneeloch.bostonbusmap_library.data;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import android.os.Parcel;
import android.os.Parcelable;
import com.schneeloch.bostonbusmap_library.transit.TransitSystem;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TimeBounds implements Parcelable {
	private final ImmutableMap<Integer, TimeSpan> bounds;
	private final String routeTitle;
	
	private static final int MONDAY = 0x1;
	private static final int TUESDAY = 0x2;
	private static final int WEDNESDAY = 0x4;
	private static final int THURSDAY = 0x8;
	private static final int FRIDAY = 0x10;
	private static final int SATURDAY = 0x20;
	private static final int SUNDAY = 0x40;
	
	private static final int WEEKDAYS = MONDAY | TUESDAY | WEDNESDAY | THURSDAY | FRIDAY;
	
	private static final int[] boundsOrder = new int[] {
		WEEKDAYS, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
	};

	private TimeBounds(String routeTitle, Builder builder) {
		this.bounds = ImmutableMap.copyOf(builder.bounds);
		this.routeTitle = routeTitle;
	}
	
	public static class Builder
	{
		private final Map<Integer, TimeSpan> bounds = Maps.newHashMap();
		
		public void add(int weekdaysBits, int start, int end) {
			bounds.put(weekdaysBits, new TimeSpan(start, end));
		}

		public TimeBounds build(String routeTitle) {
			return new TimeBounds(routeTitle, this);
		}
	}
	
	private static class TimeSpan
	{
		public final int begin;
		public final int end;
		
		public TimeSpan(int begin, int end) {
			this.begin = begin;
			this.end = end;
		}
	}
	
	private static boolean doesRouteRunOnDayOfWeek(int dayOfWeek, int weekdayBits) {
		switch (dayOfWeek) {
		case Calendar.MONDAY:
			if ((weekdayBits & MONDAY) == 0) {
				return false;
			}
			break;
		case Calendar.TUESDAY:
		    if ((weekdayBits & TUESDAY) == 0) {
		        return false;
		    }
		    break;
		case Calendar.WEDNESDAY:
		    if ((weekdayBits & WEDNESDAY) == 0) {
		        return false;
		    }
		    break;
		case Calendar.THURSDAY:
		    if ((weekdayBits & THURSDAY) == 0) {
		        return false;
		    }
		    break;
		case Calendar.FRIDAY:
		    if ((weekdayBits & FRIDAY) == 0) {
		        return false;
		    }
		    break;
		case Calendar.SATURDAY:
		    if ((weekdayBits & SATURDAY) == 0) {
		        return false;
		    }
		    break;
		case Calendar.SUNDAY:
		    if ((weekdayBits & SUNDAY) == 0) {
		        return false;
		    }
		    break;
		default:
			throw new RuntimeException("Calendar.DAYOFWEEK returned unexpected value");
		}
		return true;
	}
	
	/**
	 * Checks if route is running at the given time
	 * @param calendar Some calendar, probably today's date, in 
	 * @return
	 */
	public boolean isRouteRunning(Calendar calendar) {
		if (bounds.size() == 0) {
			// this is a just in case measure
			return true;
		}
		
		int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
		
		int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
		int currentMinute = calendar.get(Calendar.MINUTE);
		int currentSecond = calendar.get(Calendar.SECOND);
		
		int secondsFromMidnight = currentSecond + 60*currentMinute + 60*60*currentHour;
		
		/**
		 * end can be greater than 24 hours, if the route ends at 1:30am for instance
		 * 
		 * check if secondsFromMidnight after start and before end for today's day of week
		 * also check for day of week = yesterday if end > 24 hours
		 */
		for	(Integer weekdayBitsObj : bounds.keySet()) {
			TimeSpan timeSpan = bounds.get(weekdayBitsObj);
			int weekdayBits = weekdayBitsObj;

			if (doesRouteRunOnDayOfWeek(dayOfWeek, weekdayBits)) {
				if (secondsFromMidnight >= timeSpan.begin && secondsFromMidnight < timeSpan.end) {
					return true;
				}
			}
			else if (timeSpan.end > 24*60*60) {
				int yesterday = calcYesterday(dayOfWeek);
				
				for (Integer yesterdayWeekdayBitsObj : bounds.keySet()) {
					int yesterdayWeekdayBits = yesterdayWeekdayBitsObj;
					if (doesRouteRunOnDayOfWeek(yesterday, yesterdayWeekdayBits)) {
						if (secondsFromMidnight < timeSpan.end - 24*60*60) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Like calendar.roll(Calendar.DAY_OF_WEEK, false), but without changing calendar object
	 * @param i
	 * @return
	 */
	private int calcYesterday(int today) {
		switch (today) {
		case Calendar.MONDAY:
			return Calendar.SUNDAY;
		case Calendar.TUESDAY:
			return Calendar.MONDAY;
		case Calendar.WEDNESDAY:
			return Calendar.TUESDAY;
		case Calendar.THURSDAY:
			return Calendar.WEDNESDAY;
		case Calendar.FRIDAY:
			return Calendar.THURSDAY;
		case Calendar.SATURDAY:
			return Calendar.FRIDAY;
		case Calendar.SUNDAY:
			return Calendar.SATURDAY;
			default:
				throw new RuntimeException("Unexpected day of week");
		}
	}

	public static com.schneeloch.bostonbusmap_library.data.TimeBounds.Builder builder() {
		return new com.schneeloch.bostonbusmap_library.data.TimeBounds.Builder();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(routeTitle);
		dest.writeInt(bounds.size());
		for (Integer weekdays : bounds.keySet()) {
			TimeSpan span = bounds.get(weekdays);
			dest.writeInt(weekdays);
			dest.writeInt(span.begin);
			dest.writeInt(span.end);
		}
	}
	
	public static final Parcelable.Creator<TimeBounds> CREATOR = new Creator<TimeBounds>() {

		@Override
		public TimeBounds createFromParcel(Parcel source) {
			Builder builder = new Builder();

			String route = source.readString();
			int size = source.readInt();
			for (int i = 0; i < size; i++) {
				int weekdays = source.readInt();
				int start = source.readInt();
				int end = source.readInt();
				builder.add(weekdays, start, end);
			}
			return builder.build(route);
		}

		@Override
		public TimeBounds[] newArray(int size) {
			return new TimeBounds[size];
		}
	};
	
	private void appendTimeBound(int weekdays, StringBuilder ret) {
		TimeSpan span = bounds.get(weekdays);
		List<String> dayStrings = Lists.newArrayList();
		if ((weekdays & WEEKDAYS) == WEEKDAYS) {
			dayStrings.add("Weekdays");
		}
		else { 
			if ((weekdays & MONDAY) != 0) {
				dayStrings.add("Monday");
			}
			if ((weekdays & TUESDAY) != 0) {
				dayStrings.add("Tuesday");
			}
			if ((weekdays & WEDNESDAY) != 0) {
				dayStrings.add("Wednesday");
			}
			if ((weekdays & THURSDAY) != 0) {
				dayStrings.add("Thursday");
			}
			if ((weekdays & FRIDAY) != 0) {
				dayStrings.add("Friday");
			}
		}
		
		if ((weekdays & SATURDAY) != 0) {
			dayStrings.add("Saturday");
		}
		if ((weekdays & SUNDAY) != 0) {
			dayStrings.add("Sunday");
		}
		
		ret.append(Joiner.on(", ").join(dayStrings)).append(" - ");
		ret.append(makeTimeString(span.begin)).append(" until ").append(makeTimeString(span.end)).append("<br />");

	}
	
	public String makeSnippet() {
		StringBuilder ret = new StringBuilder("Schedule for route ").append(routeTitle).append(": <br/>");
		
		for (int weekdays : boundsOrder) {
			if (bounds.containsKey(weekdays)) {
				appendTimeBound(weekdays, ret);
			}
		}
		return ret.toString();
	}

	private String makeTimeString(int secondsFromMidnight) {
		int seconds = secondsFromMidnight % 60;
		int totalMinutes = secondsFromMidnight / 60;
		int minutes = totalMinutes % 60;
		int totalHours = totalMinutes / 60;
		int hours = totalHours % 24;
		
		DateFormat format = TransitSystem.getDefaultTimeFormat();
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, hours);
		calendar.set(Calendar.MINUTE, minutes);
		calendar.set(Calendar.SECOND, seconds);
		
		
		String ret = format.format(calendar.getTime()); 
		return ret;
	}
	
	public String getRouteTitle() {
		return routeTitle;
	}
}
