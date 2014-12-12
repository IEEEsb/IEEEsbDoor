package es.ieeesb.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;





/**
 * @author Gregorio
 * DBManager, pretty self-explanatory. Each method tries to connect to the proper database and retrieve the requested data.
 * Can't read SQL? Too bad, GTFO.
 */
public class DBManager {
	public static String url, user, pass;
	private static Connection connection;
	private static final String USER_IN_DB = "SELECT * FROM doorUsers WHERE dni=?";
	private static final String TOKEN_IN_DB = "SELECT * FROM doorTokens WHERE Token=? AND DNI=?";
	private static final String USER_DNI_FROM_REGID = "SELECT dni FROM doorUsers WHERE RegistrationID=?";
	private static final String USER_CREDIT = "SELECT credit FROM users WHERE dni=?";
	private static final String USER_REG_VALID = "SELECT dni FROM doorUsers WHERE RegistrationID=? AND IDUsed='0' AND dni=?";
	private static final String INVALIDATE_REGID = "UPDATE doorUsers SET IDUsed='1' WHERE RegistrationID=? AND dni=?";
	private static final String STORE_TOKEN = "INSERT INTO doorTokens (Name, DNI, Token) VALUES (?, ?, ?)";
	private static final String GET_ACCID = "SELECT * FROM doorUsers WHERE dni=?";
	private static final String USING_LATCH = "SELECT * FROM doorUsers WHERE dni=? and UsingLatch='1'";
	private static final String INSERT_ACCID = "UPDATE doorUsers SET LatchAccountId=?, UsingLatch='1' WHERE dni=?";
	private static final String DELETE_ACCID = "UPDATE doorUsers SET LatchAccountId=NULL, UsingLatch='0' WHERE dni=?";
	private static final String LOG = "INSERT INTO doorLog (TimeStamp, Type, Subtype, Message) VALUES (NOW(), ?, ?, ?)";
	private static final String GET_VERSION = "SELECT * from versions WHERE OS=?";
	
	private static final String DOORDB_ADDRESS = PropertiesManager.getProperty("doorDBAddress");
	private static final String DOORDB_USERNAME = PropertiesManager.getProperty("doorDBUsername");
	private static final String DOORDB_PASSWORD = PropertiesManager.getProperty("doorDBPassword");
	
	private static final String FRIDGEDB_ADDRESS = PropertiesManager.getProperty("fridgeDBAddress");
	private static final String FRIDGEDB_USERNAME = PropertiesManager.getProperty("fridgeDBUsername");
	private static final String FRIDGEDB_PASSWORD = PropertiesManager.getProperty("fridgeDBPassword");
	
	private static final String LOGDB_ADDRESS = PropertiesManager.getProperty("logDBAddress");
	private static final String LOGDB_USERNAME = PropertiesManager.getProperty("logDBUsername");
	private static final String LOGDB_PASSWORD = PropertiesManager.getProperty("logDBPassword");
	
	
	
	public static boolean isUserInDatabase( String dni ) {
		boolean userFound = false;
		try {
			Class.forName( "com.mysql.jdbc.Driver" );
			connection = DriverManager.getConnection(DOORDB_ADDRESS, DOORDB_USERNAME, DOORDB_PASSWORD);
			PreparedStatement preparedStatment = connection.prepareStatement( USER_IN_DB );
			dni = dni.trim();
			preparedStatment.setString(1, dni);
			ResultSet resultSet = preparedStatment.executeQuery();
			userFound = resultSet.next();
			resultSet.close();
			connection.close();
		} catch (Exception e) {
			Log.LogError(Log.SUBTYPE.DOORDB, "Error de base de datos: " + e.getMessage());
		} 
		return userFound;
	}
	
	public static boolean isUsingLatch( String userIdentification ) {
		boolean usingLatch = false;
		try {
			Class.forName( "com.mysql.jdbc.Driver" );
			connection = DriverManager.getConnection(DOORDB_ADDRESS, DOORDB_USERNAME, DOORDB_PASSWORD);
			PreparedStatement preparedStatment = connection.prepareStatement( USING_LATCH );
			userIdentification = userIdentification.trim();
			preparedStatment.setString(1, userIdentification);
			ResultSet resultSet = preparedStatment.executeQuery();
			usingLatch = resultSet.next();
			resultSet.close();
			connection.close();
		} catch (Exception e) {
			Log.LogError(Log.SUBTYPE.DOORDB, "Error de base de datos: " + e.getMessage());
		} 
		return usingLatch;
	}
	
