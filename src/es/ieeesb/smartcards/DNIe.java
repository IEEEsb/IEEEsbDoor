package es.ieeesb.smartcards;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.smartcardio.*;

import es.ieeesb.ieeesbdoor.Main;
import es.ieeesb.utils.DBManager;
import es.ieeesb.utils.Log;
import es.ieeesb.utils.Usuario;
/**
 * @author Jandion
 * Gregorio is writing this. I only know it returns the DNI number after reading it from the smartcard reader. Ask jandion 
 * for details. Lol.
 */

public class DNIe implements Runnable{

	private static final byte[] dnie_v_1_0_Atr = {
		(byte)0x3B, (byte)0x7F, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x6A, (byte)0x44,
		(byte)0x4E, (byte)0x49, (byte)0x65, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
		(byte)0x00, (byte)0x00, (byte)0x90, (byte)0x00};

	private static final byte[] dnie_v_1_0_Mask = {
		(byte)0xFF, (byte)0xFF, (byte)0x00, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
		(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
		(byte)0x00, (byte)0x00, (byte)0xFF, (byte)0xFF};

	private static final byte[] command1 = new byte[] {
		(byte)0x00, (byte)0xa4, (byte)0x04, (byte)0x00, (byte)0x0b, (byte)0x4D, (byte)0x61, (byte)0x73, 
		(byte)0x74, (byte)0x65, (byte)0x72, (byte)0x2E, (byte)0x46, (byte)0x69, (byte)0x6C, (byte)0x65};

	private static final byte[] command2 = new byte[] {
		(byte)0x00, (byte)0xA4, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x50, (byte)0x15};

	private static final byte[] command3 = new byte[] {	
		(byte)0x00, (byte)0xA4, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x60, (byte)0x04};

	private static final byte[] command4 = new byte[] {	
		(byte)0x00, (byte)0xB0, (byte)0x00, (byte)0x00, (byte)0xFF};

	private static final Pattern p = Pattern.compile("([A-Z ]*), ([A-Z ]*?) .AUTENTICACI");
	private static final Pattern p2 = Pattern.compile("([0-9]{8}[A-Z]{1})");

	public Usuario LeerNIF(){
		Usuario usuarioEntrante=null;
		try {

			Card c = ConexionTarjeta();
			if (c == null) {
				String response = "No se ha encontrado ninguna tarjeta";
				throw new Exception(response);
			}
			byte[] atr = c.getATR().getBytes();
			CardChannel ch = c.getBasicChannel();

			if (esDNIe(atr)) {
				usuarioEntrante = leerDeCertificado(ch);
			}
			c.disconnect(false);

		} catch (Exception ex) {
			return null;
		}
		return usuarioEntrante;
	}

	public Usuario leerDeCertificado(CardChannel ch) throws CardException {
		int offset = 0;
		Usuario usuarioEntrante = null;
		ResponseAPDU r = ch.transmit(new CommandAPDU(command1));
		//Seleccionamos el directorio PKCS#15 5015
		r = ch.transmit(new CommandAPDU(command2));
		//Seleccionamos el Certificate Directory File (CDF) del DNIe 6004
		r = ch.transmit(new CommandAPDU(command3));
		//Leemos FF bytes del archivo
		r = ch.transmit(new CommandAPDU(command4));

		if( (byte)r.getSW() == (byte)0x9000){
			byte[] r2 = r.getData();
			//System.out.println(new String(r2));

			if(r2[4]==0x30){
				offset = r2[5]+6; //Obviamos la seccion del Label
			}

			if(r2[offset]==0x30){
				offset += r2[offset+1]+2; //Obviamos la seccion de la informacion sobre la fecha de expedición etc
			}

			if ( (byte)r2[offset] == (byte) 0xA1){
				//El certificado empieza aquí
				byte[] r3 = new byte[25];

				//Nos posicionamos en el byte donde empieza el NIF y leemos sus 9 bytes
				System.arraycopy(r2, 104, r3, 0, 25);
				Matcher m2 = p2.matcher(new String(r3));
				Matcher m = p.matcher(new String(r2));
				m.find();
				m2.find();
				usuarioEntrante = new Usuario(
						m2.group(1), 
						m.group(2)+" "+m.group(1));

			}
		}        
		
		
		return usuarioEntrante;
	}

	private Card ConexionTarjeta() throws Exception {

		Card card = null;
		TerminalFactory factory = TerminalFactory.getDefault();
		List<CardTerminal> terminals = factory.terminals().list();
		for (int i = 0; i < terminals.size(); i++) {
			// get terminal
			CardTerminal terminal = terminals.get(i);
			//terminal.waitForCardPresent(Long.MAX_VALUE);
			try {
				if(terminal.isCardPresent()) {
					card = terminal.connect("T=0");
				}
			} catch (Exception e) 
			{
				card = null;
			}
		}
		return card;
	}

	private boolean esDNIe(byte[] atrCard) {
		int j = 0;
		boolean found = false;

		//Es una tarjeta DNIe?
		if(atrCard.length == dnie_v_1_0_Atr.length) {
			found = true;
			while(j < dnie_v_1_0_Atr.length && found) {
				if((atrCard[j] & dnie_v_1_0_Mask[j]) != (dnie_v_1_0_Atr[j] & dnie_v_1_0_Mask[j]))
					found = false; //No es una tarjeta DNIe
				j++;
			}
		}
		//return found;
		return true;

	}

	@Override
	public void run() {
		Log.LogEvent(Log.SUBTYPE.DNI, "Inicializando sistema de acceso por DNI");
		while(true){
			Usuario usuarioEntrante = LeerNIF();
			if(usuarioEntrante!=null)
			{
				if(DBManager.isUserInDatabase(usuarioEntrante.dni))
				{
					if(!DBManager.isUsingLatch(usuarioEntrante.dni) || Main.latch.checkLatch(usuarioEntrante))
					{
						Log.LogEvent(Log.SUBTYPE.DNI, "Acceso autorizado a " + usuarioEntrante.toString());
						Main.openDoor(usuarioEntrante);
					}
				}
				else
				{
					Log.LogEvent(Log.SUBTYPE.DNI, "Acceso denegado a " + usuarioEntrante.toString());
				}
				try
				{
					Thread.sleep(2000);
				}
				catch (InterruptedException e)
				{
					Log.LogError(Log.SUBTYPE.DNI, "Error en el hilo de autenticaci�n por DNI: " + e.getMessage());
				}
			}
		}
	}

}
