package me.gurupras.androidcommons;


/**
 * Helper class designed to store time. <br>
 * @author Guru
 *
 */
public class TimeKeeper {
	private long startTime;
	private long endTime;
	private long timeInNS;
	
	/**
	 * Obtain time taken in nanoseconds <br>
	 * Logic is (end-time - start-time) <br>	
	 * @return {@code long} containing the time taken in nanoseconds
	 */
	public long getTimeTaken() {
		return this.timeInNS;
	}
	
	/**
	 * Start the timer
	 */
	public void start() {
		startTime 	= System.nanoTime();
	}
	
	/**
	 * Stop the timer
	 */
	public void stop() {
		endTime		= System.nanoTime();
		timeInNS	= endTime - startTime;
	}
	
	@Override
	public String toString() {
		TimeUnits unit = getBestUnit();
		
		double value = this.timeInNS;
		switch(unit) {
		case HOURS:
			value /= 60;
		case MINUTES:
			value /= 60;
		case SECONDS:
			value /= 1000;
		case MILLISECONDS:
			value /= 1000;
		case MICROSECONDS:
			value /= 1000;
		case NANOSECONDS:
			break;
		}
		
		return String.format("%03.3f", value) + unit; 
	}
	
	/**
	 * Compute the best unit to use for the stored time
	 * @return {@code TimeUnits} containing the best unit
	 */
	private TimeUnits getBestUnit() {
		float value = this.timeInNS;
		TimeUnits bestUnit = TimeUnits.NANOSECONDS;
		while(true) {
			if(value < 1e3 || bestUnit == TimeUnits.HOURS)
				return bestUnit;
			switch(bestUnit) {
			case NANOSECONDS:
				value /= 1e3;
				bestUnit = TimeUnits.MICROSECONDS;
				break;
			case MICROSECONDS:
				value /= 1e3;
				bestUnit = TimeUnits.MILLISECONDS;
				break;
			case MILLISECONDS:
				value /= 1e3;
				bestUnit = TimeUnits.SECONDS;
				break;
			case SECONDS:
				bestUnit = TimeUnits.MINUTES;
				value /= 60;
			case MINUTES:
				value /= 60;
				bestUnit = TimeUnits.HOURS;
				break;
			case HOURS:
			default:
				throw new IllegalStateException("getBestUnit did not break as it should have");
			}
		}
	}

	/**
	 * The {@code TimeUnits} class is a private class used to represent various units of time
	 * @author Guru
	 *
	 */
	private static enum TimeUnits {
		NANOSECONDS("ns"),
		MICROSECONDS("us"),
		MILLISECONDS("ms"),
		SECONDS("s"),
		MINUTES("m"),
		HOURS("h"),
		;
		
		private final String unitsString;
		
		private TimeUnits(String unit) {
			this.unitsString	= unit;
		}
	
		@Override
		public String toString() {
			return this.unitsString;
		}
	}
}