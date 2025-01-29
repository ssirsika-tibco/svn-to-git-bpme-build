package com.tibco.bpm.cdm.util;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a data/time expression in a string into a Calendar, including support for partial
 * expressions, as long as the 'leftmost' portion is populated.
 * 
 * e.g. Supported expressions include:
 * 
 * 2010
 * 2010-02
 * 2010-02-10
 * 2010-02-10T11
 * 2010-02-10T11:34
 * 2010-02-10T11:34:22
 * 2010-02-10T11:34:22.222
 *
 * The date can optionally be appended with a timezone specifier of the following format:
 * Z - Specifies a UTC Date
 * [+|-]HH
 * [+|-]HH:MM
 * [+|-]HHMM
 *
 * <p/>&copy;2019 TIBCO Software Inc.
 * @author smorgan
 * @since 2019
 */
public class DateTimeParser
{
	public static final TimeZone	UTC_ZONE							= TimeZone.getTimeZone("UTC");

	private static final String		UTC_DESIGNATOR						= "Z";

	// The following definitions are used in the parseString method to define
	// Lower and upper bounds defaults for the various time/date fields
	// The offset fields are used to adjust a "normal" field into
	// a calendar valid field - only used for Month normally as Calendar stores months 0 based
	private static final int		SPLIT_DATETIME_MS_OFFSET			= 0;

	private static final int		SPLIT_DATETIME_MS_UBOUND			= 999;

	private static final int		SPLIT_DATETIME_MS_LBOUND			= 0;

	private static final int		SPLIT_DATETIME_SECOND_OFFSET		= 0;

	private static final int		SPLIT_DATETIME_SECOND_UBOUND		= 59;

	private static final int		SPLIT_DATETIME_SECOND_LBOUND		= 0;

	private static final int		SPLIT_DATETIME_MINUTE_OFFSET		= 0;

	private static final int		SPLIT_DATETIME_MINUTE_UBOUND		= 59;

	private static final int		SPLIT_DATETIME_MINUTE_LBOUND		= 0;

	private static final int		SPLIT_DATETIME_HOUR_OFFSET			= 0;

	private static final int		SPLIT_DATETIME_HOUR_UBOUND			= 23;

	private static final int		SPLIT_DATETIME_HOUR_LBOUND			= 0;

	private static final int		SPLIT_DATETIME_DAY_OFFSET			= 0;

	private static final int		SPLIT_DATETIME_DAY_LBOUND			= 1;

	private static final int		SPLIT_DATETIME_MONTH_OFFSET			= -1;

	private static final int		SPLIT_DATETIME_MONTH_UBOUND			= 11;

	private static final int		SPLIT_DATETIME_MONTH_LBOUND			= 0;

	private static final int		SPLIT_DATETIME_YEAR_OFFSET			= 0;

	private static final int		SPLIT_DATETIME_YEAR_UBOUND			= 2199;

	private static final int		SPLIT_DATETIME_YEAR_LBOUND			= 1900;

	// The following definitions are used in conjunction with the DATE_TIME_PATTERN
	// to extract fields from a formatted string
	private static final int		SPLIT_DATETIME_YEAR_POS				= 1;												// Position of Year in expression

	private static final int		SPLIT_DATETIME_MONTH_POS			= 2;												// Position of Month in expression

	private static final int		SPLIT_DATETIME_DAY_POS				= 3;												// Position of Day in expression

	private static final int		SPLIT_DATETIME_HOUR_POS				= 4;												// Position of Hour in expression

	private static final int		SPLIT_DATETIME_MINUTE_POS			= 5;												// Position of Minute in expression

	private static final int		SPLIT_DATETIME_SECOND_POS			= 6;												// Position of Second in expression

	private static final int		SPLIT_DATETIME_MS_POS				= 7;												// Position of MS in expression

	private static final int		SPLIT_DATETIME_TIMEZONE_POS			= 8;												// Position of Timezone in expression

	private static final int		SPLIT_DATETIME_REQUIRED_GROUPCOUNT	= 8;												// The number of groups expected in a specified date string

	/**
	 * The following pattern is used by the parseString function to
	 * validate/split a time-only String into fields which can be used to
	 * populate a calendar. See that function for the definition of the time
	 * fomats are supported. The expression itself is huge, but boils down a
	 * series of (?:(expression)) style groups this is done to throw away the :
	 * separator used in the time but to keep the element itself We then
	 * organise the brackets to enforce the fact that, for example, you can't
	 * have seconds without minutes.
	 */
	// @formatter:off    
    private static final String TIME_PATTERN =
            "(?:" +
                "([0-1][0-9]|2[0-3])" + // Group 4 - Hour
                "(" +
                    "?:\\:" +
                    "([0-5][0-9])" + // Group 5 - Minutes
                    "(" +
                        "?:\\:" +
                        "([0-5][0-9])" + // Group 6 -Seconds
                        "(" +
                            "?:\\."+
                            "(\\d{3})" + // Group 7 - Milliseconds
                        ")?" +
                    ")?" +
                ")?" +
            ")?" +
            "(" + // Group 8 - TZ
                UTC_DESIGNATOR +
                "|" +
                "[+|-]\\d{2}(?:\\:?\\d{2})?" +
            ")?";
	// @formatter:on

