import com.electriccloud.client.groovy.ElectricFlow

$[/myProject/scripts/preamble]
$[/myProject/scripts/Discovery]
$[/myProject/scripts/DiscoveryClusterHandler]

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
def privateKey = '''$[ecp_azure_privateKey]'''


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

def pluginConfig
def pluginProjectName = '$[/myProject/projectName]'
def configName
def cluster
try {
    cluster = ef.getCluster(projectName: envProjectName, environmentName: environmentName, clusterName: clusterName)?.cluster


} catch (RuntimeException e) {
    if (e.message =~ /NoSuchCluster|NoSuchEnvironment|NoSuchProject/) {
        if (!tenantId || !subscriptionId || !clientId || !azureSecretKey || !privateKey) {
            efClient.handleProcedureError("Because the specified cluster ${clusterName} does not exist in ${environmentName} environment, you must specify this fields to create cluster: enantID, subscriptionID, clientID, azureSecretKey, privateKey")
        }

        def discoveryClusterHandler = new DiscoveryClusterHandler()
        configName = discoveryClusterHandler.ensureConfiguration(tenantId, subscriptionId, clientId, azureSecretKey, privateKey)
        def project = discoveryClusterHandler.ensureProject(envProjectName)
        def environment = discoveryClusterHandler.ensureEnvironment(envProjectName, environmentName)
        cluster = discoveryClusterHandler.ensureCluster(envProjectName, environmentName, clusterName, configName)
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
pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

AzureClient client = new AzureClient()

String azAccessToken = client.retrieveAccessToken(pluginConfig)
String masterFqdn = client.getMasterFqdn(pluginConfig.subscriptionId, clusterParameters.resourceGroupName, clusterParameters.clusterName, azAccessToken)
String clusterEndpoint = "https://${masterFqdn}"
privateKey = efClient.getCredentials("${configName}_keypair")
String accessToken = client.retrieveOrchestratorAccessToken(pluginConfig,
        clusterParameters.resourceGroupName,
        clusterParameters.clusterName,
        azAccessToken,
        clusterParameters.adminUsername,
        masterFqdn,
        privateKey.password)

try {
    if (!cluster) {
        throw new PluginException("Cluster ${clusterName} does not exist in the environment ${environmentName}")
    }
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
