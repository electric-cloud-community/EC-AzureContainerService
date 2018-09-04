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



}
