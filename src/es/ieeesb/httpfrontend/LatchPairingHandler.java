package es.ieeesb.httpfrontend;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import es.ieeesb.ieeesbdoor.Main;
import es.ieeesb.utils.DBManager;
import es.ieeesb.utils.Log;
import es.ieeesb.utils.Usuario;

/**
 * @author Gregorio
 * Handler that controls latch pairing.
 * To get a valid response from this endpoint, name, DNI, pairing token and door token are required.
 */

public class LatchPairingHandler implements HttpHandler 
{
	private int statusCode;

	@Override
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
		Usuario user = parsePairingRequest(request);
		if(user == null)
		{
			Log.LogEvent(Log.SUBTYPE.HTTP_LATCH, "Petición inválida");
			response = "Error";
			statusCode = 404;
		}
		if(user != null && DBManager.isTokenInDatabase(user))
		{
			String accID = Main.latch.pair(user);
			if(accID != null && accID != "")
			{
				Log.LogHidden(Log.SUBTYPE.LATCH, "AccountID de " + user.toString() + ": " + accID);
				user.accID = accID;
				DBManager.insertAccId(user);
				statusCode = 200;
				response = "Emparejado correctamente con Latch";
				Log.LogEvent(Log.SUBTYPE.HTTP_LATCH, "Emparejamiento del usuario " + user.toString() + " correcto");
			}
			else
			{
				Log.LogEvent(Log.SUBTYPE.HTTP_LATCH, "Emparejamiento del usuario " + user.toString() + " incorrecto");
				response = "Error";
				statusCode = 404;
			}
		}
		else 
		{
			Log.LogEvent(Log.SUBTYPE.HTTP_LATCH, "El usuario " + user.toString() + " no existe o no tiene un token válido");
			response = "Error";
			statusCode = 404;
		}
		t.sendResponseHeaders(statusCode, response.length());
		os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
	
	/**
	 * Auxiliary method that parses the POST request
	 * @param request
	 * @return null if the request is not valid, an user otherwise
	 */
	public Usuario parsePairingRequest(String request) 
	{
		if(request.length() == 0 || request == null) return null;
		int cutpoint1 = request.indexOf("&");
		int cutpoint2 = request.indexOf("&", cutpoint1 + 1);
		int cutpoint3 = request.indexOf("&", cutpoint2 + 1);
		String name = request.substring(0, cutpoint1);
		String DNI = request.substring(cutpoint1, cutpoint2);
		String pairingToken = request.substring(cutpoint2, cutpoint3);
		String token = request.substring(cutpoint3);
		name = name.substring(name.indexOf("=") + 1);
		DNI = DNI.substring(DNI.indexOf("=") + 1);
		pairingToken = pairingToken.substring(pairingToken.indexOf("=") + 1);
		token = token.substring(token.indexOf("=") + 1);
		Usuario result = new Usuario(DNI, name);
		result.pairingToken = pairingToken;
		result.token = token;
		return result;
	}

}
