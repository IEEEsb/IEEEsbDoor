package es.ieeesb.utils;

import java.util.ArrayList;
import java.util.TimerTask;

/**
 * @author Gregorio
 * Utility class that periodically erases users from lastUsers list in the main class. Used for flood prevention.
 */
public 	class UserTimerTask extends TimerTask {

    private final Usuario user;
    private final ArrayList<Usuario> lastUsers;


    /**
     * Constructor, takes the user attempting to open the door and a reference to the list of users currently trying to open
     * the door.
     * @param user
     * @param lastUsers
     */
    public UserTimerTask(Usuario user, ArrayList<Usuario> lastUsers)
    {
      this.user = user;
      this.lastUsers = lastUsers;
    }

    /* 
     * Everytime a timer runs this task, it looks for an occurrence of the user in the list and erases it.
     */
    public void run() 
    {
      while(lastUsers.contains(user))
    	  lastUsers.remove(user);
    }
}
