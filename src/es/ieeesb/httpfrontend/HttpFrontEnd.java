package es.ieeesb.httpfrontend;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

import es.ieeesb.utils.Log;
/**
 * @author Gregorio
 * Http server. It creates the handlers for the different services offered.
 */

public class HttpFrontEnd {
	
	HttpServer server;


	/**
	 * Constructor, initializes the server and creates the contexts.
	 */
	public HttpFrontEnd() {
		try {
			server = HttpServer.create(new InetSocketAddress(80), 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		server.createContext("/fridge", new FridgeHandler());
		server.createContext("/door", new DoorHandler());
		server.createContext("/3dprinter", new PrinterHandler());
		server.createContext("/latchPair", new LatchPairingHandler());
		server.createContext("/latchUnpair", new LatchUnpairingHandler());
		server.createContext("/ieeeNumber", new IEEENumberHandler());
		server.createContext("/version", new VersionHandler());
		server.createContext("/slic3r", new Slic3rHandler());
		server.createContext("/slic3rProfiles", new Slic3rProfilesHandler());
		server.setExecutor(null); // creates a default executor
	}



	public void start() 
	{
		Log.LogEvent(Log.SUBTYPE.HTTP, "Inicializando frontend HTTP");
		server.start();
	}

}
