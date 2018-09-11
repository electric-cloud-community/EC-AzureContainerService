package com.electriccloud.client.plugin


import com.electriccloud.client.commander.CommanderClient
import io.qameta.allure.Step
import static com.electriccloud.helpers.config.ConfigHelper.message
import static com.electriccloud.helpers.config.ConfigHelper.dslPath
import static com.electriccloud.helpers.config.ConfigHelper.yamlPath
import static com.electriccloud.helpers.enums.LogLevels.*
import static com.electriccloud.helpers.enums.ServiceTypes.*

class AzureContainerServiceClient extends CommanderClient {


    AzureContainerServiceClient(){
        this.timeout = 500
        this.plugin = 'azure_container_service'
    }

    AzureContainerServiceClient(serverUri, username, password) {
        super(serverUri, username, password)
        this.timeout = 500
        this.plugin = 'azure_container_service'
    }


    @Step("Create configuration: {configurationName}, {clusterEndpoint}")
    def createConfiguration(configName, publicKey,
                            privateKey,
                            credPrivateKey,
                            credClientId,
                            tenantId,
                            subscriptionId,
                            testConnection = true,
                            LogLevel logLevel = LogLevel.DEBUG) {
        message("creating Azure config")
        def json = jsonHelper.azureConfigJson(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, testConnection, logLevel.getValue())
        def response = client.dslFile(dslPath(plugin, 'config'), client.encode(json.toString()))
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Configuration: ${configName} is successfully created.")
        response
    }

    @Step
    def createEnvironment(configName,
                          adminName,
                          acsClusterName,
                          resourceGroup,
                          agentPoolCount = 2,
                          agentPoolDnsPrefix = "flowqeagent",
                          agentPoolName = 'agentflowqe',
                          masterDnsPrefix = "flowqe",
                          masterFqdn = "masterflowqe") {
        message("environment creation")
        def json = jsonHelper.azureEnvJson(configName, adminName, acsClusterName, resourceGroup, agentPoolCount.toString(), agentPoolDnsPrefix, agentPoolName, masterDnsPrefix, masterFqdn)
        def response = client.dslFile dslPath(plugin, 'environment'), client.encode(json.toString())
        client.log.info("Environment for project: ${response.json.project.projectName} is created successfully.")
        response
    }

    @Step
    def provisionCluster(projectName, environmrntName, clusterName){
        def response = provisionEnvironment(projectName, environmrntName, clusterName, 900)
        response
    }

    @Step
    def createService(replicaNum,
                      volumes = [source: null, target: null ],
                      canaryDeploy,
                      ServiceType serviceType = ServiceType.LOAD_BALANCER,
                      namespace = 'default',
                      deploymentTimeout = 120) {
        message("service creation")
        def json = jsonHelper.serviceJson(replicaNum, volumes, canaryDeploy.toString(), serviceType.getValue(), namespace, deploymentTimeout.toString())
        def response = client.dslFile dslPath(plugin, 'service'), client.encode(json.toString())
        client.log.info("Service for project: ${response.json.project.projectName} is created successfully.")
        return response
    }


    @Step
    def createApplication(replicaNum,
                          volumes = [source: null, target: null ],
                          canaryDeploy,
                          ServiceType serviceType = ServiceType.LOAD_BALANCER,
                          namespace = "default",
                          deploymentTimeout = 120) {
        message("application creation")
        def json = jsonHelper.serviceJson(replicaNum, volumes, canaryDeploy.toString(), serviceType.getValue(), namespace, deploymentTimeout.toString())
        def response = client.dslFile dslPath(plugin, 'application'), client.encode(json.toString())
        client.log.info("Service for project: ${response.json.project.projectName} is created successfully.")
        return response
    }

    @Step
    def updateService(replicaNum,
                      volumes = [source: null, target: null ],
                      canaryDeploy,
                      ServiceType serviceType = ServiceType.LOAD_BALANCER,
                      namespace = "default",
                      deploymentTimeout = 120) {
        message("service update")
        def json = jsonHelper.serviceJson(replicaNum, volumes, canaryDeploy.toString(), serviceType.getValue(), namespace, deploymentTimeout.toString())
        def response = client.dslFile dslPath(plugin, 'service'), client.encode(json.toString())
        client.log.info("Service for project: ${response.json.project.projectName} is updated successfully.")
        return response
    }

    @Step
    def updateApplication(replicaNum,
                          volumes = [source: null, target: null ],
                          canaryDeploy,
                          ServiceType serviceType = ServiceType.LOAD_BALANCER,
                          namespace = "default",
                          deploymentTimeout = 120) {
        message("service update")
        def json = jsonHelper.serviceJson(replicaNum, volumes, canaryDeploy.toString(), serviceType.getValue(), namespace, deploymentTimeout.toString())
        def response = client.dslFile dslPath(plugin, 'application'), client.encode(json.toString())
        client.log.info("Service for project: ${response.json.project.projectName} is updated successfully.")
        return response
    }


    @Step("Discover {cluster} on {endpoint}")
    def discoverService(projectName, envProjectName, environmentName, clusterName, namespace = 'default', azClusterName = null, azResourceGroupName = null, agentPoolCount = '1', clientId = null, azureSecretKey = null, privateKey = "", publicKey = "", subscriptionId = null, tenantId = null, importApp = false, appName = null) {
        message("service discovery")
        def privKey = privateKey.readLines().join('\\n')
        def json = jsonHelper.acsDiscoveryJson(projectName, envProjectName, environmentName, clusterName,  namespace, azClusterName,  azResourceGroupName, agentPoolCount.toString(), clientId, azureSecretKey, privKey, publicKey, subscriptionId, tenantId, importApp.toString(), appName)
        def response = client.dslFile dslPath(plugin, 'discover'), client.encode(json.toString())
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Service is discovered successfully.")
        return response
    }

    @Step("Gather Cluster Info")
    def gatherClusterInfo(envProjectName, environmentName, clusterName) {
        message("getting cluster info")
        def json = jsonHelper.gatherClusterInfo(clusterName, envProjectName, environmentName)
        def response = client.dslFile dslPath(plugin, 'gatherClusterInfo'), client.encode(json.toString())
        client.waitForJobToComplete(response.json.jobId, timeout, 2, "Cluster info gathered successfully.")
        response
    }

    @Step
    def cleanUpCluster(config, clusterName, resourceGroup, namespace = 'default') {
        message("azure cluster clean-up")
        def response = client.dslFile dslPath(plugin, 'cleanUp'), client.encode(jsonHelper.azureCleanUpJson(config, clusterName, resourceGroup, namespace).toString())
        client.waitForJobToComplete(response.json.jobId, timeout, 5, "Cluster is successfully cleaned-up.")
        return response
    }









}