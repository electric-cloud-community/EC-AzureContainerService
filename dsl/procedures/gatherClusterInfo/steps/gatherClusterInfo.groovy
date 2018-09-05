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
        environmentName
)

def configName = clusterParameters.config
def pluginProjectName = '$[/myProject/projectName]'

def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)
AzureClient azureClient = new AzureClient()

String azAccessToken = azureClient.retrieveAccessToken(pluginConfig)
String masterFqdn = azureClient.getMasterFqdn(pluginConfig.subscriptionId, clusterParameters.resourceGroupName, clusterParameters.clusterName, azAccessToken)

def privateKey = efClient.getCredentials("${configName}_keypair")
String clusterEndPoint = "https://${masterFqdn}"
String accessToken = azureClient.retrieveOrchestratorAccessToken(
        pluginConfig,
        clusterParameters.resourceGroupName,
        clusterParameters.clusterName,
        azAccessToken,
        clusterParameters.adminUsername,
        masterFqdn,
        privateKey.password
)

// store gathered information under within cluster properties
final String ENDPOINT_PROPERTY = 'ec_clusterEndPoint'
final String TOKEN_PROPERTY = 'ec_clusterAccessTokenEncrypted'
final String TOKEN_IV_PROPERTY = 'ec_clusterAccessTokenEncryptionIv'

ClusterInfoCrypter clusterInfoCrypter = new ClusterInfoCrypter()
ClusterInfoCrypter.EncryptionResult encryptionResult = clusterInfoCrypter.encrypt(
        accessToken,
        pluginConfig.credential.password,
        pluginConfig.credential.userName
)
String accessTokenEncrypted = encryptionResult.getEncryptedSensitiveData()
String iv = encryptionResult.getIv()

String clusterEndpointPropertyPath = "/projects/$clusterOrEnvProjectName/environments/$environmentName/clusters/$clusterName/$ENDPOINT_PROPERTY"
String clusterAccessTokenEncryptedPropertyPath = "/projects/$clusterOrEnvProjectName/environments/$environmentName/clusters/$clusterName/$TOKEN_PROPERTY"
String clusterAccessTokenEncryptionIvPropertyPath = "/projects/$clusterOrEnvProjectName/environments/$environmentName/clusters/$clusterName/$TOKEN_IV_PROPERTY"

ScriptExtensions.createOrUpdateProperty(efClient, clusterEndpointPropertyPath, clusterEndPoint)
ScriptExtensions.createOrUpdateProperty(efClient, clusterAccessTokenEncryptedPropertyPath, accessTokenEncrypted)
ScriptExtensions.createOrUpdateProperty(efClient, clusterAccessTokenEncryptionIvPropertyPath, iv)

class ScriptExtensions {
    static void createOrUpdateProperty(EFClient efClient, String propertyPath, String value) {
        if (efClient.getEFProperty(propertyPath, true).data) {
            efClient.setEFProperty(propertyPath, value)
        } else {
            efClient.createProperty(propertyPath, value)
        }
    }
}