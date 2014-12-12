package es.ieeesb.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author Gregorio
 * Utility class. Creates beautiful log lines with an unified format. Admits different log types and subtypes (type of event,
 * subsystem that generated the event). By default, normal events show up in the server console and get stored in the database,
 * but it also allows you to log hidden things that only get stored in the database and
 * are not shown in the server console. So evil.
 */
public class Log {
	
	public enum TYPE { ERROR, EVENT, WARNING, HIDDEN }
	public enum SUBTYPE { LOGDB, DOORDB, FRIDGEDB, SYSTEM, PRINTER,HTTP, HTTP_VERSION, HTTP_DOOR, HTTP_FRIDGE, HTTP_LATCH, DOOR, DNI, NFC, LATCH, ACTIVE_DIRECTORY, HTTP_IEEENUMBER, HTTP_SLIC3R, SLIC3R, EMAIL, PROPERTIES, OTHER }
	
	public static void LogEvent(SUBTYPE subtype, String message)
	{
		DBManager.log(TYPE.EVENT.toString(), subtype.toString(), message);
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println("[" + timeStamp + "]" + "[" + TYPE.EVENT.toString() + "][" + subtype.toString() + "] " + message);
	}
	
	public static void LogWarning(SUBTYPE subtype, String message)
	{
		DBManager.log(TYPE.WARNING.toString(), subtype.toString(), message);
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println("[" + timeStamp + "]" + "[" + TYPE.WARNING.toString() + "][" + subtype.toString() + "] " + message);
	}
	
	public static void LogError(SUBTYPE subtype, String message)
	{
		DBManager.log(TYPE.ERROR.toString(), subtype.toString(), message);
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		System.out.println("[" + timeStamp + "]" + "[" + TYPE.ERROR.toString() + "][" + subtype.toString() + "] " + message);
	}
	
	public static void LogHidden(SUBTYPE subtype, String message)
	{
		DBManager.log(TYPE.HIDDEN.toString(), subtype.toString(), message);
	}

}
