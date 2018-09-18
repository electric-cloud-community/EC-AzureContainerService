package electricflow

import electricflow.backend.container_management_plugins.TopologyMatcher
import electricflow.helpers.json.JsonHelper
import electricflow.ectool.EctoolApi
import electricflow.helpers.objects.Artifactory
import electricflow.rest.APIClient
import electricflow.rest.api.microsoft_azure.AzureContainerServiceApi
import electricflow.rest.api.docker.DockerApi
import electricflow.rest.api.docker.DockerHubApi
import electricflow.rest.api.google_cloud.GoogleContainerEngineApi
import electricflow.rest.api.kubernetes.KubernetesApi
import electricflow.rest.api.openshift.OpenshiftApi
import electricflow.rest.clients.build_application_plugins.ArtifactoryClient
import electricflow.rest.clients.container_management.DockerClient
import electricflow.rest.clients.container_management.AzureContainerServiceClient
import electricflow.rest.clients.container_management.GoogleContainerEngineClient
import electricflow.rest.clients.container_management.KubernetesClient
import electricflow.rest.clients.container_management.OpenshiftClient
import net.bytebuddy.utility.RandomString

import java.text.SimpleDateFormat

trait NamingTestBase {

    // Default Id

    def ecpNamespaceId,
        ecpClusterId,
        ecpClusterName,
        ecpServiceId,
        ecpServiceName,
        ecpContainerId,
        ecpContainerName,
        ecpNamespaceName = "default",
        ecpPodName   = "",
        ecpPodId     = ""

    // Default Names

    def projectName     = 'qe proj',
        adminAccount    = '',
        clusterName     = 'Cluster 1',
        environmentId   = '',
        environmentName = 'qe env',
        environmentProjectName = 'qe env proj',
        serviceName     = 'qe service',
        applicationId = '',
        applicationName = 'qe app',
        processId = '',
        processName = '',
        processStepName = '',
        containerName   = 'qe container',
        pluginProjectName = '',
        pluginName      = '',
        pluginVersion = '',
        pluginLegacyVersion = '',
        configName      = 'qe conf',

        snapshotName,

        serviceId,
        appServiceId,
        clusterId,

        pipelineName,
        releaseName,
        stageName,
        pipelineId,
        releaseId,
        stageId,
        flowRuntimeId,
        taskName,

        clusterEndpoint,
        clusterToken,
        clusterVersion,
        resourceGroup,
        acsClusterName,
        topologyOutcome,

        description = 'some desc',

        //Docker
        endpoint,
        nodeEndpoint,
        caCert,
        cert,
        key,
        certsPath,
        artifactsDir,
        configSwarm,
        configTls,
        configCommunity,

        //Artifactory
        artifactoryUrl,
        artifactoryConfig,
        artifactoryUsername,
        artifactoryPassword

    //API
    EctoolApi ectoolApi
    APIClient chronic3 // todo remove
    GoogleContainerEngineApi gceApi
    DockerApi dockerApi

    KubernetesApi k8sApi
    OpenshiftApi osApi
    DockerHubApi dockerHub
    AzureContainerServiceApi acsApi

    // Clients
    DockerClient dockerClient
    KubernetesClient k8sClient
    GoogleContainerEngineClient gceClient
    AzureContainerServiceClient acsClient
    OpenshiftClient osClient
    TopologyMatcher topologyM
    JsonHelper jsonHelper
    // Artifactory
    ArtifactoryClient artifactoryClient
    Artifactory artifactory

    // Parametrized names

    // Naming Helpers

    String unique(objectName) {
//        new SimpleDateFormat("${objectName}yyyyMMddHHmmssSSS".toString()).format(new Date())
        objectName + (new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()))
    }

    String characters(objectName, num) {
        num = num as Integer
        def _num
        if(num != 0) {
            _num = new RandomString(num).nextString()
            return "${objectName}${_num}".toString()
        } else {
            return ''
        }
    }

    String characters(num) {
        characters('', num)
    }

    // Data Providers
}
