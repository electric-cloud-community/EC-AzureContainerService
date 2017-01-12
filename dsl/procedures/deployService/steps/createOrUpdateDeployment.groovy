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
println "VBIYANI clusterParameters="+clusterParameters

def pluginProjectName = '$[/myProject/projectName]'

def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

AzureClient az = new AzureClient()

String azAccessToken = az.retrieveAccessToken(pluginConfig)
String masterFqdn = az.getMasterFqdn(pluginConfig.subscriptionId, clusterParameters.resourceGroupName, clusterParameters.clusterName, azAccessToken)

String accessToken = az.retrieveOrchestratorAccessToken(pluginConfig,
                                                        clusterParameters.resourceGroupName,
                                                        clusterParameters.clusterName,
                                                        azAccessToken,
                                                        clusterParameters.adminUsername,
                                                        masterFqdn)

println "KubeAccessToken="+accessToken

def serviceDetails = efClient.getServiceDeploymentDetails(
        serviceName,
        serviceProjectName,
        applicationName,
        applicationRevisionId,
        clusterName,
        clusterOrEnvProjectName,
        environmentName)

String clusterEndPoint = "https://${masterFqdn}"
KubernetesClient client = new KubernetesClient()
client.createOrUpdateService(clusterEndPoint, serviceDetails, accessToken)
client.createOrUpdateDeployment(clusterEndPoint, serviceDetails, accessToken)
