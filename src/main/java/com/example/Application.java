package com.example;


import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCredential;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Enumeration;

@SpringBootApplication
public class Application {
    
    @Autowired
    private Environment env;

    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    
    @Bean
    CommandLineRunner commandLineRunner( ) {
        return args -> {
            
            String SCOPE = env.getProperty("mail.oauth2.SCOPE");
            String AUTHORITY = env.getProperty("mail.oauth2.AUTHORITY");
            String Application_client_ID = env.getProperty("mail.oauth2.application_client_id");
            String oauth_secret_value = env.getProperty("mail.oauth2.secret_value");
            String host = env.getProperty("mail.host");
            String username = env.getProperty("mail.username");
            
            String token = getToken(SCOPE,AUTHORITY,Application_client_ID,oauth_secret_value);
            
            if (token!=null)
                connectToOutlookOauth2 ( host, username, token );
            
        };    
    }

    
    private void connectToOutlookOauth2(String host, String userName, String accessToken) {
        
        System.out.println("=== Connecting via Oauth2 ");
        
        //String encodedToken = tokenEncoder( userName, accessToken ); 

        Properties props = new Properties();
        
        props.setProperty("mail.imap.ssl.protocols", "TLSv1.2");
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.ssl.trust", "*");
        props.put("mail.imap.starttls.enable", "true");
        props.put("mail.imap.auth.mechanisms", "XOAUTH2");
        props.put("mail.imap.auth.login.disable", "true");
        props.put("mail.imap.auth.plain.disable", "true");
        props.put("mail.imap.sasl.enable", "true");
        props.put("mail.imap.sasl.mechanisms", "XOAUTH2");
        props.put("mail.imap.sasl.mechanisms.oauth2.oauthToken", accessToken); 
        
        if ( env.getProperty("mail.debug").equalsIgnoreCase("true")) {
            props.put("mail.debug", "true");
            props.put("mail.debug.auth", "true");
        }
              
        try {
            Session session = Session.getInstance(props);

            System.out.println("=== Connecting to imap ..." );

            Store store = session.getStore("imap");

            store.connect(host, userName, accessToken); // The OAuth 2.0 Access Token should be passed as the password, and not base64 encoded 
            
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            int messageCount = inbox.getMessageCount();

            System.out.println("=== Message count in INBOX: " + messageCount);
            
            // saving a message to file
            //saveMessageToFile(  inbox.getMessage(messageCount-1) , "/tmp/test.eml");
            
            inbox.close(false);
            store.close();
            
            System.out.println("=== Disconnected.");
            
        } catch (MessagingException e) {
          e.printStackTrace();
        }
        
    }


    //private String tokenEncoder(String userName, String accessToken) {
    //    String plainString = "user=" + userName + "^Aauth=Bearer " + accessToken + "^A^A" ;
    //    String encodeBytes = java.util.Base64.getEncoder().encodeToString(( plainString ).getBytes());
    //    return encodeBytes;
    //}


    private String getToken(String SCOPE, String AUTHORITY, String Application_client_ID, String oauth_secret_value  ) {
        
        String mytoken = null ;

        try{
            Set<String> lstScope = new HashSet<>();
            lstScope.add(SCOPE);

            IClientCredential cred = ClientCredentialFactory.createFromSecret( oauth_secret_value );
            ConfidentialClientApplication cca = ConfidentialClientApplication.builder(Application_client_ID, cred).authority(AUTHORITY).build();
            ClientCredentialParameters parameters = ClientCredentialParameters.builder(lstScope).build();
            IAuthenticationResult token = cca.acquireToken(parameters).join();
            
            mytoken = token.accessToken() ;

            if ( env.getProperty("mail.token.debug").equalsIgnoreCase("true")) {
                System.out.println("=========== TOKEN ===========");
                System.out.println("Token  : " + token.accessToken());
                System.out.println("Scope  : " + token.scopes());
                System.out.println("Expires: " + token.expiresOnDate());
                System.out.println("Payload: " + tokenDecodePayload( token.accessToken() ) );
                System.out.println("=============================");
            }
             
        }catch(Exception e){
            e.printStackTrace();
        }
        
        return mytoken;
    }
    
    
    private String tokenDecodePayload(String token) {
        String[] chunks = token.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        //String header = new String(decoder.decode(chunks[0]));
        String payload = new String(decoder.decode(chunks[1]));
        return payload;
    }

    

    public static void saveMessageToFile(Message msg, String filePath) {
        
        System.out.println("=== saving mesate to: " + filePath);
        
        
        try {
            byte[] bytes = messageToBytes(msg);
            
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(bytes);
            }
        
        }catch (Exception e) {
            
            System.out.println(e);
            
        }
        
    }

    private static byte[] messageToBytes(Message msg) throws MessagingException, IOException {
        byte[] bytes =null ;
        
        String headers = "" ;
        Enumeration<Header> headerEnumeration = msg.getAllHeaders();
        while (headerEnumeration.hasMoreElements()) {
            Header header = (Header) headerEnumeration.nextElement();
            String name = header.getName();
            String value = header.getValue();
            headers += name+": "+value+"\r\n" ;
        }
        headers += "\r\n" ;
          
        Object content = msg.getContent();
                
        if (content instanceof Multipart) {  
            
            ByteArrayOutputStream fos = new  ByteArrayOutputStream();

            fos.write( headers.getBytes() );

            Multipart x = (Multipart)msg.getContent();
            x.writeTo( fos );

            fos.close();

            bytes = fos.toByteArray()  ;

         } else {
           
            String text_body = msg.getContent().toString();
            if (msg.getContentType().toLowerCase().contains("html")) {
                text_body=text_body.replace("=", "=3D"); 
            }

            headers = headers.replace("Content-Transfer-Encoding: base64", ""); 
            String fullBody = headers + text_body ;
            bytes = fullBody.getBytes()  ;
        }
        
        return bytes ;
    }
    
}
