$[/myProject/scripts/helperClasses]

EFClient efClient = new EFClient()
def actualParams = efClient.getActualParameters()

if (actualParams.containsKey('tenantId')
        && actualParams.containsKey('clientId')
        && actualParams.containsKey('password')
        && efClient.toBoolean(actualParams.get('testConnection'))) {

    efClient.logger INFO, "Testing connection to the Azure Container Service cluster using the given credentials"
    try {
        AzureClient client = new AzureClient()
        client.retrieveAccessToken([tenantId: actualParams.tenantId, clientId: actualParams.clientId, password: actualParams.password])

        efClient.logger INFO, "Successfully connected to the Azure Container Service using the given account details"
        efClient.createProperty('summary', 'Connection successful')

    } catch (Exception ex) {
        // Specialized types to be added
         ex.printStackTrace()
         String msg = (ex.message?:(ex.cause?.message))?:ex.class.name
     }
}



