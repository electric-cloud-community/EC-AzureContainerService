$[/myProject/scripts/helperClasses]

EFClient efClient = new EFClient()
def actualParams = efClient.getActualParameters()

if (actualParams.containsKey('credential')
        && efClient.toBoolean(actualParams.get('testConnection'))) {

    def cred = efClient.getCredentials('credential')

    efClient.logger INFO, "Testing connection to the Google Cloud Platform using the given service account details."
    if (!cred.password) {
        efClient.handleConfigurationError('Service account JSON key not specified')
    } else {
        try {
            GCEClient client = new GCEClient()
            client.retrieveAccessToken(actualParams << [credential: [userName: cred.userName, password: cred.password]])

            efClient.logger INFO, "Successfully connected to the Google Cloud Platform using the given service account details"
            efClient.createProperty('summary', 'Connection successful')

        } catch (com.fasterxml.jackson.core.JsonParseException ex) {
            ex.printStackTrace()
            efClient.handleConfigurationError("Error while reading Service account JSON key: ${ex.getMessage()}")
        } catch (java.io.IOException ex) {
            ex.printStackTrace()
            String msg = (ex.message?:(ex.cause?.message))?:ex.class.name
            efClient.handleConfigurationError("Error while connecting to the Google Cloud Platform using the given Service account JSON key: $msg")
        }
    }
}



