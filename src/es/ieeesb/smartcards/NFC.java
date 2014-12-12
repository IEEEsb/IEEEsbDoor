package es.ieeesb.smartcards;


import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import es.ieeesb.ieeesbdoor.Main;
import es.ieeesb.utils.DBManager;
import es.ieeesb.utils.Log;
import es.ieeesb.utils.Usuario;
/**
 * @author Gregorio
 * Handler that controls NFC authentication. Work in progress
 */

public class NFC implements Runnable {

	public static final char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static final String CARDREADER = "OMNIKEY CardMan 5x21-CL 0";
	
	public static CardTerminal terminal;

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	/**
	 * Constructor. Looks for Smartcard readers.
	 */
	public NFC()
	{
		Log.LogEvent(Log.SUBTYPE.NFC, "Inicializando sistema de acceso por NFC");
		terminal = null;
		TerminalFactory factory;
		try 
		{
			factory = TerminalFactory.getInstance("PC/SC", null);
			List<CardTerminal> terminals = factory.terminals().list();
			if (terminals.isEmpty()) 
			{
				throw new Exception("Lector no disponible");
			}
			
			for (CardTerminal terminalSelection : terminals) 
			{
				if (terminalSelection.getName().equals(CARDREADER))
					terminal = terminalSelection;
			}
		} 
		catch (Exception e) 
		{
			Log.LogError(Log.SUBTYPE.NFC, "Error de NFC: " + e.getMessage());
		}
	}

	/* 
	 * Continuosly tries to read the smartcard reader and opens the door if it should. Also checks users's Latch.
	 */
	public void run() 
	{
		while (true) 
		{
			try {
				Usuario usuarioEntrante = leerNFC();
				if (usuarioEntrante != null) 
				{
					if(DBManager.isUserInDatabase(usuarioEntrante.dni))
					{
						if(!DBManager.isUsingLatch(usuarioEntrante.dni) || Main.latch.checkLatch(usuarioEntrante))
						{
							Log.LogEvent(Log.SUBTYPE.NFC, "Acceso autorizado a "
									+ usuarioEntrante.toString());
							Main.openDoor(usuarioEntrante);
						}
					}
					else
					{
						Log.LogEvent(Log.SUBTYPE.NFC, "Acceso denegado a "
								+ usuarioEntrante.toString());
					}
					try
					{
						Thread.sleep(2000);
					}
					catch (InterruptedException e)
					{
						Log.LogError(Log.SUBTYPE.DNI, "Error en el hilo de autenticación por NFC: " + e.getMessage());
					}
				}

			} 
			catch (Exception e) 
			{
				
			}

		}
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public Usuario leerNFC() throws Exception 
	{
		terminal.waitForCardPresent(0);
		try 
		{
			Card card = terminal.connect("*");
			CardChannel channel = card.getBasicChannel();

			CommandAPDU command = new CommandAPDU(new byte[] { (byte) 0xFF,
					(byte) 0xB0, (byte) 0x00, (byte) 0x03, (byte) 0x10 });
			ResponseAPDU response = channel.transmit(command);

			byte[] byteArray = response.getBytes();
			byte[] trimmed = new byte[10];
			for (int i = 0; i < trimmed.length - 1; i++) {
				trimmed[i] = byteArray[i + 1];
			}
			String result = new String(trimmed, "UTF-8");
			result = result.substring(0, 9);
			return new Usuario(result, "");
		} 
		catch (CardException e) 
		{
			Log.LogError(Log.SUBTYPE.NFC, "Error leyendo NFC: " + e.getMessage());
			return null;
		}
	}

}
