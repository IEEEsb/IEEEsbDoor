package es.ieeesb.httpfrontend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import es.ieeesb.utils.Log;
import es.ieeesb.utils.PropertiesManager;


/**
 * @author Gregorio
 * Handler that controls 3D printer status requests. Basically forwards a GET request with the correct parameter so 
 * Octoprint can return the status json.
 */

public class PrinterHandler implements HttpHandler
{
	public void handle(HttpExchange t) throws IOException 
	{
		OutputStream os;
        StringBuffer response = new StringBuffer();
    	URL url = new URL(PropertiesManager.getProperty("OctoprintAddress")+"/api/job");
		URLConnection con = url.openConnection();
		con.setRequestProperty("X-Api-Key", PropertiesManager.getProperty("OctoprintAPIKey"));
		con.connect();
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		while ((inputLine = in.readLine()) != null) 
		{
			response.append(inputLine);
		}
		Log.LogEvent(Log.SUBTYPE.PRINTER, "Consultando estado de la impresora 3D");
		in.close();
        t.sendResponseHeaders(200, response.length());
        os = t.getResponseBody();
        os.write(response.toString().getBytes());
        os.close();
    }
}
