package es.ieeesb.latch;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;




import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;




import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import es.elevenpaths.latch.Latch;
import es.elevenpaths.latch.LatchResponse;
import es.ieeesb.utils.DBManager;
import es.ieeesb.utils.Log;
import es.ieeesb.utils.Usuario;

/**
 * @author Gregorio
 * Our implementation of the Latch plugin. This beauty is possibly worth 10000$. Be careful!
 */
public class IEEEsbLatch extends Latch
{
	
	private String appID;
	@SuppressWarnings("unused")
	private String secret;

	/**
	 * Constructor, required to initialize parent class.
	 * @param appId
	 * @param secretKey
	 */
	public IEEEsbLatch(String appId, String secretKey)
	{
		super(appId, secretKey);
		this.appID = appId;
		this.secret = secretKey;
		Log.LogEvent(Log.SUBTYPE.LATCH, "Inicializando plugin de Latch");
	}


	/* 
	 * Implementation of the HTTP_GET method that performs the requests to Latch servers. Internally called
	 * from Latch's SDK. Not my code, but easy to follow.
	 * Don't touch it!
	 */
	@Override
	public JsonElement HTTP_GET(String URL, Map<String, String> headers)
	{
		JsonElement responseJson = null; 
		try
		{
			SchemeRegistry schemeRegistry = new SchemeRegistry();
	        schemeRegistry.register(new Scheme("https",443,new EasySSLSocketFactory()));

	        DefaultHttpClient client = new DefaultHttpClient(new ThreadSafeClientConnManager(schemeRegistry));
			HttpGet request = new HttpGet(URL);

			for (String key : headers.keySet()) 
			{
				request.addHeader(key, headers.get(key));
			}

			ResponseHandler<JsonElement> rh = new ResponseHandler<JsonElement>() {
				@Override
				public JsonElement handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {

					StatusLine statusLine = response.getStatusLine();
					HttpEntity entity = response.getEntity();
					if (statusLine.getStatusCode() >= HttpStatus.SC_MULTIPLE_CHOICES) {
						throw new HttpResponseException(
								statusLine.getStatusCode(),
								statusLine.getReasonPhrase());
					}
					if (entity == null) {
						throw new ClientProtocolException(
								"Response contains no content");
					}
					Gson gson = new GsonBuilder().create();
					Reader reader = new InputStreamReader(entity.getContent());
					return gson.fromJson(reader, JsonElement.class);
				}
			};

			responseJson = client.execute(request, rh);

		}
		catch (Exception e) 
		{
			Log.LogError(Log.SUBTYPE.LATCH, "Error contactando con Latch: " + e.getMessage());
		} 

		return responseJson;
	}
	
	/**
	 * Performs a pairing request with Latch
	 * @param user
	 * @return the user account ID in case it success, null otherwise
	 */
	public synchronized String pair(Usuario user)
	{
		if (user == null)
			return null;
		String accID = null;
		try
		{
			Log.LogEvent(Log.SUBTYPE.LATCH, "El usuario "  + user.toString() + "ha solicitado emparejar su cuenta con Latch");
			LatchResponse response = pair(user.pairingToken);
			
			if(response.getError() != null)
			{
				Log.LogError(Log.SUBTYPE.LATCH, "Error emparejando al usuario " + user.toString() + " con Latch");
				return null;
			}
				
			JsonObject data = response.getData();
			accID = data.get("accountId").getAsString();
			Log.LogEvent(Log.SUBTYPE.LATCH, "Emparejamiento del usuario " + user.toString() + " completado");
		}
		catch(Exception e)
		{
			Log.LogError(Log.SUBTYPE.LATCH, "Error emparejando al usuario " + user.toString() + " con Latch");
			return null;
		}
		return accID;
	}
	
	/**
	 * Performs an upairing request with Latch
	 * @param user
	 * @return true if successful, false otherwise
	 */
	public synchronized boolean unpair(Usuario user)
	{
		if (user == null)
			return false;
		try
		{
			Log.LogEvent(Log.SUBTYPE.LATCH, "El usuario " + user.toString() + "ha solicitado desemparejar su cuenta de Latch");
			String accID = DBManager.getAccId(user.dni);
			LatchResponse response = unpair(accID);
			if(response.getError() != null)
			{
				Log.LogError(Log.SUBTYPE.LATCH, "Error desemparejando al usuario " + user.toString() + " de Latch");
				return false;
			}
			Log.LogEvent(Log.SUBTYPE.LATCH, "Desemparejamiento del usuario " + user.toString() + " completado");
			return true;
		}
		catch(Exception e)
		{
			Log.LogError(Log.SUBTYPE.LATCH, "Error desemparejando al usuario " + user.toString() + " de Latch");
			return false;
		}
		
	}

	/**
	 * @param user
	 * @return
	 */
	public synchronized boolean checkLatch(Usuario user)
	{
		if (user == null)
			return false;
		LatchResponse response = status(DBManager.getAccId(user.dni));
		if(response.getError() != null)
		{
			Log.LogError(Log.SUBTYPE.LATCH, "Se ha producido un error comprobando el latch de " + user.toString());
			return false;
		}
		JsonObject data = response.getData();
		JsonElement operations = data.get("operations");
		JsonElement appID = operations.getAsJsonObject().get(this.appID);
		String status = appID.getAsJsonObject().get("status").getAsString();
		if (status.equals("on"))
		{
			Log.LogEvent(Log.SUBTYPE.LATCH, "El usuario " + user.toString()
					+ " tiene su latch desbloqueado");
			return true;
		}
		Log.LogEvent(Log.SUBTYPE.LATCH, "El usuario " + user.toString()
				+ " tiene su latch bloqueado");
		return false;
	}

}
