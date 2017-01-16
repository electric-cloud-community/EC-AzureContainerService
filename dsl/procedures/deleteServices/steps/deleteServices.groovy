$[/myProject/scripts/helperClasses]

//// Input parameters
def configName = '$[config]'
def resourceGroupName = '$[resourceGroupName]'
def clusterName = '$[clusterName]'
def adminUsername = '$[adminUsername]'


EFClient efClient = new EFClient()
def pluginProjectName = '$[/myProject/projectName]'

def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)


AzureClient client = new AzureClient()
String azToken = client.retrieveAccessToken(pluginConfig)

def masterFqdn = client.getMasterFqdn(pluginConfig.subscriptionId, resourceGroupName, clusterName, azToken)
def clusterEndPoint = "https://${masterFqdn}"

// TODO To validate if all parameters to this method are avialble here
String kubeToken = client.retrieveOrchestratorAccessToken(pluginConfig,
                                                      resourceGroupName,
                                                      clusterName,
                                                      azToken,
                                                      adminUsername,
                                                      masterFqdn)

efClient.logger WARNING, "Deleting all services, and deployments in the cluster '$clusterName'!"
def serviceList = client.doHttpGet(clusterEndPoint,
                                   '/api/v1/namespaces/default/services/',
                                   kubeToken,
                                   /*failOnErrorCode*/false,
                                   null)
for(service in serviceList.data.items){
    def svcName = service.metadata.name
    //skip the 'kubernetes' service
    if (svcName == 'kubernetes') continue
    efClient.logger INFO, "Deleting service $svcName"
    client.doHttpDelete(clusterEndPoint,
                         "/api/v1/namespaces/default/services/${svcName}",
                         kubeToken,
                         /*failOnErrorCode*/false)
}

def deploymentList = client.doHttpGet(clusterEndPoint,
                                   '/apis/extensions/v1beta1/namespaces/default/deployments',
                                   kubeToken,
                                   /*failOnErrorCode*/false,
                                   null)

for(deployment in deploymentList.data.items){
    def deploymentName = deployment.metadata.name
    efClient.logger INFO, "Deleting deployment $deploymentName"
    client.doHttpDelete(clusterEndPoint,
                        "/apis/extensions/v1beta1/namespaces/default/deployments/${deploymentName}",
                        kubeToken,
                        /*failOnErrorCode*/false)

}

def rcList = client.doHttpGet(clusterEndPoint,
                              '/apis/extensions/v1beta1/namespaces/default/replicasets',
                              kubeToken,
                              /*failOnErrorCode*/false,
                              null)

for(rc in rcList.data.items){

    def rcName = rc.metadata.name
    efClient.logger INFO, "Deleting replicaset $rcName"
    client.doHttpDelete(clusterEndPoint,
                        "/apis/extensions/v1beta1/namespaces/default/replicasets/${rcName}",
                        kubeToken,
                        /*failOnErrorCode*/false)
}


def podList = client.doHttpGet(clusterEndPoint,
                               '/api/v1/namespaces/default/pods/',
                               kubeToken,
                               /*failOnErrorCode*/false,
                              null)
for(pod in podList.data.items){
    def podName = pod.metadata.name
    efClient.logger INFO, "Deleting pod $podName"
    client.doHttpDelete(clusterEndPoint,
                        "/api/v1/namespaces/default/pods/${podName}",
                        kubeToken,
                        /*failOnErrorCode*/false)
}