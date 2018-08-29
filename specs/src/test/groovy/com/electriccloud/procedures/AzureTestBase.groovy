package com.electriccloud.procedures

import com.electriccloud.NamingTestBase
import com.electriccloud.client.api.AzureContainerServiceApi
import com.electriccloud.client.api.KubernetesApi
import com.electriccloud.client.ectool.EctoolApi
import com.electriccloud.client.plugin.AzureContainerServiceClient
import com.electriccloud.client.plugin.KubernetesClient
import com.electriccloud.listeners.TestListener
import io.qameta.allure.Epic
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Listeners
import java.util.concurrent.TimeUnit
import static io.restassured.RestAssured.given
import static org.awaitility.Awaitility.setDefaultTimeout
import static com.electriccloud.client.HttpClient.pluginsConf

@Epic('EC-AzureContainerService')
@Listeners(TestListener.class)
class AzureTestBase implements NamingTestBase {



    def privateKey
    def publicKey
    def subscriptionId
    def tenantId
    def credClientId
    def credPrivateKey
    def getHost = { hostValue -> new URL(hostValue).host }
    def req = given().relaxedHTTPSValidation().when()
    def volumes = [ source: '[{"name": "html-content","hostPath": "/var/html"}]',
                    target: '[{"name": "html-content","mountPath": "/usr/share/nginx/html"}]' ]


    @BeforeClass
    void setUpData(){
        setDefaultTimeout(40, TimeUnit.SECONDS)
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

        pluginName          = pluginsConf.containerPlugins.plugins.azureContainerService.name
        adminAccount        = pluginsConf.containerPlugins.plugins.azureContainerService.admin
        pluginVersion       = pluginsConf.containerPlugins.plugins.azureContainerService.buildVersion
        pluginLegacyVersion = pluginsConf.containerPlugins.plugins.azureContainerService.legacyVersion
        subscriptionId      = pluginsConf.containerPlugins.plugins.azureContainerService.subscriptionId
        tenantId            = pluginsConf.containerPlugins.plugins.azureContainerService.tenantId
        credClientId        = pluginsConf.containerPlugins.plugins.azureContainerService.credClientId
        credPrivateKey      = pluginsConf.containerPlugins.plugins.azureContainerService.credPrivateKey
        certsPath           = pluginsConf.containerPlugins.plugins.azureContainerService.certsDir
        clusterEndpoint     = pluginsConf.containerPlugins.plugins.azureContainerService.clusterEndpoint
        nodeEndpoint        = pluginsConf.containerPlugins.plugins.azureContainerService.nodeEndpoint
        clusterToken        = pluginsConf.containerPlugins.plugins.azureContainerService.clusterToken

        privateKey          = new File("${certsPath}/privateKey.pub").text
        publicKey           = new File("${certsPath}/publicKey.pub").text

        ectoolApi = new EctoolApi(true)
        acsClient = new AzureContainerServiceClient()
        k8sClient = new KubernetesClient()
        acsApi    = new AzureContainerServiceApi(credClientId, tenantId, credPrivateKey, subscriptionId)
        k8sApi    = new KubernetesApi(clusterEndpoint, clusterToken)

        ectoolApi.ectoolLogin()
    }



}
