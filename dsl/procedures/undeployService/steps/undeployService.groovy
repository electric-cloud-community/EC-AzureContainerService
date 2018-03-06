$[/myProject/scripts/preamble]

//// Input parameters
String serviceName = '$[serviceName]'
String serviceProjectName = '$[serviceProjectName]'
String applicationName = '$[applicationName]'
String clusterName = '$[clusterName]'
String envProjectName = '$[envProjectName]'
// default env project name if not explicitly set
if (!envProjectName) {
    envProjectName = serviceProjectName
}

String environmentName = '$[environmentName]'
String applicationRevisionId = '$[applicationRevisionId]'
String serviceEntityRevisionId = '$[serviceEntityRevisionId]'

//// -- Driver script logic to undeploy service -- //
EFClient efClient = new EFClient()
def clusterParameters = efClient.getProvisionClusterParameters(
        clusterName,
        envProjectName,
        environmentName)

def configName = clusterParameters.config
def pluginProjectName = '$[/myProject/projectName]'
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

AzureClient client = new AzureClient()

String azAccessToken = client.retrieveAccessToken(pluginConfig)
String masterFqdn = client.getMasterFqdn(pluginConfig.subscriptionId, clusterParameters.resourceGroupName, clusterParameters.clusterName, azAccessToken)
String clusterEndPoint = "https://${masterFqdn}"
def privateKey = efClient.getCredentials("${configName}_keypair")
String accessToken = client.retrieveOrchestratorAccessToken(pluginConfig,
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
                envProjectName,
                environmentName,
                serviceEntityRevisionId)
String namespace = client.getServiceParameter(serviceDetails, 'namespace', 'default')

client.undeployService(
        efClient,
        accessToken,
        clusterEndPoint,
        namespace,
        serviceName,
        serviceProjectName,
        applicationName,
        applicationRevisionId,
        clusterName,
        envProjectName,
        environmentName,
        serviceEntityRevisionId)
