package es.ieeesb.smartcards;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import es.ieeesb.ieeesbdoor.Main;
import es.ieeesb.utils.DBManager;
import es.ieeesb.utils.Log;
import es.ieeesb.utils.PropertiesManager;
import es.ieeesb.utils.Usuario;

/**
 * @author Gregorio Handler that controls NFC authentication. Work in progress
 */

public class NFC implements Runnable
{
	private String baseDir;
	/**
	 * Constructor. Looks for Smartcard readers.
	 */
	public NFC()
	{
		Log.LogEvent(Log.SUBTYPE.NFC, "Inicializando sistema de acceso por NFC");
		if(Main.isWindows())
		{
			baseDir = System.getProperty("user.dir");
			Log.LogEvent(Log.SUBTYPE.NFC, "Sistema Windows detectado");
		}
		else
		{
			File currentDir = null;
			try
			{
				currentDir = new File("./").getCanonicalFile();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			baseDir = currentDir.getAbsolutePath();
			Log.LogEvent(Log.SUBTYPE.NFC, "Sistema Unix detectado");
		}
	}

	/*
	 * Continuosly tries to read the smartcard reader and opens the door if it
	 * should. Also checks users's Latch.
	 */
	public void run()
	{
		while (true)
		{
			try
			{
				Usuario usuarioEntrante = leerNFC();
				if (usuarioEntrante != null)
				{
					if (DBManager.isTokenInDatabase(usuarioEntrante))
					{
						if (!DBManager.isUsingLatch(usuarioEntrante.dni)
								|| Main.latch.checkLatch(usuarioEntrante))
						{
							Log.LogEvent(Log.SUBTYPE.NFC,
									"Acceso autorizado a " + usuarioEntrante.toString());
							Main.openDoor(usuarioEntrante);
						}
					}
					else
					{
						Log.LogEvent(Log.SUBTYPE.NFC,
								"Acceso denegado a " + usuarioEntrante.toString());
					}
					try
					{
						Thread.sleep(2000);
					}
					catch (InterruptedException e)
					{
						Log.LogError(Log.SUBTYPE.DNI, "Error en el hilo de autenticación por NFC: "
								+ e.getMessage());
					}
				}

			}
			catch (Exception e)
			{

			}

		}
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public Usuario leerNFC()
	{
		try
		{
			ProcessBuilder pb = new ProcessBuilder("python", baseDir + PropertiesManager.getProperty("scriptFile"));
			pb.redirectErrorStream(true);
			Process proc;
			proc = pb.start();
			Reader reader = new InputStreamReader(proc.getInputStream());
			int ch;
			StringBuilder strBuilder = new StringBuilder();
			while ((ch = reader.read()) != -1)
			{
				strBuilder.append((char)ch);
			}
			String dni = strBuilder.toString().substring(0, strBuilder.indexOf("&"));
			String token = strBuilder.toString().substring(strBuilder.indexOf("&")+1, strBuilder.length()-1);
			String username = DBManager.getUserFromDNI(dni);
			Usuario user = new Usuario(dni, username);
			user.token = token;
			reader.close();
			return user;
		}
		catch (Exception e)
		{
			Log.LogError(Log.SUBTYPE.NFC, "Error leyendo NFC: " + e.getMessage());
			return null;
		}
	}

}
