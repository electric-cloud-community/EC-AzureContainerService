$[/myProject/scripts/helperClasses]

EFClient efClient = new EFClient()
def actualParams = efClient.getActualParameters()

if (actualParams.containsKey('credential')
        && actualParams.containsKey('tenantId')
        && actualParams.containsKey('subscriptionId') 
        && efClient.toBoolean(actualParams.get('testConnection'))) {

    def cred = efClient.getCredentials('credential')

    if (!cred.password || !cred.userName) {
        efClient.handleConfigurationError('Service principal Client ID or Key not specified')
    } else {
            String clientId = cred.userName
            String password = cred.password

            efClient.logger INFO, "Testing connection to the Azure Container Service cluster using the given credentials"
            try {
                AzureClient client = new AzureClient()
                client.retrieveAccessToken([tenantId: actualParams.get('tenantId'), clientId: clientId , password: password ])

                efClient.logger INFO, "Successfully connected to the Azure Container Service using the given account details"
                efClient.createProperty('summary', 'Connection successful')

            } catch (Exception ex) {
                // Specialized types to be added
                 ex.printStackTrace()
                 String msg = (ex.message?:(ex.cause?.message))?:ex.class.name
            }
    }
}



