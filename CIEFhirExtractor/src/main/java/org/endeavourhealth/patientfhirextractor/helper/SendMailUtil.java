package org.endeavourhealth.patientfhirextractor.helper;

import com.fasterxml.jackson.databind.JsonNode;
import org.endeavourhealth.patientfhirextractor.constants.ResourceConstants;
import org.endeavourhealth.patientfhirextractor.models.CIEExtractorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.Properties;

public class SendMailUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SendMailUtil.class);

    public static boolean sendMail(CIEExtractorConfig configurations, String from, String subject, String...  attachment){
        LOG.info("Start of sendMail");

        boolean mailSend = false;

        // Get system properties
        Properties properties = System.getProperties();

        // Setup mail server
        properties.put("mail.smtp.host", ResourceConstants.SMTP_HOST);
        properties.put("mail.smtp.port", ResourceConstants.SMTP_PORT);
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.socketFactory.port", ResourceConstants.SMTP_PORT);
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        // Get the Session object.
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(ResourceConstants.SMTP_SERVER_USERNAME, ResourceConstants.SMTP_SERVER_PWD);
            }

        });
        //session.setDebug(true);
        try {
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);
            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));
            // Set To: header field of the header.
            for(JsonNode to_emailId:configurations.getEmailIds()){
                String to = to_emailId.asText();
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            }

            // Set Subject: header field
            message.setSubject(subject);
            Multipart multipart = new MimeMultipart();
            MimeBodyPart textPart = new MimeBodyPart();
            MimeBodyPart attachmentPart = new MimeBodyPart();

            try {

                textPart.setText("Please find the Audit Error report");
                multipart.addBodyPart(textPart);
                if(attachment!=null) {

                    attachmentPart.attachFile(attachment[0]);
                    multipart.addBodyPart(attachmentPart);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            message.setContent(multipart);
            LOG.info("sending...");
            // Send message
            Transport.send(message);
            LOG.info("Sent message successfully....");
            mailSend = true;
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
        LOG.info("End of sendMail");
        return mailSend;
    }

}
