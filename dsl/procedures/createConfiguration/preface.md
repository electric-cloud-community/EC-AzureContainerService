  #### Prerequisites
  1. EC-Kubernetes plugin version 1.0.5 or higher
  2. Retrieve the "Subscription ID" of the account. (https://blogs.msdn.microsoft.com/mschray/2015/05/13/getting-your-azure-guid-subscription-id/)
      * Go to:  http://manage.windowsazure.com/ 
      * Scroll all the way down the left nav
      * Click settings
      * Copy you GUID
  3. You also need "Client ID", "Tenant ID" and a "Key" (password) for a registered service principal. (https://docs.microsoft.com/en-us/azure/azure-resource-manager/resource-group-create-service-principal-portal)
      1. Required permissions
          * Check Azure Active Directory permissions
             1. Select Azure Active Directory.
             2. In Azure Active Directory, select User settings.
             3. Check the App registrations setting. If set to Yes, non-admin users can register AD apps. This setting means any user in the Azure AD tenant can register an app. You can proceed to Check Azure subscription permissions.
             4. If the app registrations setting is set to No, only admin users can register apps. Check whether your account is an admin for the Azure AD tenant. Select Overview and look at your user information. If your account is assigned to the User role, but the app registration setting (from the preceding step) is limited to admin users, ask your administrator to either assign you to an administrator role, or to enable users to register apps.
          * Check Azure subscription permissions
             1. Select your account in the upper right corner, and select My permissions.
             2. From the drop-down list, select the subscription. Select Click here to view complete access details for this subscription.
             3. View your assigned roles, and determine if you have adequate permissions to assign an AD app to a role. If not, ask your subscription administrator to add you to User Access Administrator role. In the following image, the user is assigned to the Owner role, which means that user has adequate permissions.
      2. Create an Azure Active Directory application
         1. Log in to your Azure Account through the Azure portal (https://portal.azure.com/).
         2. Select Azure Active Directory.
         3. Select App registrations.
         4. Select New application registration.
         5. Provide a name and URL for the application. Select Web app / API for the type of application you want to create. You cannot create credentials for a Native application (https://docs.microsoft.com/en-us/azure/active-directory/active-directory-application-proxy-native-client); therefore, that type does not work for an automated application. After setting the values, select Create.
         6. You have created your application.
      3. Get application ID and authentication key
         1. From App registrations in Azure Active Directory, select your application.
         2. Copy the Application ID and store it in your application code. Some sample applications refer to this value as the client ID.
         3. To generate an authentication key, select Settings.
         4. To generate an authentication key, select Keys.
         5. Provide a description of the key, and a duration for the key. When done, select Save.
         6. After saving the key, the value of the key is displayed. Copy this value because you are not able to retrieve the key later. You provide the key value with the application ID to log in as the application. Store the key value where your application can retrieve it.
      4. Get tenant ID
         1. Select Azure Active Directory.
         2. To get the tenant ID, select Properties for your Azure AD tenant.
         3. Copy the Directory ID. This value is your tenant ID.
      5. Assign application to role
         1. Navigate to the level of scope you wish to assign the application to. For example, to assign a role at the subscription scope, select Subscriptions. You could instead select a resource group or resource.
         2. Select the particular subscription (resource group or resource) to assign the application to.
         3. Select Access Control (IAM).
         4. Select Add.
         5. Select the role you wish to assign to the application. The following image shows the Reader role.
         6. By default, Azure Active Directory applications aren't displayed in the available options. To find your application, you must provide the name of it in the search field. Select it.
         7. Select Save to finish assigning the role. You see your application in the list of users assigned to a role for that scope.
  
  
   #### EC - preparatory steps
   1. Upload the plugin
   2. Create the environment with appropriate configuration values.
      * Configuration: ec_plugin_cnfgs
      * Container Cluster Name: test-poc
      * Resource Group Name: ec-test
      * Orchestrator Type: kubernetes
      * Master Zone: westus
      * Admin User Name: ecloudadmin
      * Number Of Master VMs: 1
      * Master DNS Prefix: tmaster
      * Master FQDN: testmaster
      * Master VM Size: Standard_D2
      * Agent Pool Name: agenttest
      * Agent Pool Count: 3
      * Agent Pool VM Size: Standard_D2
      * Agent DNS Prefix: tagent
      * The wait time for cluster creation (In minutes): 8
   2. Create the configuration - you can utilise the information we retrieved from Azure portal in prerequisite steps. You will also need a SSH keypair, the public part of key is placed on VMs that are created and private key is used to communicate with machines via SSH by plugin .This is also a good step to validate that the service principal etc. have been created as needed by "test connection" option available:
      * Configuration: az_conf
      * Description: EC-AzureContainerService
      * Tenant ID: your Tenant ID
      * Subscription ID: your Subscription ID
      * Service Principal Details: 
        * Client ID: your Client ID
        * Private Key: your Private key
      * Public Key: your Public key
      * Private Key: 
        * User Name: Service Principal Details ->  Private Key
        * Private Key: RSA private key