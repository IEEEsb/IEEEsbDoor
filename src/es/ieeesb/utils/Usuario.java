package es.ieeesb.utils;

/**
 * @author Gregorio
 * Utility class that represents a user of the system.
 */
public class Usuario 
{
	public String dni;
	public String nombre;
	public String registrationID;
	public String token;
	public String accID;
	public String pairingToken;
	public String email;
	
	public Usuario(String dni, String nombre) 
	{
		this.dni = dni;
		this.nombre = nombre;
	}


	@Override
	public String toString() {
		return "[DNI=" + dni + ", Nombre=" + nombre + "]";
	}
	@Override
	public boolean equals(Object obj) 
	{
		if(obj == null) return false;
		Usuario anotherUser = (Usuario)obj;
		return anotherUser.dni.equals(this.dni) || anotherUser.token.equals(this.token);
	}
	

}
