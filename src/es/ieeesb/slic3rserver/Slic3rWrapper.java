package es.ieeesb.slic3rserver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import es.ieeesb.ieeesbdoor.Main;
import es.ieeesb.utils.EmailManager;
import es.ieeesb.utils.Log;
import es.ieeesb.utils.PropertiesManager;


public class Slic3rWrapper implements Runnable
{

	public static ArrayList<String> profiles = new ArrayList<String>();
	public static String baseDir;
	public static String profileDir;
	public static String uploadedDir;
	private String owner;
	private File stl;
	private File profile;
	private File gcode;

	public Slic3rWrapper(File stl, File profile, String owner)
	{
		if (stl.exists())
		{
			this.stl = stl;
			this.profile = profile;
			this.owner = owner;
		}
	}

	public static void setEnvironment()
	{
		Log.LogEvent(Log.SUBTYPE.SLIC3R, "Inicializando servidor de Slic3r");
		if(Main.isWindows())
		{
			baseDir = System.getProperty("user.dir") + PropertiesManager.getProperty("slic3rBaseW");
			profileDir = System.getProperty("user.dir") + PropertiesManager.getProperty("slic3rProfiles");
			uploadedDir = System.getProperty("user.dir") + PropertiesManager.getProperty("slic3rUploaded");
			Log.LogEvent(Log.SUBTYPE.SLIC3R, "Sistema Windows detectado");
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
			baseDir = currentDir.getAbsolutePath() + PropertiesManager.getProperty("slic3rBaseU");
			profileDir = currentDir.getAbsolutePath() + PropertiesManager.getProperty("slic3rProfiles");
			uploadedDir = currentDir.getAbsolutePath() + PropertiesManager.getProperty("slic3rUploaded");
			Log.LogEvent(Log.SUBTYPE.SLIC3R, "Sistema Unix detectado");
		}
	}



	public static File getProfileByName(String name)
	{
		if (!profiles.contains(name))
			return null;

		return new File(profileDir + name);
	}

	public void slice()
	{
		Log.LogEvent(Log.SUBTYPE.SLIC3R, "Cargando archivo: " + stl.getName() + " en la cola de Slic3r");
		List<String> arguments = new ArrayList<String>();
		ProcessBuilder pb = new ProcessBuilder();
		pb.directory(new File(baseDir));
		if(Main.isWindows())
		{
			arguments.add(baseDir + "/slic3r-console.exe");
		}
		else
		{
			arguments.add(baseDir + "bin/slic3r");	
		}
		arguments.add("--ignore-nonexistent-config");
		arguments.add("--load");
		arguments.add(profile.getAbsolutePath());
		arguments.add(stl.getAbsolutePath());
		pb.command(arguments);
		try
		{
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			final Process process = pb.start();
			process.waitFor();
		}
		catch (Exception e)
		{
			Log.LogError(Log.SUBTYPE.SLIC3R, "Error de Slic3r: " + e.getMessage());
		}

		gcode = new File(stl.getParentFile() + "/" + stl.getName().replace(".stl", ".gcode"));
		Log.LogEvent(Log.SUBTYPE.SLIC3R, "GCode guardado en: " + gcode.getAbsolutePath());
	}

	private void sendMail()
	{
		try
		{
			Log.LogEvent(Log.SUBTYPE.EMAIL, "Enviando gcode por email a: " + owner);
			EmailManager.Send(owner, gcode.getAbsolutePath(), gcode.getName());
		}
		catch (Exception e)
		{
			Log.LogError(Log.SUBTYPE.EMAIL, "Error enviando email a: " + owner + ", " + e.getMessage());
		}

	}

	public static ArrayList<String> refreshProfiles()
	{
		Log.LogEvent(Log.SUBTYPE.SLIC3R, "Actualizando perfiles de impresión");
		profiles.clear();
		File profileDirFile = new File(profileDir);
		for (File f : profileDirFile.listFiles())
		{
			profiles.add(f.getName());
		}
		return profiles;
	}

	public void run()
	{
		slice();
		sendMail();
	}

}
