/**
 * The MIT License
 *
 * Copyright (c) 2010-2012 www.myjeeva.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE. 
 * 
 */
package es.ieeesb.utils; 

import java.util.Properties;


import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * Query Active Directory using Java
 * 
 * @filename ActiveDirectory.java
 * @author <a href="mailto:jeeva@myjeeva.com">Jeevanandam Madanagopal</a>
 * @copyright &copy; 2010-2012 www.myjeeva.com
 */
public class ActiveDirectory {


    //required private variables   
    private static Properties properties;
    private static DirContext dirContext;
    private static SearchControls searchCtls;
	private static String[] returnAttributes = { "sAMAccountName", "givenName", "cn", "mail", "EmployeeNumber", "EmployeeId" };
    private static String domainBase;
    private static String baseFilter = "(&((&(objectCategory=Person)(objectClass=User)))";

    /**
     * constructor with parameter for initializing a LDAP context
     * 
     * @param username a {@link java.lang.String} object - username to establish a LDAP connection
     * @param password a {@link java.lang.String} object - password to establish a LDAP connection
     * @param domainController a {@link java.lang.String} object - domain controller name for LDAP connection
     */
    public ActiveDirectory() 
    {

    }
    
    public static void openConnection() throws NamingException
    {
        properties = new Properties();        

        properties.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        properties.put(Context.PROVIDER_URL, PropertiesManager.getProperty("ADAddress"));
        properties.put(Context.SECURITY_AUTHENTICATION, "simple");
        properties.put(Context.SECURITY_PRINCIPAL, PropertiesManager.getProperty("ADAdminUsername"));
        properties.put(Context.SECURITY_CREDENTIALS, PropertiesManager.getProperty("ADAdminPassword"));

        
        //initializing active directory LDAP connection
        dirContext = new InitialDirContext(properties);

        
        //default domain base for search
        domainBase = getDomainBase(PropertiesManager.getProperty("ADDomain"));
        
        //initializing search controls
        searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchCtls.setReturningAttributes(returnAttributes);
    }
    
    
    /**
     * search the Active directory by username/email id for given search base
     * 
     * @param searchValue a {@link java.lang.String} object - search value used for AD search for eg. username or email
     * @param searchBy a {@link java.lang.String} object - scope of search by username or by email id
     * @param searchBase a {@link java.lang.String} object - search base value for scope tree for eg. DC=myjeeva,DC=com
     * @return search result a {@link javax.naming.NamingEnumeration} object - active directory search result
     * @throws NamingException
     */
    public static NamingEnumeration<SearchResult> searchUser(String searchValue, String searchBy, String searchBase) throws NamingException {
    	Log.LogEvent(Log.SUBTYPE.ACTIVE_DIRECTORY, "Abriendo conexión con active directory");
    	try
    	{
    		openConnection();
    	}
    	catch(Exception e)
    	{
    		Log.LogError(Log.SUBTYPE.ACTIVE_DIRECTORY, "Error contactando con active directory: " + e.getMessage());
    		return null;
    	}
    	String filter = getFilter(searchValue, searchBy);    	
    	String base = (null == searchBase) ? domainBase : getDomainBase(searchBase); // for eg.: "DC=myjeeva,DC=com";
    	
		return dirContext.search(base, filter, searchCtls);
    }

    /**
     * closes the LDAP connection with Domain controller
     */
    public static void closeConnection(){
        try {
            if(dirContext != null)
                dirContext.close();
            Log.LogEvent(Log.SUBTYPE.ACTIVE_DIRECTORY, "Conexión con active directory cerrada");
        }
        catch (NamingException e) {
        	Log.LogError(Log.SUBTYPE.ACTIVE_DIRECTORY, "Error contactando con active directory: " + e.getMessage());          
        }
    }
    
    /**
     * active directory filter string value
     * 
     * @param searchValue a {@link java.lang.String} object - search value of username/email id for active directory
     * @param searchBy a {@link java.lang.String} object - scope of search by username or email id
     * @return a {@link java.lang.String} object - filter string
     */
    private static String getFilter(String searchValue, String searchBy) 
    {
    	String filter = baseFilter;    	
    	if(searchBy.equals("email")) 
    	{
    		filter += "(mail=" + searchValue + "))";
    	} 
    	else if(searchBy.equals("username")) 
    	{
    		filter += "(samaccountname=" + searchValue + "))";
    	}
    	else if(searchBy.equals("DNI"))
    	{
    		filter += "(EmployeeNumber=" + searchValue + "))";
    	}
		return filter;
	}
    
    /**
     * creating a domain base value from domain controller name
     * 
     * @param base a {@link java.lang.String} object - name of the domain controller
     * @return a {@link java.lang.String} object - base name for eg. DC=myjeeva,DC=com
     */
	private static String getDomainBase(String base) {
		char[] namePair = base.toUpperCase().toCharArray();
		String dn = "DC=";
		for (int i = 0; i < namePair.length; i++) {
			if (namePair[i] == '.') {
				dn += ",DC=" + namePair[++i];
			} else {
				dn += namePair[i];
			}
		}
		return dn;
	}
}
