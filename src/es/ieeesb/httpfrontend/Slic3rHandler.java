package es.ieeesb.httpfrontend;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import es.ieeesb.slic3rserver.Slic3rWrapper;
import es.ieeesb.utils.AbstractFile;
import es.ieeesb.utils.DBManager;
import es.ieeesb.utils.Log;
import es.ieeesb.utils.Usuario;

public class Slic3rHandler implements HttpHandler
{

	private static final int BUFFER_SIZE = 1024;
	private static int statusCode;

	public void handle(HttpExchange t) throws IOException
	{
		Log.LogEvent(Log.SUBTYPE.HTTP_SLIC3R, "Recibiendo archivo para Slic3r");
		String SAVE_DIR = Slic3rWrapper.uploadedDir;
		byte[] buffer = new byte[BUFFER_SIZE];
		OutputStream os;
		File intermediaryFile = new File(SAVE_DIR + "temp");
		FileOutputStream intermediary = null;
		try
		{
			intermediary = new FileOutputStream(intermediaryFile);
		}
		catch (Exception e)
		{
			Log.LogError(Log.SUBTYPE.HTTP_SLIC3R,
					"Error creando archivo temporal: " + e.getMessage());
		}
		String response = "";
		int bytesRead = -1;
		try
		{
			while ((bytesRead = t.getRequestBody().read(buffer)) != -1)
			{
				intermediary.write(buffer, 0, bytesRead);
			}
			intermediary.close();
		}
		catch (Exception e)
		{
			Log.LogError(Log.SUBTYPE.HTTP_SLIC3R,
					"Error creando el archivo temporal: " + e.getMessage());
		}
		String tempStr = "";
		try
		{
			tempStr = readFileAsString(SAVE_DIR + "temp");
		}
		catch (Exception e)
		{
			Log.LogError(Log.SUBTYPE.HTTP_SLIC3R,
					"Error leyendo archivo temporal: " + e.getMessage());
		}
		AbstractFile file = parseFileUpload(tempStr);
		

		if(file == null)
		{
			Log.LogError(Log.SUBTYPE.HTTP_SLIC3R, "Petición incorrecta");
			statusCode = 404;
			response = "Error";
		}
		else if(file.owner == null || !DBManager.isTokenInDatabase(file.owner))
		{
			Log.LogError(Log.SUBTYPE.HTTP_SLIC3R, "Acceso no autorizado o usuario inexistente");
			response = "Error";
			statusCode = 404;
		}
		else
		{
			File saveFile = new File(SAVE_DIR + file.filename);
			try
			{
				saveFile.createNewFile();
				PrintWriter fileStream = new PrintWriter(saveFile, "ISO-8859-1");
				fileStream.write(file.content);
				fileStream.close();
			}
			catch (Exception e)
			{
				Log.LogError(Log.SUBTYPE.HTTP_SLIC3R, "Error escribiendo stl en disco: " + e.getMessage());
			}

			Log.LogEvent(Log.SUBTYPE.HTTP_SLIC3R, "Subida completada, archivo guardado en: "
					+ saveFile.getAbsolutePath());

			Slic3rWrapper.refreshProfiles();
			File profile = Slic3rWrapper.getProfileByName(file.profile);
			if (profile != null)
			{
				Log.LogEvent(Log.SUBTYPE.SLIC3R, "Perfil seleccionado: " + profile);
				Slic3rWrapper slic3rWrapper = new Slic3rWrapper(saveFile, profile, file.owner.email);
				Thread slic3rThread = new Thread(slic3rWrapper);
				slic3rThread.start();
				response = "Subida completa, enviado a la cola de Slic3r.";
				statusCode = 200;
			}
			else
			{
				statusCode = 404;
				response = "Error";
				Log.LogError(Log.SUBTYPE.HTTP_SLIC3R, "Perfil de impresión incorrecto");
			}
		}

		

		t.sendResponseHeaders(statusCode, response.length());
		os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}

	private String readFileAsString(String filePath) throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(
				filePath), "ISO-8859-1"));
		char[] buffer = new char[1024];
		int bytesRead = -1;
		StringBuffer result = new StringBuffer();
		while ((bytesRead = reader.read(buffer)) != -1)
		{
			String readData = String.valueOf(buffer, 0, bytesRead);
            result.append(readData);
		}
		reader.close();
		return result.toString();
	}

	private AbstractFile parseFileUpload(String request)
	{
		Log.LogEvent(Log.SUBTYPE.HTTP_SLIC3R, "Parseando archivo recibido");
		
		int nameIndex = request.indexOf("Name");
		if(nameIndex == -1) return null;
		request = request.substring(nameIndex);
		request = request.substring(request.indexOf("\r\n\r\n"));
		String name = request.substring(0, request.indexOf("--"));
		
		int dniIndex = request.indexOf("DNI");
		if(dniIndex == -1) return null;
		request = request.substring(dniIndex);
		request = request.substring(request.indexOf("\r\n\r\n"));
		String DNI = request.substring(0, request.indexOf("--"));
		
		int tokenIndex = request.indexOf("Token");
		if(tokenIndex == -1) return null;
		request = request.substring(tokenIndex);
		request = request.substring(request.indexOf("\r\n\r\n"));
		String Token = request.substring(0, request.indexOf("--"));
		
		int emailIndex = request.indexOf("Email");
		if(emailIndex == -1) return null;
		request = request.substring(emailIndex);
		request = request.substring(request.indexOf("\r\n\r\n"));
		String Email = request.substring(0, request.indexOf("--"));
		
		int profileIndex = request.indexOf("Profile");
		if(profileIndex == -1) return null;
		request = request.substring(profileIndex);
		request = request.substring(request.indexOf("\r\n\r\n"));
		String profile = request.substring(0, request.indexOf("--"));
		
		Usuario owner = new Usuario(DNI.trim(), name.trim());
		owner.token = Token.trim();
		owner.email = Email.trim();
		
		int filenameIndex = request.indexOf("filename=") + 9;
		if(filenameIndex == -1) return null;
		request = request.substring(filenameIndex);
		String filename = request.substring(0, request.indexOf("\r\n"));
		
		request = request.substring(request.indexOf("\r\n\r\n"));
		int finalIndex = request.indexOf("--");
		if(finalIndex == -1) return null;
		
		request = request.substring(0, finalIndex);
		
		String content = request.substring(4, request.length()-2);
		filename = filename.trim();
		filename = filename.replace("\"", "");
		profile = profile.trim();
		Log.LogEvent(Log.SUBTYPE.HTTP_SLIC3R, "Archivo parseado correctamente");
		return new AbstractFile(filename, content, owner, profile);
	}

}
