import com.electriccloud.client.groovy.ElectricFlow
import groovy.json.JsonBuilder

class DiscoveryClusterHandler {

    static final String PLUGIN_KEY = 'EC-AzureContainerService'

    @Lazy(soft = true)
    ElectricFlow ef = { new ElectricFlow() }()


    def ensureProject(projectName) {
        def project
        try {
            project = ef.getProject(projectName: projectName)?.project
            println "Project ${projectName} has been found"
        } catch (Throwable e) {
            project = ef.createProject(projectName: projectName)?.project
            println "Project ${projectName} has been created"
        }
        project
    }

    def ensureEnvironment(projectName, environmentName) {
        def environment
        try {
            environment = ef.getEnvironment(projectName: projectName, environmentName: environmentName)?.environment
            println "Environment ${environmentName} has been found in the project ${projectName}"
        } catch (Throwable e) {
            environment = ef.createEnvironment(projectName: projectName, environmentName: environmentName).environment
            println "Environment ${environmentName} has been created in the project ${projectName}"
        }
        environment
    }

    def ensureCluster(projectName,
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
                      adminUsername) {
        def cluster
        try {
            cluster = ef.getCluster(
                projectName: projectName,
                environmentName: environmentName,
                clusterName: clusterName
            )?.cluster
            println "Cluster ${clusterName} has been found in the environment ${environmentName}"
        } catch (Throwable e) {
            cluster = ef.createCluster(
                projectName: projectName,
                environmentName: environmentName,
                clusterName: clusterName,
                pluginKey: PLUGIN_KEY,
                provisionParameters: [
                    [provisionParameterName: 'config', value: configName],
                    [provisionParameterName: 'clusterName', value: azClusterName],
                    [provisionParameterName: 'resourceGroupName', value: azResourceGroupName],
                    [provisionParameterName: 'masterZone', value: masterZone],
                    [provisionParameterName: 'adminUsername', value: adminUsername],
                    [provisionParameterName: 'masterCount', value: masterCount],
                    [provisionParameterName: 'masterDnsPrefix', value: masterDnsPrefix],
                    [provisionParameterName: 'masterFqdn', value: masterFqdn],
                    [provisionParameterName: 'agentPoolName', value: agentPoolName],
                    [provisionParameterName: 'agentPoolCount', value: agentPoolCount],
                    [provisionParameterName: 'agentPoolVmsize', value: agentPoolVmsize],
                    [provisionParameterName: 'agentPoolDnsPrefix', value: agentPoolDnsPrefix],
                    [provisionParameterName: 'clusterWaitime', value: clusterWaitTime]
                ],
                provisionProcedure: 'Provision Cluster'
            )?.cluster
            println "Cluster ${clusterName} has been found in the environment ${environmentName}"
        }
        cluster
    }

    def ensureConfiguration(tenantId,
                            subscriptionId,
                            clientId,
                            azureSecretKey,
                            publicKey,
                            privateKey) {
//        def version = retrieveKubernetesVersion(endpoint, token)
        def configName = createConfigurationName(clientId)

        def exists = false
        try {
            def propertySheet = ef.getProperty(propertyName: "/plugins/${PLUGIN_KEY}/project/ec_plugin_cfgs/${configName}")
            exists = true
        } catch (Throwable e) {
//            It's ok, the configuration is not created yet
        }

        if (exists) {
            println "Configuration ${configName} already exists"
            return configName
        }

        def actualParameters = [
            [actualParameterName: 'config', value: configName],
            [actualParameterName: 'desc', value: "This configuration was created by Discovery procedure."],
            [actualParameterName: 'tenantId', value: tenantId],
            [actualParameterName: 'subscriptionId', value: subscriptionId],
            [actualParameterName: 'publicKey', value: publicKey],
            [actualParameterName: 'credential', value: configName],
            [actualParameterName: 'keypair', value: "keypair"],
        ]
        def result = ef.runProcedure(
            projectName: "/plugins/${PLUGIN_KEY}/project",
            procedureName: 'CreateConfiguration',
            actualParameters: actualParameters,
            credentials: [
                [credentialName: configName, userName: clientId, password: azureSecretKey],
                [credentialName: "keypair", userName: azureSecretKey, password: privateKey]
            ]
        )

        println "Launched configuration job: jobId: ${result.jobId}"
        def jobId = result.jobId

        def status = ''
        def timeout = 240
        def elapsed = 0
        def delay = 20
        def info
        while (status != 'completed' && elapsed < timeout) {
            info = ef.getJobInfo(jobId: jobId)?.job
            status = info.status
            sleep(delay * 1000)
            elapsed += delay
        }
        def outcome = info.outcome
        if (outcome == 'error') {
            throw new PluginException("Cannot create configuration: job ${jobId} has failed")
        }
        println "Configuration ${configName} has been created"
        return configName
    }

    def createConfigurationName(String clientId) {
        def random = new Random()
        def randomSuffix = random.nextInt(10 ** 10)
        "ACS-${randomSuffix}".toString()
    }

//    def retrieveKubernetesVersion(endpoint, token) {
//        def client = new KubernetesClient()
//        def version = client.getVersion(endpoint, "Bearer ${token}")
//        int major = version.major as int
//        int minor = version.minor.replaceAll('\\D+', '') as int
//        return "${major}.${minor}"
//    }
}
