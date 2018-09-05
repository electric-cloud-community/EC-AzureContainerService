import com.electriccloud.client.groovy.ElectricFlow

$[/myProject/scripts/preamble]
$[/myProject/scripts/Discovery]
$[/myProject/scripts/DiscoveryClusterHandler]

$[/myProject/scripts/ClusterInfoCrypter]
$[/myProject/scripts/ClusterInfoStorer]

// Input parameters
def envProjectName = '$[envProjectName]'
def environmentName = '$[envName]'
def clusterName = '$[clusterName]'
def namespace = '$[namespace]'
def projectName = '$[projName]'
def applicationScoped = '$[ecp_azure_applicationScoped]'
def applicationName = '$[ecp_azure_applicationName]'
def tenantId = '$[ecp_azure_tenantId]'
def subscriptionId = '$[ecp_azure_subscriptionId]'
def clientId = '$[ecp_azure_clientId]'
def azureSecretKey = '''$[ecp_azure_azureSecretKey]'''
def publicKey = '''$[ecp_azure_publicKey]'''
def privateKey = '''$[ecp_azure_privateKey]'''
def azClusterName = '$[ecp_azure_azClusterName]'
def azResourceGroupName = '$[ecp_azure_azResourceGroupName]'
def masterZone = '$[ecp_azure_masterZone]'
def adminUsername = '$[ecp_azure_adminUsername]'
def masterCount = '$[ecp_azure_masterCount]'
def masterDnsPrefix = '$[ecp_azure_masterDnsPrefix]'
def masterFqdn = '$[ecp_azure_masterFqdn]'
def agentPoolName = '$[ecp_azure_agentPoolName]'
def agentPoolCount = '$[ecp_azure_agentPoolCount]'
def agentPoolVmsize = '$[ecp_azure_agentPoolVmsize]'
def agentPoolDnsPrefix = '$[ecp_azure_agentPoolDnsPrefix]'
def clusterWaitTime = '$[ecp_azure_clusterWaitTime]'

println "Using plugin @PLUGIN_NAME@"
println "Environment Project Name: $envProjectName"
println "Environment Name: $environmentName"
println "Cluster Name: $clusterName"
println "Namespace: $namespace"
println "Project Name: $projectName"
//println "Endpoint: $endpoint"

//if (token) {
//    println "Token: ****"
//}
if (applicationScoped) {
    println "Application Name: $applicationName"
}

EFClient efClient = new EFClient()
ElectricFlow ef = new ElectricFlow()

if (applicationScoped == 'true') {
    if (!applicationName) {
        efClient.handleProcedureError("Application name must be provided")
    }
}
else {
    applicationName = null
}

def pluginConfig = [createByDiscovery: false, credential:[:]]
def pluginProjectName = '$[/myProject/projectName]'
def configName
def cluster
try {
    cluster = ef.getCluster(projectName: envProjectName, environmentName: environmentName, clusterName: clusterName)?.cluster
    def resTemp = cluster.provisionParameters.parameterDetail.find{it -> it.parameterName == "config"} //.parameterValue
    configName = resTemp.parameterValue
    pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

} catch (RuntimeException e) {
    if (e.message =~ /NoSuchCluster|NoSuchEnvironment|NoSuchProject/) {
        if (!tenantId || !subscriptionId || !clientId || !azureSecretKey || !privateKey) {
            efClient.handleProcedureError("Because the specified cluster ${clusterName} does not exist in ${environmentName} environment, you must specify this fields to create cluster: enantID, subscriptionID, clientID, azureSecretKey, privateKey")
        }

        def discoveryClusterHandler = new DiscoveryClusterHandler()
        configName = discoveryClusterHandler.ensureConfiguration(tenantId, subscriptionId, clientId, azureSecretKey, publicKey, privateKey)
        def project = discoveryClusterHandler.ensureProject(envProjectName)
        def environment = discoveryClusterHandler.ensureEnvironment(envProjectName, environmentName)
        cluster = discoveryClusterHandler.ensureCluster(envProjectName,
                                                        environmentName,
                                                        clusterName,
                                                        configName,
                                                        azClusterName,
                                                        azResourceGroupName,
                                                        masterZone,
                                                        masterCount,
                                                        masterDnsPrefix,
                                                        masterFqdn,
                                                        agentPoolName,
                                                        agentPoolCount,
                                                        agentPoolVmsize,
                                                        agentPoolDnsPrefix,
                                                        clusterWaitTime,
                                                        adminUsername)
        pluginConfig.tenantId = tenantId
        pluginConfig.subscriptionId = subscriptionId
        pluginConfig.publicKey = publicKey
        pluginConfig.credential.userName = clientId
        pluginConfig.credential.password = azureSecretKey
        pluginConfig.createByDiscovery = true
        pluginConfig.put("${configName}_keypair".toString(), ["userName": azureSecretKey,"password":privateKey] )
    }
    else {
        throw e
    }
}

def clusterParameters = efClient.getProvisionClusterParameters(
        clusterName,
        envProjectName,
        environmentName)
configName = clusterParameters.config

AzureClient client = new AzureClient()

String azAccessToken = client.retrieveAccessToken(pluginConfig)
masterFqdn = client.getMasterFqdn(pluginConfig.subscriptionId, clusterParameters.resourceGroupName, clusterParameters.clusterName, azAccessToken)
String clusterEndpoint = "https://${masterFqdn}"

privateKey = pluginConfig.createByDiscovery ? privateKey : efClient.getCredentials("${configName}_keypair").password
String accessToken = client.retrieveOrchestratorAccessToken(pluginConfig,
        clusterParameters.resourceGroupName,
        clusterParameters.clusterName,
        azAccessToken,
        clusterParameters.adminUsername,
        masterFqdn,
        privateKey)

try {
//    if (!cluster) {
//        throw new PluginException("Cluster ${clusterName} does not exist in the environment ${environmentName}")
//    }
    if (cluster.pluginKey != 'EC-AzureContainerService') {
        throw new PluginException("ElectricFlow cluster '$clusterName' in environment '$environmentName' is not backed by a ACS-based cluster")
    }

    pluginConfig.clusterEndpoint = clusterEndpoint
    pluginConfig.accessToken = accessToken

    def discovery = new DiscoveryBuilder()
        .projectName(projectName)
        .applicationName(applicationName)
        .environmentProjectName(envProjectName)
        .environmentName(environmentName)
        .clusterName(clusterName)
        .pluginConfig(pluginConfig)
        .namespace(namespace)
        .build()

    def services = discovery.discover()
    if (services.size() == 0) {
        print "No services found on the cluster ${pluginConfig.clusterEndpoint}"
        ef.setProperty(propertyName: '/myCall/summary', value: "No services found on the cluster ${pluginConfig.clusterEndpoint}")
        ef.setProperty(propertyName: '/myJobStep/outcome', value: 'warning')
    }
    discovery.saveToEF(services)
} catch (PluginException e) {
    efClient.handleProcedureError(e.getMessage())
}

ClusterInfoCrypter clusterInfoCrypter = new ClusterInfoCrypter()
ClusterInfoStorer.storeClusterInfo(
        clusterEndpoint,
        accessToken,
        pluginConfig,
        efClient,
        clusterInfoCrypter,
        envProjectName,
        environmentName,
        clusterName
)