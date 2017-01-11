$[/myProject/scripts/helperClasses]

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

//// -- Driverl script logic to provision cluster -- //

EFClient efClient = new EFClient()

def clusterParameters = efClient.getProvisionClusterParameters(
        clusterName,
        clusterOrEnvProjectName,
        environmentName)

def configName = clusterParameters.config
def clusterEndpoint = clusterParameters.clusterURL

def pluginProjectName = '$[/myProject/projectName]'

def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)



AzureClient az = new AzureClient()
String azAccessToken = az.retrieveAccessToken(pluginConfig)

String accessToken = az.retrieveOrchestratorAccessToken()

def serviceDetails = efClient.getServiceDeploymentDetails(
        serviceName,
        serviceProjectName,
        applicationName,
        applicationRevisionId,
        clusterName,
        clusterOrEnvProjectName,
        environmentName)

// This should work as it is without any modification
client.createOrUpdateService(clusterEndPoint, serviceDetails, accessToken)

client.createOrUpdateDeployment(clusterEndPoint, serviceDetails, accessToken)
