package es.ieeesb.httpfrontend;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import es.ieeesb.slic3rserver.Slic3rWrapper;
import es.ieeesb.utils.Log;




public class Slic3rProfilesHandler implements HttpHandler
{
	
	public static int statusCode;

	public void handle(HttpExchange t) throws IOException
	{
		Log.LogEvent(Log.SUBTYPE.HTTP_SLIC3R, "Perfiles de impresión solicitados");
		OutputStream os;
		ArrayList<String> profiles = Slic3rWrapper.refreshProfiles();
		StringBuffer response = new StringBuffer();
		for(String profile : profiles)
		{
			response.append(profile);
			response.append(",");
		}
		statusCode = 200;
		t.sendResponseHeaders(statusCode, response.length());
		os = t.getResponseBody();
		os.write(response.toString().getBytes());
		os.close();
	}

}
