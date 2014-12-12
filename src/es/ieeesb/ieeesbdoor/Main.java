package es.ieeesb.ieeesbdoor;

import java.util.ArrayList;
import java.util.Timer;

import es.ieeesb.httpfrontend.HttpFrontEnd;
import es.ieeesb.latch.IEEEsbLatch;
import es.ieeesb.slic3rserver.Slic3rWrapper;
import es.ieeesb.smartcards.DNIe;
import es.ieeesb.smartcards.NFC;
import es.ieeesb.utils.Log;
import es.ieeesb.utils.PropertiesManager;
import es.ieeesb.utils.ProtocolManager;
import es.ieeesb.utils.UserTimerTask;
import es.ieeesb.utils.Usuario;


/**
 * @author Gregorio
 * Main class. Launches all the services and monitors door access.
 */
public class Main
{
	public static String OS = System.getProperty("os.name").toLowerCase();
	
	public static PropertiesManager propertiesManager;


	public static ProtocolManager protocol;
	/**
	 * List that hold the lasts users that tried to open the door. It is used to prevent DDoS attacks or irresponsible
	 * uses. 
	 */
	public static ArrayList<Usuario> lastUsers;
	/**
	 * Task that removes users from the list above once a security time lapse has passed.
	 */
	public static UserTimerTask task;
	/**
	 * Our own Latch implementation
	 */
	public static IEEEsbLatch latch;



	/**
	 * @param args
	 * Handles initialization and shutdown.
	 */
	public static void main(String args[])
	{
		propertiesManager = new PropertiesManager();
		Log.LogEvent(Log.SUBTYPE.SYSTEM, "Iniciando sistemas");
		
		
		lastUsers = new ArrayList<Usuario>();
		protocol = new ProtocolManager(PropertiesManager.getProperty("portName"), PropertiesManager.getProperty("portSpeed"));
		Thread nfc = new Thread(new NFC());
		nfc.start();
		Thread dnie = new Thread(new DNIe());
		dnie.start();
		Slic3rWrapper.setEnvironment();
		HttpFrontEnd httpFrontEnd = new HttpFrontEnd();
		httpFrontEnd.start();
		latch = new IEEEsbLatch(PropertiesManager.getProperty("latchAppID"), PropertiesManager.getProperty("latchSecret"));
		Log.LogEvent(Log.SUBTYPE.SYSTEM, "Sistemas inicializados");
		
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
		    @Override
		    public void run()
		    {
		        protocol.close();
		        Log.LogEvent(Log.SUBTYPE.SYSTEM, "Apagando sistema");
		    }
		});
	}
	


	/**
	 * Opens the door provided a valid user. This method can be called by the different authentication threads once 
	 * they return a successful user logon. Also prevents an user from trying to flood the system.
	 * @param user
	 */
	public synchronized static void openDoor(Usuario user)
	{
		if (user == null)
			return;
		lastUsers.add(user);
		Timer timer = new Timer();
		UserTimerTask userTimerTask = new UserTimerTask(user, lastUsers);
		timer.schedule(userTimerTask, 5000);
		int ocurrences = 0;
		for (Usuario userInList : lastUsers)
		{
			if (user.equals(userInList))
				ocurrences++;
		}
		if (ocurrences < 3)
		{
			protocol.write("-ABRE-");
			Log.LogEvent(Log.SUBTYPE.DOOR, "Abriendo al usuario " + user.toString());
		}
		else
		{
			Log.LogWarning(Log.SUBTYPE.DOOR, "El usuario " + user.toString() + " se está pasando de intentos");
		}
	}
	
	public static boolean isUnix()
	{

		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);

	}

	public static boolean isWindows()
	{

		return (OS.indexOf("win") >= 0);

	}
	
	
}
