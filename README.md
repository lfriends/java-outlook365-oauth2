
# Java Sample App to connect to outlook365 with OAuth2


This project is an example on how make a **client-to-server** connection to **outlook365** (or any similar service) over IMAP using **OAUTH2** authentication
without a user interaction (MFA)


## Table of Contents

* [Requirements](#requirements)
* [Registering an app on Azure](#registering-an-app-on-azure)
* [Authorizing access on the Exchange server](#authorizing-access-on-the-exchange-server)
* [Updating the app parameters](#updating-the-app-parameters)
* [Troubleshooting](#troubleshooting)
* [Reference Documentation](#reference-documentation)



## Requirements

In order to successfully run this sample app you need to:

1. register a new app on the azure portal
1. Authorize access on the Exchange server
1. Fill up the [`application.properties`](src/main/resources/application.properties) file, by adding 
    * the email account
    * the tenant id
    * the application id
    * the secret id  (OAuth2AppClientId, OAuth2AppClientSecret) by copying over from the keys section for your app.
1.  git-clone this project: `git clone https://github.com/lfriends/java-outlook365-oauth2`

 
## Registering an app on Azure

1. Sign in to the [Azure portal](https://portal.azure.com/)
1. If you have more tentants, switch to the desired one
1. Open **Azure Active Directory**
1. Click on **App registrations** on the left panel
<br><br>

1. Add a **new registration**
    1. set a name as you like (Il will be displayed on the access token info) 
    1. select "Accounts in any organizational directory" 
    1. click "Register"
<br><br>

1. Open up your newly create *App registration*

    * click **Authentication** on the left panel 
        * Add a platform: "Mobile and desktop" 
        * select: "https://login.microsoftonline.com/common/oauth2/nativeclient" 
        * add a redirect URI, eg: "http://localhost" (wont be used) 
        * click "Configure"
        
     * click **Certificates and secrets** 
        * New client secret
        * choose the name and duration
        * click "ADD"
        * **VERY IMPORTANT:** note down the Secret "Value" because it wont be visible again
        
     * goto **App permission**
         * "Add permission"
         * select tab "APIs my organization uses" > search: "Office 365 Exchange Online" > select

             * IMAP.AccessAsApp
             * IMAP.AccessAsApp
             * Mail.Read
             * Mail.Send (if you want to send)
             
         * click "Add permission"
         * **DON'T FORGET** to click "Grand admin consent for ..." to activate the grants

        
        

 
## Authorizing access on the Exchange server

this can **only** be done by powershell scripts on exchange

1. connect to powershell console on exchange (Help on how to connect can be found [here](https://learn.microsoft.com/en-us/powershell/exchange/connect-to-exchange-online-powershell?view=exchange-ps) )
1. Install ExchangeOnlineManagement

```
Install-Module -Name ExchangeOnlineManagement -allowprerelease Import-module ExchangeOnlineManagement Connect-ExchangeOnline -Organization   
```


  3.Register Service Principal in Exchange

```
New-ServicePrincipal -AppId <APPLICATION_ID> -ServiceId <OBJECT_ID> [-Organization <ORGANIZATION_ID>]   
```

Make sure to use ObjectId from *enterprise applications* rather than object id of *application*
as well described in this post 
[Exchange config: Make sure to use ObjectId from enterprise applications rather than object id of application ](https://stackoverflow.com/questions/74899182/how-to-read-my-outlook-mail-using-java-and-oauth2-0-with-application-regsitratio?answertab=scoredesc#tab-top)


 
## Updating the app parameters


Open the "application.properties" file and update with your IDs:

* **mail.username**: is the email account you want to access - eg: john.doe@example.com
* **mail.oauth2.secret_value**: use the secret value (not the ID) genereted ad tep #1.6 - Eg: XUad94~M...
* **mail.oauth2.application_client_id**: this the "Application (client) ID" visibile on the "overview" panel of the application
* **mail.oauth2.direcotry_tenant_id**: this the "Directory (tenant) ID" visibile on the "overview" panel of the application



## Troubleshooting

##### ERROR: OAUTH2 is not working?

 	
```
DEBUG IMAP: AUTH: PLAIN
.OR.
DEBUG IMAP: Can't load SASL authenticator 
```

verify you are using javaMail >= 1.6 
   
and that you have correcly set all the mail properties, like:

> props.put("mail.imap.sasl.enable", "true");  
> props.put("mail.imap.sasl.mechanisms", "XOAUTH2");  
> props.put("mail.imap.auth.mechanisms", "XOAUTH2");  
> props.put("mail.imap.auth.login.disable", "true");  
> props.put("mail.imap.auth.plain.disable", "true");  
> [...]   

Please note that in case you are connecting to "imaps"

> Store store = session.getStore("imaps");  

remember also to update all the **props** to **imaps** too 
   
> props.put("mail.imap**s**.sasl.enable", "true");  
> props.put("mail.imap**s**.sasl.mechanisms", "XOAUTH2");   
> [...]   

   

##### ERROR:  AADSTS7000215: Invalid client secret provided...

```
> AADSTS7000215: Invalid client secret provided Ensure the secret being sent in the request is the client secret value, not the client secret ID, for a secret added to app '*********'.
```
	
as the message is stating, ensure you are using the *SECRET VALUE* and not the *SECRET ID*
Note that you can copy che *Secret value* only once, as it has been created. As you close che creation page, the *Secret value* will never be visible.
In this case you will need to create d new *Secret value*


##### ERROR:  AUTHENTICATE failed

```
> DEBUG IMAP:  XOAUTH2  
> DEBUG IMAP:   
> A1 AUTHENTICATE XOAUTH2 **********   
> A1 NO AUTHENTICATE failed.   
> javax.mail.AuthenticationFailedException: AUTHENTICATE failed.  
```

You most probably have missed to set, or have set the wrong permissions over one of the followings:

1. Make sure you have set the proper rights on the azure application configuration
2. ensure you have ran the CLI commands to authorize the *application* to access the email folder requested (this can only be solved by powershells script on exchange) 

check this out [Application permissions .and. Exchange config](https://stackoverflow.com/questions/74899182/how-to-read-my-outlook-mail-using-java-and-oauth2-0-with-application-regsitratio?answertab=scoredesc#tab-top)

Please note that you need to wait up to **15 minutes** for these change to apply


## Reference Documentation

Further reference and credit for this project:

* [Exchange config: Make sure to use ObjectId from enterprise applications rather than object id of application ](https://stackoverflow.com/questions/74899182/how-to-read-my-outlook-mail-using-java-and-oauth2-0-with-application-regsitratio?answertab=scoredesc#tab-top)
* [MSFT Example for Java](https://github.com/Azure-Samples/ms-identity-msal-java-samples/tree/main/2.%20Client-Side%20Scenarios/Integrated-Windows-Auth-Flow#step-3--register-the-sample-with-your-azure-active-directory-tenant)
* [How to connect to Exchange powershell console](https://learn.microsoft.com/en-us/powershell/exchange/connect-to-exchange-online-powershell?view=exchange-ps)

