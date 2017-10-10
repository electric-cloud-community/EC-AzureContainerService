$[/myProject/scripts/preamble]

EFClient efClient = new EFClient()
def actualParams = efClient.getActualParameters()

if (actualParams.containsKey('credential')
        && actualParams.containsKey('tenantId')
        && actualParams.containsKey('subscriptionId') 
        && efClient.toBoolean(actualParams.get('testConnection'))) {

    def cred = efClient.getCredentials('credential')
    efClient.logger INFO, "Configuration parameters for testing connection to the Azure Container Service: " +
            "tenantId: " + actualParams.get('tenantId') +
            ", subscriptionId: " + actualParams.get('subscriptionId') +
            ", userName: ${cred.userName}"

    if (!cred.password || !cred.userName) {
        efClient.handleConfigurationError('Service principal Client ID or Key not specified')
    } else {
            String userName = cred.userName
            String password = cred.password

            efClient.logger INFO, "Testing connection to the Azure Container Service cluster using the given credentials..."
            try {
                AzureClient client = new AzureClient()
                client.retrieveAccessToken([tenantId: actualParams.get('tenantId'), credential: [userName: userName, password: password]])

                efClient.logger INFO, "Successfully connected to the Azure Container Service using the given account details"
                efClient.createProperty('summary', 'Connection successful')

            } catch (com.microsoft.aad.adal4j.AuthenticationException ex) {
                ex.printStackTrace()
                efClient.handleConfigurationError("Error while connecting to the Azure Container Service using the given account details: ${ex.getMessage()}")
            } catch (Exception ex) {
                // Specialized types to be added
                 ex.printStackTrace()
                 String msg = (ex.message?:(ex.cause?.message))?:ex.class.name
                efClient.handleConfigurationError("Error while connecting to the Azure Container Service using the given account details: $msg")
            }
    }
}



