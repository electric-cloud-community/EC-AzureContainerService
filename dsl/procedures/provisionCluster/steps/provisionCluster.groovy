$[/myProject/scripts/helperClasses]

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
def agentPoolName = '$[agentPoolName]'
def agentPoolCount = '$[agentPoolCount]'
def agentPoolVmsize = '$[agentPoolVmsize]'
def agentPoolDnsPrefix = '$[agentPoolDnsPrefix]'
def clusterWaitime =  '$[clusterWaitime]' //TODO Retrieve from cluster properties

//Input Parameters needed if running outside of Flow environment
/*
def adminUsername = 'ecloudadmin'
def resourceGroupName = "eccontainer-test"
def clusterName = "ec-kube-test"
def orchestratorType = "kubernetes"
def pluginConfig = [ tenantId: "7b4b14e5-87f8-4f09-9c83-f91d9b8a49fd",
                clientId: "aea53f55-c6b3-484b-bef9-2a2cf972c6af",
                password: "1/sJBFs9Rj65pv9ya40OxyWx4JGcQEazYEZdTLlFOqw=",
                subscriptionId: "20ab6f85-801d-4c3a-a0d4-11da3631d29c",
                publicKey: "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDXQRP64gIXqH6OrAKm7527/xafW7BDUDyvydMxoUHJjecfKKOxxzb0tb/cLN3ByeK30b4pfq71IxwxSHMMQCKMLWxFKfEj6tIVbaNA4ANFEPUKeFdMDyn3SjsO6ohG4N3SyRc3TMhADkmu/K5H7JoJPR/EDBwE0kV/E1jQS5urnx2Odau1Vs3I4UUP6eBlS2sgUnyo0FVgWm7te0f/JMGxemLPV/qp8GwGFRD+DKAt7JgE49L/hqcghf3JEayEP/32MmV1dvpgZ1i/srHTX0yHw/DmH+ZmSNxMYaYsRVyVtL1jFG1xczml1MC20oG6EJRSSm+IJ4Q7c41rkVrhsyFF vishalb@Vishals-MacBook-Pro.local"
]
def masterCount = 1
def masterFqdn = 'ecloud'
def masterDnsPrefix = 'k-master'
def agentPoolName = 'agentPool'
def agentPoolCount = 3
def agentPoolVmsize = 'Standard_D2'
def agentPoolDnsPrefix = 'k-agent'
*/
// End of input paraneters given for script


// -- Driver script logic to provision cluster -- //
EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)
println "pluginConfig="+pluginConfig

AzureClient az = new AzureClient()

int clusterWaitimeInt = (clusterWaitime?:'5').isInteger() ? clusterWaitime.toInteger() : 0
if (clusterWaitimeInt <= 1) {
    client.handleError("'$clusterWaitime' invalid as parameter value for 'clusterWaitime'. Parameter value must be >= 1.")
}

def token = az.retrieveAccessToken(pluginConfig)


az.getOrCreateResourceGroup(resourceGroupName, pluginConfig.subscriptionId ,token)

def deployedAcs = az.getAcs(pluginConfig.subscriptionId, resourceGroupName, clusterName, token)

// Don't switch to simply checking content of object deployedAcs - that is not working in Azure
// So checking the status is a MUST
if(deployedAcs.status == 200){
      println "The ACS with name ${clusterName} exists already, updating changes"
      //TODO The updating of existing cluste to be done
  } else {
      println "The ACS with name ${clusterName} does not exist, creating new one"
      def acsPayLoad = az.buildContainerServicePayload(
                          location: zone, 
                          orchestratorType: orchestratorType,
                          clientId: "${pluginConfig.credential.userName}",
                          secret: "${pluginConfig.credential.password}",
                          masterCount: masterCount,
                          masterFqdn: masterFqdn,
                          masterDnsPrefix: masterDnsPrefix,
                          agentPoolName: agentPoolName,
                          agentPoolCount: agentPoolCount,
                          agentPoolVmsize: agentPoolVmsize,
                          agentPoolDnsPrefix: agentPoolDnsPrefix,
                          adminUsername: adminUsername,
                          publicKey: "${pluginConfig.publicKey}"
                      )
      def response = az.doHttpPut(az.AZURE_ENDPOINT, 
                               "/subscriptions/${pluginConfig.subscriptionId}/resourcegroups/${resourceGroupName}/providers/Microsoft.ContainerService/containerServices/${clusterName}",
                               token,
                               acsPayLoad,
                               false,                       
                               az.APIV_2016_09_30)
      println "VBIYANI response="+response

      if(response.status >= 400){
            if(response.status == 409){
              client.handleError("A conflict has occured while creating cluster ${clusterName} in resource group ${resourceGroupName}")
            } else {
              client.handleError("A error has occured while creating cluster ${clusterName} in resource group ${resourceGroupName}. Cause:${response}")
            }

      }
      if(response.data.properties.provisioningState == "Creating"){
            println "Entering PollTillCompletion"
            az.pollTillCompletion("/subscriptions/${pluginConfig.subscriptionId}/resourcegroups/${resourceGroupName}/providers/Microsoft.ContainerService/containerServices/${clusterName}",
                                  token,
                                  /*timeInSeconds*/ clusterWaitimeInt*60, 
                                  "Waiting for cluster creation to complete...")
      }
      efClient.logger INFO, "Container cluster creation complete"
  }

// -- Driver script end -- //