	/**
	 * The following pattern is used by the parseString function to
	 * validate/split a Date into fields which can be used to populate a
	 * calendar. See that function for the definition of the date formats ares
	 * supported. The expression itself is huge, but boils down a series of
	 * (?:(expression)) style groups this is done to throw away the : or -
	 * separator used in the date but to keep the element itself We then
	 * organise the brackets to enforce the fact that, for example, you can't
	 * have seconds without minutes.
	 */
	// @formatter:off
    private static final String DATE_TIME_PATTERN =
            "(\\d{4})" + // Group 1 - Year
            "(" +
                "?:-" + // Throw away the colon - but require it
			"([0][0-9]|[1][0-2])" +																							// Group 2 - Month
                "(" +
                    "?:-" + // Throw away the colon - but require it
			"([0-2][0-9]|[3][0-1])" +																						// Group 3 - Day
                ")?" +
            ")?" +
            "(?:T" + // Time boundary - We require the T but throw it away
                TIME_PATTERN +
            ")?";
	// @formatter:on

	private static final Pattern	splitDateTimePattern				= Pattern.compile(DATE_TIME_PATTERN);

	/**
	 * Parses the string and returns a calendar. If values are missing to the right
	 * of the expression, they are filled with zeroes.  If maximise is set, they
	 * are set to their maximum possible value instead. 
	 * e.g. 2017 becomes 2017-01-01T00:00:00.000Z.  If maximise is set, 
	 * it becomes 2017-12-31T23:59:59.999Z.
	 *
	 * @param string
	 * @param maximise
	 * @return
	 */
	public static Calendar parseString(String string, boolean maximise)
	{
		if (string == null)
		{
			return null;
		}

		string = string.trim().toUpperCase();
		if (string.length() == 0)
		{
			return null;
		}

		// This date is valid as far as the pattern is concerned
		Matcher match = splitDateTimePattern.matcher(string);
		if ((!match.matches()) || (match.groupCount() != SPLIT_DATETIME_REQUIRED_GROUPCOUNT))
		{
			return null;
		}

		String timezone = match.group(SPLIT_DATETIME_TIMEZONE_POS);

		// Time-zone is always mandatory
		if (timezone == null)
		{
			return null;
		}

		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT" + timezone));

		c.set(Calendar.YEAR, getField(match, SPLIT_DATETIME_YEAR_POS, maximise, SPLIT_DATETIME_YEAR_LBOUND,
				SPLIT_DATETIME_YEAR_UBOUND, SPLIT_DATETIME_YEAR_OFFSET));
		c.set(Calendar.MONTH, getField(match, SPLIT_DATETIME_MONTH_POS, maximise, SPLIT_DATETIME_MONTH_LBOUND,
				SPLIT_DATETIME_MONTH_UBOUND, SPLIT_DATETIME_MONTH_OFFSET));
		c.set(Calendar.DAY_OF_MONTH,
				getField(match, SPLIT_DATETIME_DAY_POS, maximise, SPLIT_DATETIME_DAY_LBOUND,
						c.getActualMaximum(Calendar.DAY_OF_MONTH), // No static for UBound as it will depend on the year/month
						SPLIT_DATETIME_DAY_OFFSET));
		c.set(Calendar.HOUR_OF_DAY, getField(match, SPLIT_DATETIME_HOUR_POS, maximise, SPLIT_DATETIME_HOUR_LBOUND,
				SPLIT_DATETIME_HOUR_UBOUND, SPLIT_DATETIME_HOUR_OFFSET));
		c.set(Calendar.MINUTE, getField(match, SPLIT_DATETIME_MINUTE_POS, maximise, SPLIT_DATETIME_MINUTE_LBOUND,
				SPLIT_DATETIME_MINUTE_UBOUND, SPLIT_DATETIME_MINUTE_OFFSET));
		c.set(Calendar.SECOND, getField(match, SPLIT_DATETIME_SECOND_POS, maximise, SPLIT_DATETIME_SECOND_LBOUND,
				SPLIT_DATETIME_SECOND_UBOUND, SPLIT_DATETIME_SECOND_OFFSET));
		c.set(Calendar.MILLISECOND, getField(match, SPLIT_DATETIME_MS_POS, maximise, SPLIT_DATETIME_MS_LBOUND,
				SPLIT_DATETIME_MS_UBOUND, SPLIT_DATETIME_MS_OFFSET));

		return c;
	}

	private static int getField(Matcher match, int position, boolean maximise, int lower, int upper, int offset)
	{
		String value = match.group(position);
		if (value == null)
		{
			return maximise ? upper : lower;
		}
		//		else
		//		{
		//			System.out.println("using a default");
		//		}

		return (Integer.parseInt(value) + offset);
	}

	public static Calendar toUTC(Calendar aCalendar)
	{
		if (aCalendar == null)
		{
			return (null);
		}

		if (UTC_ZONE.equals(aCalendar.getTimeZone()))
		{
			return (aCalendar);
		}

		Calendar result = Calendar.getInstance(UTC_ZONE);
		result.setTimeInMillis(aCalendar.getTimeInMillis());

		return (result);
	}
}
