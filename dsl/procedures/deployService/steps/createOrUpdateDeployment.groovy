$[/myProject/scripts/preamble]

$[/myProject/scripts/ClusterInfoCrypter]
$[/myProject/scripts/ClusterInfoStorer]

//// Input parameters
String serviceName = '$[serviceName]'
String serviceProjectName = '$[serviceProjectName]'
String applicationName = '$[applicationName]'
String clusterName = '$[clusterName]'
String clusterOrEnvProjectName = '$[clusterOrEnvProjectName]'
// default cluster project name if not explicitly set
if (!clusterOrEnvProjectName) {
    clusterOrEnvProjectName = serviceProjectName
}
String environmentName = '$[environmentName]'
String applicationRevisionId = '$[applicationRevisionId]'
String serviceEntityRevisionId = '$[serviceEntityRevisionId]'

String resultsPropertySheet = '$[resultsPropertySheet]'
if (!resultsPropertySheet) {
    resultsPropertySheet = '/myParent/parent'
}

//// -- Driverl script logic to provision cluster -- //

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

def serviceDetails = efClient.getServiceDeploymentDetails(
                serviceName,
                serviceProjectName,
                applicationName,
                applicationRevisionId,
                clusterName,
                clusterOrEnvProjectName,
                environmentName,
                serviceEntityRevisionId)
String namespace = azureClient.getServiceParameter(serviceDetails, 'namespace', 'default')

azureClient.deployService(
    efClient,
    accessToken,
    clusterEndPoint,
    namespace,
    serviceName,
    serviceProjectName,
    applicationName,
    applicationRevisionId,
    clusterName,
    clusterOrEnvProjectName,
    environmentName,
    resultsPropertySheet,
    serviceEntityRevisionId)

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