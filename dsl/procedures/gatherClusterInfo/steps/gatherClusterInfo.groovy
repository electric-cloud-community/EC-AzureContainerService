$[/myProject/scripts/preamble]

$[/myProject/scripts/ClusterInfoCrypter]
$[/myProject/scripts/ClusterInfoStorer]

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

ClusterInfoCrypter clusterInfoCrypter = new ClusterInfoCrypter()
ClusterInfoStorer.storeClusterInfo(
        clusterEndPoint,
        accessToken,
        pluginConfig,
        efClient,
        clusterInfoCrypter,
        clusterOrEnvProjectName,
        environmentName,
        clusterName
)