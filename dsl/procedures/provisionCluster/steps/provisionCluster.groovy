$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def clusterName = '$[clusterName]'
def zone = '$[masterZone]'
def resourceGroupName = '$[resourceGroupName]'
def orchestratorType = '$[orchestratorType]'
def adminUsername = '$[adminUsername]'

def configName = '$[config]'

def masterCount = '$[masterCount]'
def masterFqdn = '$[masterFqdn]'
def masterDnsPrefix = '$[masterDnsPrefix]'
def masterVmsize = '$[masterVmsize]'
def agentPoolName = '$[agentPoolName]'
def agentPoolCount = '$[agentPoolCount]'
def agentPoolVmsize = '$[agentPoolVmsize]'
def agentPoolDnsPrefix = '$[agentPoolDnsPrefix]'
def clusterWaitime =  '$[clusterWaitime]' //TODO Retrieve from cluster properties
def clusterPrepTime = '$[clusterPrepTime]'

// -- Driver script logic to provision cluster -- //
EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

AzureClient az = new AzureClient()

int clusterWaitimeInt = (clusterWaitime?:'5').isInteger() ? clusterWaitime.toInteger() : 0
if (clusterWaitimeInt <= 1) {
    efClient.handleError("'$clusterWaitime' invalid as parameter value for 'clusterWaitime'. Parameter value must be >= 1.")
}

def token = az.retrieveAccessToken(pluginConfig)


az.getOrCreateResourceGroup(resourceGroupName, pluginConfig.subscriptionId ,token, zone)

def deployedAcs = az.getAcs(pluginConfig.subscriptionId, resourceGroupName, clusterName, token)

// Don't switch to simply checking content of object deployedAcs - that is not working in Azure
// So checking the status is a MUST

def acsPayLoad = az.buildContainerServicePayload(
                    location: zone, 
                    orchestratorType: orchestratorType,
                    clientId: "${pluginConfig.credential.userName}",
                    secret: "${pluginConfig.credential.password}",
                    masterCount: masterCount,
                    masterFqdn: masterFqdn,
                    masterDnsPrefix: masterDnsPrefix,
                    masterVmsize: masterVmsize,
                    agentPoolName: agentPoolName,
                    agentPoolCount: agentPoolCount,
                    agentPoolVmsize: agentPoolVmsize,
                    agentPoolDnsPrefix: agentPoolDnsPrefix,
                    adminUsername: adminUsername,
                    publicKey: "${pluginConfig.publicKey}"
                )

if(deployedAcs.status == 200){
      println "The ACS with name ${clusterName} exists already, updating changes"
      def response = az.doHttpPut(az.AZURE_ENDPOINT, 
                               "/subscriptions/${pluginConfig.subscriptionId}/resourcegroups/${resourceGroupName}/providers/Microsoft.ContainerService/containerServices/${clusterName}",
                               token,
                               acsPayLoad,
                               false,                       
                               az.APIV_2017_07_01)

      if(response.status >= 400){
            if(response.status == 409){
              efClient.handleError("It is not allowed to update ${clusterName} in resource group ${resourceGroupName}, the error is: ${response}")
            } else {
              efClient.handleError("A error has occured while updating cluster ${clusterName} in resource group ${resourceGroupName}. Cause:${response}")
            }

      }
      if(response.data.properties.provisioningState == "Updating"){
            az.pollTillCompletion("/subscriptions/${pluginConfig.subscriptionId}/resourcegroups/${resourceGroupName}/providers/Microsoft.ContainerService/containerServices/${clusterName}",
                                  token,
                                  /*timeInSeconds*/ clusterWaitimeInt*60, 
                                  "Waiting for cluster creation to complete...")
      }
      efClient.logger INFO, "Container cluster Update complete"
  } else {
      println "The ACS with name ${clusterName} does not exist, creating new one"
      def response = az.doHttpPut(az.AZURE_ENDPOINT, 
                               "/subscriptions/${pluginConfig.subscriptionId}/resourcegroups/${resourceGroupName}/providers/Microsoft.ContainerService/containerServices/${clusterName}",
                               token,
                               acsPayLoad,
                               /*failOnErrorCode*/ true,
                               az.APIV_2017_07_01)

      if(response.status >= 400){
            if(response.status == 409){
              efClient.handleError("A conflict has occured while creating cluster ${clusterName} in resource group ${resourceGroupName}")
            } else {
              efClient.handleError("A error has occured while creating cluster ${clusterName} in resource group ${resourceGroupName}. Cause:${response}")
            }

      }
      if(response.data.properties.provisioningState == "Creating"){
            az.pollTillCompletion("/subscriptions/${pluginConfig.subscriptionId}/resourcegroups/${resourceGroupName}/providers/Microsoft.ContainerService/containerServices/${clusterName}",
                                  token,
                                  /*timeInSeconds*/ clusterWaitimeInt*60, 
                                  "Waiting for cluster creation to complete...")
      }
      int postProvisionWaitTime = ((clusterPrepTime?:'5').isInteger() ? clusterPrepTime.toInteger() : 5) * 1000
      Thread.sleep(postProvisionWaitTime)
      efClient.logger INFO, "Container cluster creation complete"
  }

// -- Driver script end -- //