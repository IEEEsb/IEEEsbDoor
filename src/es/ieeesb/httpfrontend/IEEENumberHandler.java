package es.ieeesb.httpfrontend;

import java.io.IOException;
import java.io.OutputStream;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import es.ieeesb.utils.ActiveDirectory;
import es.ieeesb.utils.DBManager;
import es.ieeesb.utils.Log;
import es.ieeesb.utils.Usuario;


public class IEEENumberHandler implements HttpHandler
{
	
	private int statusCode;
	private static final String WEBFORM = "<html><body><form action=\"\" method=\"POST\"> Nombre: <input type='text' placehoder=\"Nombre\" name='Name'/> DNI: <input type=\"text\" placehoder=\"DNI\" name=\"DNI\"/> Token: <input type='text' placehoder=\"Token\" name='Token'/><input type='submit'/></form></body></html>";

	@Override
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
		Usuario user = parseIEEENumRequest(request);
		if(t.getRequestMethod().equals("GET"))
		{
			response = WEBFORM;
			statusCode = 200;
			Log.LogEvent(Log.SUBTYPE.HTTP_IEEENUMBER, "Acceso web solicitado");
		}
		else if (user != null && DBManager.isTokenInDatabase(user)) 
		{
			response = checkIEEENumber(user);
		} 
		else 
		{
			statusCode = 404;
			response = "Error";
		}
		t.sendResponseHeaders(statusCode, response.length());
		os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
	
	public String checkIEEENumber(Usuario user)
	{
		statusCode = 404;
		NamingEnumeration<SearchResult> result = null;
		Log.LogEvent(Log.SUBTYPE.HTTP_IEEENUMBER, "El usuario " + user.toString() + " ha solicitado ver su número del IEEE");
		try
		{
			ActiveDirectory.openConnection();
			result = ActiveDirectory.searchUser(user.dni, "DNI", null);
	        if(result.hasMore()) 
	        {
				SearchResult rs= (SearchResult)result.next();
				Attributes attrs = rs.getAttributes();
				String temp = attrs.get("samaccountname").toString();
				temp = attrs.get("givenname").toString();
				temp = attrs.get("mail").toString();
				temp = attrs.get("cn").toString();
				temp = attrs.get("employeeid").toString();
				String IEEENumber = temp.substring(temp.indexOf(":") + 1);
				statusCode = 200;
				ActiveDirectory.closeConnection();
				return IEEENumber;
			} 
	        else  
	        {
				Log.LogError(Log.SUBTYPE.HTTP_IEEENUMBER, "Error consultando el número del IEEE del usuario " + user.toString());
				return null;
			}
		}
		catch (NamingException e)
		{
			Log.LogError(Log.SUBTYPE.HTTP_IEEENUMBER, "Error consultando el active directory: " + e.getMessage());
		}
		return null;
	}

	public Usuario parseIEEENumRequest(String request)
	{
		if(request.length() == 0 || request == null) return null;
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
}