	public static void insertAccId(Usuario user)
	{
		try {
			Class.forName( "com.mysql.jdbc.Driver" );
			connection = DriverManager.getConnection(DOORDB_ADDRESS, DOORDB_USERNAME, DOORDB_PASSWORD);
			PreparedStatement preparedStatment = connection.prepareStatement( INSERT_ACCID );
			String accID = user.accID.trim();
			user.dni = user.dni.trim();
			preparedStatment.setString(1, accID);
			preparedStatment.setString(2, user.dni);
			preparedStatment.executeUpdate();
			connection.close();
		}catch (Exception e) {
			Log.LogError(Log.SUBTYPE.DOORDB, "Error de base de datos: " + e.getMessage());
		} 
	}
	
	public static void deleteAccID(Usuario user)
	{
		try {
			Class.forName( "com.mysql.jdbc.Driver" );
			connection = DriverManager.getConnection(DOORDB_ADDRESS, DOORDB_USERNAME, DOORDB_PASSWORD);
			PreparedStatement preparedStatment = connection.prepareStatement( DELETE_ACCID );
			user.dni = user.dni.trim();
			preparedStatment.setString(1, user.dni);
			preparedStatment.executeUpdate();
			connection.close();
		}catch (Exception e) {
			Log.LogError(Log.SUBTYPE.DOORDB, "Error de base de datos: " + e.getMessage());
		} 
	}
	
	public static String getVersion(String OS)
	{
		String version = "";
		try 
		{
			Class.forName( "com.mysql.jdbc.Driver" );
			connection = DriverManager.getConnection(DOORDB_ADDRESS, DOORDB_USERNAME, DOORDB_PASSWORD);
			PreparedStatement preparedStatment = connection.prepareStatement(GET_VERSION);
			OS = OS.trim();
			preparedStatment.setString(1, OS);
			ResultSet resultSet = preparedStatment.executeQuery();
			
			if(resultSet.next())
			{
				version = resultSet.getString("Version");
			}
			resultSet.close();
			connection.close();
		} 
		catch (Exception e) 
		{
			Log.LogError(Log.SUBTYPE.DOORDB, "Error de base de datos: " + e.getMessage());
			return version;
		} 
		return version;
	}
	
	public static String getAccId( String dni)
	{
		String accID = "";
		try 
		{
			Class.forName( "com.mysql.jdbc.Driver" );
			connection = DriverManager.getConnection(DOORDB_ADDRESS, DOORDB_USERNAME, DOORDB_PASSWORD);
			PreparedStatement preparedStatment = connection.prepareStatement( GET_ACCID );
			dni = dni.trim();
			preparedStatment.setString(1, dni);
			ResultSet resultSet = preparedStatment.executeQuery();
			
			if(resultSet.next())
			{
				accID = resultSet.getString("LatchAccountId");
			}
			resultSet.close();
			connection.close();
		} 
		catch (Exception e) 
		{
			Log.LogError(Log.SUBTYPE.DOORDB, "Error de base de datos: " + e.getMessage());
			return accID;
		} 
		return accID;
	}
	
	public static boolean isTokenInDatabase( Usuario user ) {
		boolean userFound = false;
		try {
			Class.forName( "com.mysql.jdbc.Driver" );
			connection = DriverManager.getConnection(DOORDB_ADDRESS, DOORDB_USERNAME, DOORDB_PASSWORD);
			PreparedStatement preparedStatment = connection.prepareStatement( TOKEN_IN_DB );
			String token = user.token.trim();
			String dni = user.dni.trim();
			preparedStatment.setString(1, token);
			preparedStatment.setString(2, dni);
			ResultSet resultSet = preparedStatment.executeQuery();
			userFound = resultSet.next();
			resultSet.close();
			connection.close();
		} catch (Exception e) {
			Log.LogError(Log.SUBTYPE.DOORDB, "Error de base de datos: " + e.getMessage());
		} 
		return userFound;
	}
	
