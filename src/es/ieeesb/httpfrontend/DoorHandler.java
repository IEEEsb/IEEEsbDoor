package es.ieeesb.httpfrontend;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import es.ieeesb.ieeesbdoor.Main;
import es.ieeesb.utils.DBManager;
import es.ieeesb.utils.Log;
import es.ieeesb.utils.TokenGenerator;
import es.ieeesb.utils.Usuario;


/**
 * @author Gregorio
 * Handler that controls the http door access.
 * Depending on the request, you will need your name and DNI or name, DNI and door token in order to get a valid response from
 * this endpoint.
 */

public class DoorHandler implements HttpHandler {
	
	private int statusCode;
	private static final String WEBFORM = "<html><body><form action=\"\" method=\"POST\"> Nombre: <input type='text' placehoder=\"Nombre\" name='Name'/> DNI: <input type=\"text\" placehoder=\"DNI\" name=\"DNI\"/> RegID: <input type='text' placehoder=\"RegID\" name='RegID'/><input type='submit'/></form><form action=\"\" method=\"POST\"> Nombre: <input type='text' placehoder=\"Nombre\" name='Name'/> DNI: <input type=\"text\" placehoder=\"DNI\" name=\"DNI\"/> Token: <input type='text' placehoder=\"Token\" name='Token'/><input type='submit'/></form></body></html>";
	
	/* (non-Javadoc)
	 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
	 */
	public void handle(HttpExchange t) throws IOException 
	{
		String request = "";
		byte[] buffer = new byte[1000];
		OutputStream os;
		String response;
		int readByte = t.getRequestBody().read();
		if(readByte != -1) 
		{
			buffer[0] = (byte)readByte;
			t.getRequestBody().read(buffer, 1, 999);
			request = new String(buffer, "UTF-8");
			request = request.trim();
		}
		Usuario user = parseFirstLogon(request);
		if(t.getRequestMethod().equals("GET"))
		{
			response = WEBFORM;
			statusCode = 200;
			Log.LogEvent(Log.SUBTYPE.HTTP_DOOR, "Acceso web solicitado");
		}
		else if (user != null) 
		{
			response = handleFirstLogon(user);
		} 
		else 
		{
			response = handleTokenAuth(parseTokenAuth(request));
		}
		t.sendResponseHeaders(statusCode, response.length());
		os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}

	/**
	 * If the user doesn't have a token yet, we try to give him one, thus invalidating his regID;
	 * @param user trying to log in
	 * @return server response
	 */
	public String handleFirstLogon(Usuario user) {
		String token;
		String response;
		if (DBManager.validRegisterID(user)) 
		{
			token = TokenGenerator.generateToken();
			DBManager.storeToken(user, token);
			DBManager.invalidateRegisterID(user);
			Log.LogEvent(Log.SUBTYPE.HTTP_DOOR, "El usuario " + user.toString() + " ha solicitado un nuevo token de acceso");
			response = token;
			statusCode = 200;
		} 
		else 
		{
			response = "El usuario " + user.toString() + " no tiene un ID de registro válido";
			statusCode = 404;
			Log.LogWarning(Log.SUBTYPE.HTTP_DOOR, response);
		}
		return response;
	}

	/**
	 * If the user has a token, we try to open the door for him. Also checks his latch.
	 * @param user
	 * @return server response
	 */
	public String handleTokenAuth(Usuario user) 
	{
		String response;
		if (DBManager.isTokenInDatabase(user)) 
		{
			if(!DBManager.isUsingLatch(user.dni) || Main.latch.checkLatch(user))
			{
				response = "Acceso autorizado";
				Log.LogHidden(Log.SUBTYPE.HTTP_DOOR, response + " al usuario " + user.toString() + " Token: " + user.token);
				Log.LogEvent(Log.SUBTYPE.HTTP_DOOR, response + " al usuario " + user.toString());
				Main.openDoor(user);
				statusCode = 200;
			}
			else
			{
				response = "Latch cerrado";
				statusCode = 404;
				Log.LogHidden(Log.SUBTYPE.HTTP_DOOR, "El usuario " + user.toString() + " tiene el latch cerrado. " + "Token: " + user.token);
				Log.LogEvent(Log.SUBTYPE.HTTP_DOOR, "El usuario " + user.toString() + " tiene el latch cerrado.");
			}
		} 
		else 
		{
			response = "Acceso no autorizado";
			statusCode = 404;
			Log.LogHidden(Log.SUBTYPE.HTTP_DOOR, response + " al usuario " + user.toString() + " Token: " + user.token);
			Log.LogEvent(Log.SUBTYPE.HTTP_DOOR, response + " al usuario " + user.toString());
		}
		return response;
	}

	/**
	 * Auxiliary method, parses the POST request when user is trying to access using his token.
	 * @param request
	 * @return null if the request is not valid, a user if it is
	 */
	private Usuario parseTokenAuth(String request) 
	{
		if(request.length() == 0 || request == null) return null;
		if (request.contains("RegID"))
			return null;
		int cutpoint1 = request.indexOf("&");
		int cutpoint2 = request.indexOf("&", cutpoint1 + 1);
		String name = request.substring(0, cutpoint1);
		String DNI = request.substring(cutpoint1, cutpoint2);
		String token = request.substring(cutpoint2);
		name = name.substring(name.indexOf("=") + 1);
		DNI = DNI.substring(DNI.indexOf("=") + 1);
		token = token.substring(token.indexOf("=") + 1);
		Usuario result = new Usuario(DNI, name);
		result.token = token;
		return result;
	}

	/**
	 * Auxiliary method that parses the POST request for the first logon of a user
	 * @param request
	 * @return null if the request is not valid, an user otherwise
	 */
	private Usuario parseFirstLogon(String request) 
	{
		if(request.length() == 0 || request == null) return null;
		if (request.contains("Token"))
			return null;
		int cutpoint1 = request.indexOf("&");
		int cutpoint2 = request.indexOf("&", cutpoint1 + 1);
		String name = request.substring(0, cutpoint1);
		String DNI = request.substring(cutpoint1, cutpoint2);
		String registrationID = request.substring(cutpoint2);
		name = name.substring(name.indexOf("=") + 1);
		DNI = DNI.substring(DNI.indexOf("=") + 1);
		registrationID = registrationID.substring(registrationID
				.indexOf("=") + 1);
		Usuario result = new Usuario(DNI, name);
		result.registrationID = registrationID;
		return result;
	}
}
