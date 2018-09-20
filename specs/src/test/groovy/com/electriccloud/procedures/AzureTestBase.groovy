package com.electriccloud.procedures

import com.electriccloud.NamingTestBase
import com.electriccloud.TopologyMatcher
import com.electriccloud.client.api.AzureContainerServiceApi
import com.electriccloud.client.api.KubernetesApi
import com.electriccloud.client.ectool.EctoolApi
import com.electriccloud.client.plugin.AzureContainerServiceClient
import com.electriccloud.client.plugin.KubernetesClient
import com.electriccloud.listeners.TestListener
import io.qameta.allure.Epic
import org.awaitility.Awaitility
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Listeners
import java.util.concurrent.TimeUnit
import static io.restassured.RestAssured.given
import static org.awaitility.Awaitility.await
import static org.awaitility.Awaitility.setDefaultPollInterval
import static org.awaitility.Awaitility.setDefaultTimeout
import static com.electriccloud.helpers.enums.LogLevels.*
import static com.electriccloud.helpers.enums.ServiceTypes.*

@Epic('EC-AzureContainerService')
@Listeners(TestListener.class)
class AzureTestBase implements TopologyMatcher {


    def pluginPath = './src/main/resources'
    def getHost = { hostValue -> new URL(hostValue).host }
    def req = given().relaxedHTTPSValidation().when()
    def volumes = [ source: '[{"name": "html-content","hostPath": "/var/html"}]',
                    target: '[{"name": "html-content","mountPath": "/usr/share/nginx/html"}]' ]


    @BeforeSuite
    void installPlugins(){
        ectoolApi = new EctoolApi(true)
        ectoolApi.ectoolLogin()
        ectoolApi.installPlugin(pluginPath, 'EC-Kubernetes')
        ectoolApi.promotePlugin('EC-Kubernetes-1.1.2.189')
    }


    @BeforeClass
    void setUpData(){
        /** Awaitility settings */
        setDefaultTimeout(70, TimeUnit.SECONDS)
        setDefaultPollInterval(1, TimeUnit.SECONDS)

        configName          = 'acsConfig'
        projectName         = 'acsProj'
        environmentProjectName = 'acsProj'
        environmentName     = "acs-environment"
        clusterName         = "acs-cluster"
        serviceName         = 'nginx-service'
        applicationName     = 'nginx-application'
        containerName       = "nginx-container"
        acsClusterName      = "flowqe-test-cluster"
        resourceGroup       = "flowqe-test-resource-group"

        pluginName          = System.getenv("PLUGIN_NAME")
        adminAccount        = System.getenv("AZURE_ADMIN")
        pluginVersion       = System.getenv("PLUGIN_BUILD_VERSION")
        pluginLegacyVersion = System.getenv("PLUGIN_LEGACY_VERSION")
        subscriptionId      = System.getenv("AZURE_SUBSCRIPTION_ID")
        tenantId            = System.getenv("AZURE_TENANT_ID")
        credClientId        = System.getenv("AZURE_CRED_CLIENT_ID")
        credPrivateKey      = System.getenv("AZURE_CRED_PRIVATE_KEY")
        clusterEndpoint     = System.getenv("AZURE_CLUSTER_ENDPOINT")
        clusterToken        = System.getenv("AZURE_CLUSTER_TOKEN")
        privateKey          = System.getenv("AZURE_PRIVATE_KEY").split("\\\\n").join('\n')
        publicKey           = System.getenv("AZURE_PUBLIC_KEY")

        ectoolApi = new EctoolApi(true)
        acsClient = new AzureContainerServiceClient()
        k8sClient = new KubernetesClient()
        acsApi    = new AzureContainerServiceApi(credClientId, tenantId, credPrivateKey, subscriptionId)
        k8sApi    = new KubernetesApi(clusterEndpoint, clusterToken)

        ectoolApi.ectoolLogin()
    }





    def createAndDeployService(appLevel = false){
        pluginProjectName = "${pluginName}-${pluginVersion}"

        k8sClient.deleteConfiguration(configName)
        acsClient.deleteConfiguration(configName)
        k8sClient.createConfiguration(configName, clusterEndpoint, adminAccount, clusterToken, "1.8", true, '/api/v1/namespaces')
        acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, LogLevel.DEBUG)

        acsClient.createEnvironment(configName, adminAccount, acsClusterName, resourceGroup, 2)
        if (appLevel){
            acsClient.createApplication(2, volumes, false, ServiceType.LOAD_BALANCER)
            acsClient.deployApplication(projectName, applicationName)
        } else {
            acsClient.createService(2, volumes, false, ServiceType.LOAD_BALANCER)
            acsClient.deployService(projectName, serviceName)
        }
        await().until { k8sApi.getPods().json.items.last().status.phase == 'Running' }
    }

    def setTopology(appLevel = false) {
        ecpPodName = k8sApi.getPods().json.items.last().metadata.name
        environmentId = acsClient.client.getEnvironment(projectName, environmentName).json.environment.environmentId
        clusterId = acsClient.client.getEnvCluster(projectName, environmentName, clusterName).json.cluster.clusterId

        ecpNamespaceId   = "$clusterEndpoint::$ecpNamespaceName"
        ecpClusterId     = clusterEndpoint
        ecpClusterName   = clusterEndpoint
        ecpServiceId     = "$ecpNamespaceId::$serviceName"
        ecpServiceName   = "$ecpNamespaceName::$serviceName"
        ecpPodId         = "$clusterEndpoint::$ecpNamespaceName::$ecpPodName"
        ecpContainerId   = "$ecpPodId::$containerName"
        ecpContainerName = "$ecpNamespaceName::$ecpPodName::$containerName"

        if(appLevel) {
            applicationId = acsClient.client.getApplication(projectName, applicationName).json.application.applicationId
            serviceId = acsClient.client.getApplicationService(projectName, applicationName, serviceName).json.service.serviceId
            appServiceId = serviceId
        } else {
            serviceId = acsClient.client.getService(projectName, serviceName).json.service.serviceId
        }
    }




}
