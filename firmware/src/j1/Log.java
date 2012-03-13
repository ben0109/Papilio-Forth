package j1;

public class Log {

	public static final int FATAL	= 0;
	public static final int ERROR	= 1;
	public static final int WARNING	= 2;
	public static final int INFO	= 3;
	public static final int DEBUG	= 4;
	public static final int TRACE	= 5;
	private static int currentLevel	= ERROR;	

	public static int getCurrentLevel() {
		return currentLevel;
	}

	public static void setCurrentLevel(int currentLevel) {
		Log.currentLevel = currentLevel;
	}

	public static void log(int level, String format, Object... args) {
		if (currentLevel>=level) {
			System.err.printf(format, args);
			System.err.println();
		}
	}

	public static void fatal(String format, Object... args) {
		log(FATAL, format,args);
	}

	public static void error(String format, Object... args) {
		log(ERROR, format,args);
	}

	public static void warning(String format, Object... args) {
		log(WARNING, format,args);
	}

	public static void info(String format, Object... args) {
		log(INFO, format,args);
	}

	public static void debug(String format, Object... args) {
		log(DEBUG, format,args);
	}

	public static void trace(String format, Object... args) {
		log(TRACE, format,args);
	}
	
}
