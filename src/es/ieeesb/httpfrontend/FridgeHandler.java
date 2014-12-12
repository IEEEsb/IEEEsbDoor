package es.ieeesb.httpfrontend;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;



import es.ieeesb.utils.DBManager;
import es.ieeesb.utils.Log;
import es.ieeesb.utils.Usuario;

/**
 * @author Gregorio
 * Handler that controls the fridge credit requests
 */

public class FridgeHandler implements HttpHandler 
{

	private int statusCode;
	private static final String WEBFORM = "<html><body><form action=\"\" method=\"POST\"> Nombre: <input type='text' placehoder=\"Nombre\" name='Name'/> DNI: <input type=\"text\" placehoder=\"DNI\" name=\"DNI\"/><input type='submit'/></form></body></html>";
	
	/* (non-Javadoc)
	 * @see com.sun.net.httpserver.HttpHandler#handle(com.sun.net.httpserver.HttpExchange)
	 */
	public void handle(HttpExchange t) throws IOException {
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
		double credit = checkCredit(parseCreditRequest(request));
		if (t.getRequestMethod().equals("GET")) 
		{
			response = WEBFORM;
			statusCode = 200;
			Log.LogEvent(Log.SUBTYPE.HTTP_FRIDGE, "Acceso a crédito mediante web solicitado");
		}
		else if (credit != -1000) {
			response = String.valueOf(credit);
			statusCode = 200;
		} 
		else 
		{
			response = "Error";
			statusCode = 404;
		}
		t.sendResponseHeaders(statusCode, response.length());
		os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}

	/**
	 * Auxiliary method that parses the POST request in order to identify the user
	 * @param request
	 * @return null if the request is not valid, an user otherwise
	 */
	public Usuario parseCreditRequest(String request) 
	{
		if(request.length() == 0 || request == null) return null;
		int cutpoint = request.indexOf("&");
		String name = request.substring(0, cutpoint);
		String DNI = request.substring(cutpoint);
		name = name.substring(name.indexOf("=") + 1);
		DNI = DNI.trim();
		DNI = DNI.substring(DNI.indexOf("=") + 1);
		DNI = DNI.substring(0, DNI.length() - 1);
		while (DNI.length() < 9) 
		{
			DNI = "0" + DNI;
		}
		Usuario result = new Usuario(DNI, name);
		return result;
	}
	
	/**
	 * Asks the database for a valid user's credit and returns it
	 * @param user
	 * @return the user's credit in case it is valid, -1000 otherwise
	 */
	public double checkCredit(Usuario user)
	{
		if(user == null) return -1000;
		double credit = DBManager.checkUserCredit(user.dni);
		if(credit == -1000)
			Log.LogWarning(Log.SUBTYPE.HTTP_FRIDGE, "Ha ocurrido un error comprobando el crédito de " + user.toString());
		else
			Log.LogEvent(Log.SUBTYPE.HTTP_FRIDGE, "El usuario " + user.toString() + " ha solicitado ver su saldo");
		return credit;
	}
}
