package es.ieeesb.utils;

import com.sun.mail.smtp.SMTPTransport;

import java.security.Security;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 *
 * @author doraemon
 */
public class EmailManager
{
	private EmailManager()
	{
	}


	public static void Send(String recipientEmail, String filePath, String fileName) throws AddressException,
			MessagingException
	{
		Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

		// Get a Properties object
		Properties props = System.getProperties();
		props.setProperty("mail.smtps.host", PropertiesManager.getProperty("emailServer"));
		props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
		props.setProperty("mail.smtp.socketFactory.fallback", "false");
		props.setProperty("mail.smtp.port", PropertiesManager.getProperty("emailPort"));
		props.setProperty("mail.smtp.socketFactory.port", PropertiesManager.getProperty("emailPort"));
		props.setProperty("mail.smtps.auth", "true");

		/*
		 * If set to false, the QUIT command is sent and the connection is
		 * immediately closed. If set to true (the default), causes the
		 * transport to wait for the response to the QUIT command.
		 * 
		 * ref :
		 * http://java.sun.com/products/javamail/javadocs/com/sun/mail/smtp
		 * /package-summary.html
		 * http://forum.java.sun.com/thread.jspa?threadID=5205249 smtpsend.java
		 * - demo program from javamail
		 */
		props.put("mail.smtps.quitwait", "false");

		Session session = Session.getInstance(props, null);

		// -- Create a new message --
		final MimeMessage msg = new MimeMessage(session);

		// -- Set the FROM and TO fields --
		msg.setFrom(new InternetAddress("info@ieeesb.es"));
		msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail, false));


		msg.setSubject(PropertiesManager.getProperty("emailAbout"));
		msg.setSentDate(new Date());
		
	    MimeBodyPart attachPart = new MimeBodyPart();
	    MimeBodyPart messageBodyPart = new MimeBodyPart();
	    messageBodyPart.setContent(PropertiesManager.getProperty("emailText"), "text/html");

	    Multipart multipart = new MimeMultipart();
		

        DataSource source = new FileDataSource(filePath);
        attachPart.setDataHandler(new DataHandler(source));
        attachPart.setFileName(fileName);
        multipart.addBodyPart(messageBodyPart);
        multipart.addBodyPart(attachPart);

        msg.setContent(multipart);


		SMTPTransport t = (SMTPTransport) session.getTransport("smtps");

		t.connect(PropertiesManager.getProperty("emailServer"), PropertiesManager.getProperty("emailUsername"), PropertiesManager.getProperty("emailPassword"));
		t.sendMessage(msg, msg.getAllRecipients());
		t.close();
	}
}