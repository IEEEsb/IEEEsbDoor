package es.ieeesb.httpfrontend;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import es.ieeesb.utils.DBManager;
import es.ieeesb.utils.Log;



/**
 * @author Gregorio
 * Handler that controls the version of the mobile apps. 
 */

public class VersionHandler implements HttpHandler
{
	
	private int statusCode;
	
	private enum OS { WP, ANDROID, IOS, SERVER, OTHER}
	
	public void handle(HttpExchange t) throws IOException 
	{
		String request = "";
		byte[] buffer = new byte[1000];
		OutputStream os;
		String response = "";
		int readByte = t.getRequestBody().read();
		if(readByte != -1) 
		{
			buffer[0] = (byte)readByte;
			t.getRequestBody().read(buffer, 1, 999);
			request = new String(buffer, "UTF-8");
			request = request.trim();
		}
		OS clientOS = parseVersionRequest(request);
		switch(clientOS)
		{
		case WP:
			Log.LogEvent(Log.SUBTYPE.HTTP_VERSION, "Devolviendo número de versión de la app de WP");
			response = DBManager.getVersion(OS.WP.toString());
			statusCode = 200;
			break;
		case ANDROID:
			Log.LogEvent(Log.SUBTYPE.HTTP_VERSION, "Devolviendo número de versión de la app de Android");
			response = DBManager.getVersion(OS.ANDROID.toString());
			statusCode = 200;
			break;
		case IOS:
			Log.LogEvent(Log.SUBTYPE.HTTP_VERSION, "Devolviendo número de versión de la app de iOS");
			response = DBManager.getVersion(OS.IOS.toString());
			statusCode = 200;
			break;
		case SERVER:
			Log.LogEvent(Log.SUBTYPE.HTTP_VERSION, "Devolviendo número de versión del servidor");
			response = DBManager.getVersion(OS.SERVER.toString());
			statusCode = 200;
			break;
		default:
			Log.LogEvent(Log.SUBTYPE.HTTP_VERSION, "Error devolviendo número de versión");
			response = "Error";
			statusCode = 404;
			break;
		}
        t.sendResponseHeaders(statusCode, response.length());
        os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

	private OS parseVersionRequest(String request)
	{
		if(request.length() == 0 || request == null) return null;

		request = request.substring(request.indexOf("=") + 1);
		OS result = null;
		try
		{
			result = OS.valueOf(request);
		}
		catch(Exception e)
		{
			return OS.OTHER;
		}
		return result;
	}
}
