package es.ieeesb.utils;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * @author Gregorio
 * Manages communication with the Arduino board.
 */
public class ProtocolManager implements SerialPortEventListener {

	private SerialPort serialPort;
	private BufferedWriter output;
	private BufferedReader input;

	/**
	 * Constructor, takes the port name and the speed. Match the speed in your Arduino sketch!
	 * @param port
	 * @param baudRate
	 */
	public ProtocolManager( String port, String baudRate ) {
		try {
			CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(port);
			serialPort = (SerialPort) portId.open("IEEEsb Door", 2000);
			serialPort.setSerialPortParams(
					Integer.parseInt(baudRate),
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
			input = new BufferedReader(
					new InputStreamReader(
							serialPort.getInputStream()));
			output = new BufferedWriter(new OutputStreamWriter(serialPort.getOutputStream()));
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
			Log.LogEvent(Log.SUBTYPE.DOOR, "Abriendo conexión con la puerta");
		} catch (Exception e) {
			Log.LogError(Log.SUBTYPE.DOOR, "Imposible conectar al puerto serie " + port);
		} 
	}

	/* 
	 * Just used to read incoming data
	 */
	@Override
	public synchronized  void serialEvent(SerialPortEvent event) {
		switch(event.getEventType()) {
		case SerialPortEvent.DATA_AVAILABLE:
			readPort();
			break;
		}
	}

	/**
	 * Reads incoming data and logs it
	 */
	private void readPort() {
		try {
			String inputLine = input.readLine();
			inputLine = inputLine.trim();
			if(!inputLine.equals("") && inputLine.length() > 5)
				Log.LogEvent(Log.SUBTYPE.DOOR, inputLine);
		} catch (IOException e) {
			Log.LogError(Log.SUBTYPE.DOOR, "Error de lectura del puerto serie: " + e.getMessage());
		}
	}
	


	/**
	 * Writes a string to the serial port. Used to open the door.
	 * @param message
	 */
	public void write( String message ) {
		try {
			output.write(message);
			output.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Closes everything beautifully in order to shut down the system.
	 */
	public void close()
	{
		try
		{
			input.close();
			output.close();
			serialPort.close();
		}
		catch(Exception e)
		{
			Log.LogError(Log.SUBTYPE.DOOR, "Error cerrando el puerto");
		}
	}
}
