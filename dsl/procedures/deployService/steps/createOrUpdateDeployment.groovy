$[/myProject/scripts/preamble]

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

if(clusterParameters.orchestratorType == "kubernetes"){
    String clusterEndPoint = "https://${masterFqdn}"
    String accessToken = azureClient.retrieveOrchestratorAccessToken(pluginConfig,
                                                            clusterParameters.resourceGroupName,
                                                            clusterParameters.clusterName,
                                                            azAccessToken,
                                                            clusterParameters.adminUsername,
                                                            masterFqdn)

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

}else{
    
    def dockerPluginConfig = [
                                endpoint:"tcp://127.0.0.1:2375",
                                cacert: null,
                                cert:null,
                                credential:null
                             ]
    DockerClient dockerClient = new DockerClient(dockerPluginConfig)

    def serviceDetails = efClient.getServiceDeploymentDetails(
                    serviceName,
                    serviceProjectName,
                    applicationName,
                    applicationRevisionId,
                    clusterName,
                    clusterOrEnvProjectName,
                    environmentName,
                    serviceEntityRevisionId)

    def session = azureClient.openSSHTunnel(masterFqdn, clusterParameters.adminUsername, pluginConfig.privateKey)
   
    dockerClient.deployService(
        efClient,    
        dockerPluginConfig.endpoint,  
        serviceName,
        serviceProjectName,
        applicationName,
        applicationRevisionId,
        clusterName,
        clusterOrEnvProjectName,
        environmentName,
        resultsPropertySheet,
        serviceEntityRevisionId)
    
    azureClient.closeSSHTunnel(session)
    String agentFqdn = azureClient.getAgentFqdn(pluginConfig.subscriptionId, clusterParameters.resourceGroupName, clusterParameters.clusterName, azAccessToken)

    serviceDetails.port?.each { port ->
        String portName = port.subport
        String url = "${agentFqdn}:${port.listenerPort}"
        println "Service ${dockerClient.formatName(serviceName)} accessible at ${url}"
    }
}