package com.tibco.bpm.cdm.core.autopurge;

/**
 * Constant values shared by various classes concerned with auto-purge functionality.
 * 
 * @author smorgan
 * @since 2019
 */
public interface AutoPurgeConstants
{
	// The config property that determines the number of minutes between auto-purge cycles.
	String PROPERTY_INTERVAL = "autopurge.interval";

	// By default, the auto-purge process will run every 15 minutes.
	int DEFAULT_INTERVAL_MINUTES = 15;

	// The auto-purge cycle cannot be configured to run more frequently than once every 15 minutes.
	int MIN_INTERVAL_MINUTES = 15;

	// The maximum interval is limited only by the capacity of Integer representing seconds.
	int MAX_INTERVAL_MINUTES = Integer.MAX_VALUE / 60;

	// The config property that determines the number of minutes after a case enters a terminal state
	// that it qualifies for auto-purging.
	String PROPERTY_PURGE_TIME = "autopurge.purgeTime";

	// 90 days.
	int DEFAULT_PURGE_TIME = 129600;

	// The minimum number of minutes before cases are purged. Zero means 'purge immediately', which effectively
	// means 'purge on the next auto-purge cycle'.
	int MIN_PURGE_TIME_MINUTES = 0;

	// 10 years (10 x 365 x 24 x 60).
	int MAX_PURGE_TIME_MINUTES = 5256000;
}
