$[/myProject/scripts/preamble]

$[/myProject/scripts/ClusterInfoCrypter]

//// Input parameters
String clusterName = '$[clusterName]'
String clusterOrEnvProjectName = '$[clusterOrEnvProjectName]'
String environmentName = '$[environmentName]'

EFClient efClient = new EFClient()

def clusterParameters = efClient.getProvisionClusterParameters(
        clusterName,
        clusterOrEnvProjectName,
        environmentName)

def configName = clusterParameters.config
def pluginProjectName = '$[/myProject/projectName]'

def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)
AzureClient azureClient = new AzureClient()

String azAccessToken = azureClient.retrieveAccessToken(pluginConfig)
String masterFqdn = azureClient.getMasterFqdn(pluginConfig.subscriptionId, clusterParameters.resourceGroupName, clusterParameters.clusterName, azAccessToken)

def privateKey = efClient.getCredentials("${configName}_keypair")
String clusterEndPoint = "https://${masterFqdn}"
String accessToken = azureClient.retrieveOrchestratorAccessToken(pluginConfig,
                                                        clusterParameters.resourceGroupName,
                                                        clusterParameters.clusterName,
                                                        azAccessToken,
                                                        clusterParameters.adminUsername,
                                                        masterFqdn,
                                                        privateKey.password)

// place gathered information under cluster properties
final String ENDPOINT_PROPERTY = 'ec_clusterEndPoint'
final String TOKEN_PROPERTY = 'ec_clusterAccessTokenEncrypted'

ClusterInfoCrypter clusterInfoCrypter = new ClusterInfoCrypter(
        pluginConfig.credential.password,
        pluginConfig.credential.userName
);

String accessTokenEncrypted = clusterInfoCrypter.encrypt(accessToken);

String clusterEndpointPropertyPath = "/projects/$clusterOrEnvProjectName/environments/$environmentName/clusters/$clusterName/$ENDPOINT_PROPERTY";
String clusterAccessTokenEncryptedPropertyPath = "/projects/$clusterOrEnvProjectName/environments/$environmentName/clusters/$clusterName/$TOKEN_PROPERTY";

if (efClient.getEFProperty(clusterEndpointPropertyPath, true).data) {
    efClient.setEFProperty(clusterEndpointPropertyPath, clusterEndPoint)
} else {
    efClient.createProperty(clusterEndpointPropertyPath, clusterEndPoint)
}

if (efClient.getEFProperty(clusterAccessTokenEncryptedPropertyPath, true).data) {
    efClient.setEFProperty(clusterAccessTokenEncryptedPropertyPath, accessTokenEncrypted)
} else {
    efClient.createProperty(clusterAccessTokenEncryptedPropertyPath, accessTokenEncrypted)
}