	public static double checkUserCredit(String userIdentification)
	{
		boolean userFound;
		double credit = -1000;
		try {
			Class.forName( "com.mysql.jdbc.Driver" );
			connection = DriverManager.getConnection(FRIDGEDB_ADDRESS, FRIDGEDB_USERNAME, FRIDGEDB_PASSWORD);
			PreparedStatement preparedStatment = connection.prepareStatement( USER_CREDIT );
			userIdentification = userIdentification.trim();
			preparedStatment.setString(1, userIdentification);
			ResultSet resultSet = preparedStatment.executeQuery();
			userFound = resultSet.next();
			if(userFound)
				credit = resultSet.getDouble(1);
			resultSet.close();
			connection.close();
		} catch (Exception e) {
			Log.LogError(Log.SUBTYPE.DOORDB, "Error de base de datos: " + e.getMessage());
		} 
		return credit;
	}
	
	public static void invalidateRegisterID(Usuario user)
	{
		try {
			Class.forName( "com.mysql.jdbc.Driver" );
			connection = DriverManager.getConnection(DOORDB_ADDRESS, DOORDB_USERNAME, DOORDB_PASSWORD);
			PreparedStatement preparedStatment = connection.prepareStatement( INVALIDATE_REGID );
			String registerID = user.registrationID.trim();
			String dni = user.dni.trim();
			preparedStatment.setString(1, registerID);
			preparedStatment.setString(2, dni);
			preparedStatment.executeUpdate();
			connection.close();
		}catch (Exception e) {
			Log.LogError(Log.SUBTYPE.DOORDB, "Error de base de datos: " + e.getMessage());
		} 
	}
	
	public static boolean validRegisterID(Usuario user)
	{
		boolean valid = false;

		try {
			Class.forName( "com.mysql.jdbc.Driver" );
			connection = DriverManager.getConnection(DOORDB_ADDRESS, DOORDB_USERNAME, DOORDB_PASSWORD);
			PreparedStatement preparedStatment = connection.prepareStatement( USER_REG_VALID );
			String registerID = user.registrationID.trim();
			String dni = user.dni.trim();
			preparedStatment.setString(1, registerID);
			preparedStatment.setString(2, dni);
			ResultSet resultSet = preparedStatment.executeQuery();
			valid = resultSet.next();
			resultSet.close();
			connection.close();
		} catch (Exception e) {
			Log.LogError(Log.SUBTYPE.DOORDB, "Error de base de datos: " + e.getMessage());
		} 
		return valid;
	}
	
	public static String dniFromRegisterID(String registerID)
	{
		String DNI = "";
		try {
			Class.forName( "com.mysql.jdbc.Driver" );
			connection = DriverManager.getConnection(DOORDB_ADDRESS, DOORDB_USERNAME, DOORDB_PASSWORD);
			PreparedStatement preparedStatment = connection.prepareStatement( USER_DNI_FROM_REGID );
			registerID = registerID.trim();
			preparedStatment.setString(1, registerID);
			ResultSet resultSet = preparedStatment.executeQuery();
			if(resultSet.next())
			{
				DNI = (String)resultSet.getString(1);
			}
			resultSet.close();
			connection.close();
		} catch (Exception e) {
			Log.LogError(Log.SUBTYPE.DOORDB, "Error de base de datos: " + e.getMessage());
		} 
		return DNI;
	}
	
	public static void storeToken(Usuario user, String token)
	{
		try {
			Class.forName( "com.mysql.jdbc.Driver" );
			connection = DriverManager.getConnection(DOORDB_ADDRESS, DOORDB_USERNAME, DOORDB_PASSWORD);
			PreparedStatement preparedStatment = connection.prepareStatement( STORE_TOKEN );
			preparedStatment.setString(1, user.nombre.trim());
			preparedStatment.setString(2, user.dni.trim());
			preparedStatment.setString(3, token.trim());
			preparedStatment.execute();
			connection.close();
		} catch (Exception e) {
			Log.LogError(Log.SUBTYPE.DOORDB, "Error de base de datos: " + e.getMessage());
		} 
	}
	
	public static void log(String type, String subtype, String message)
	{
		try {
			Class.forName( "com.mysql.jdbc.Driver" );
			connection = DriverManager.getConnection(LOGDB_ADDRESS, LOGDB_USERNAME, LOGDB_PASSWORD);
			PreparedStatement preparedStatment = connection.prepareStatement( LOG );
			preparedStatment.setString(1, type);
			preparedStatment.setString(2, subtype);
			preparedStatment.setString(3, message);
			preparedStatment.execute();
			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
