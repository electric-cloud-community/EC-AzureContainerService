$[/myProject/scripts/helperClasses]

/*

### TBD how many of these are needed

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def projectId = '$[clusterProjectID]'
def clusterName = '$[clusterName]'
def zone = '$[masterZone]'
def configName = '$[config]'

def clusterDescription = '$[clusterDescription]'
def nodePoolName = '$[nodePoolName]'
def nodePoolSize = '$[nodePoolSize]'
def machineType = '$[machineType]'
def imageType = '$[imageType]'
def diskSize = '$[diskSize]'
def additionalZones = '$[additionalZones]'
def network = '$[network]'
def subnetwork = '$[subnetwork]'

def enableAutoscaling = '$[enableAutoscaling]'
def minNodeCount = '$[minNodeCount]'
def maxNodeCount = '$[maxNodeCount]'
def clusterWaitime = '$[clusterWaitime]'

*/
//Input Parameters needed

def resource_grp_name = "ec-container-test"
def location = "westus"
def acs_name = "ec-kube"

// -- Driver script logic to provision cluster -- //
EFClient efClient = new EFClient()

//def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

def pluginConfig = [ tenantId: "7b4b14e5-87f8-4f09-9c83-f91d9b8a49fd",
                clientId: "aea53f55-c6b3-484b-bef9-2a2cf972c6af",
                password: "1/sJBFs9Rj65pv9ya40OxyWx4JGcQEazYEZdTLlFOqw=",
                subscription_id: "20ab6f85-801d-4c3a-a0d4-11da3631d29c"
]

AzureClient az = new AzureClient()
def token = az.retrieveAccessToken(pluginConfig)
println token
az.getOrCreateResourceGroup(resource_grp_name, pluginConfig.subscription_id ,token)

def deployedAcs = az.getAcs(pluginConfig.subscription_id, resource_grp_name, acs_name, token)

// Don't switch to simply checking content of object deplouedAcs - that is not working in Azure
if(deployedAcs.status == 204){
      println "The ACS with name ${acs_name} exists already, updating changes"
      // TBD
  } else {
      println "The ACS with name ${acs_name} does not exist, creating new one"
      def acsPayLoad = az.buildContainerServicePayload(
                          location: "westus", 
                          orchestratorType: "kubernetes",
                          clientId: "${pluginConfig.clientId}",
                          secret: "${pluginConfig.password}",
                          masterCount: 1,
                          masterFqdn: "ecloud",
                          masterDnsPrefix: "master",
                          agentPoolName: "agentPool",
                          agentPoolCount: 3,
                          agentPoolVmsize: "Standard_D2",
                          agentPoolDnsPrefix: "agent",
                          adminUsername: "ecloudadmin",
                          publicKey: "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDXQRP64gIXqH6OrAKm7527/xafW7BDUDyvydMxoUHJjecfKKOxxzb0tb/cLN3ByeK30b4pfq71IxwxSHMMQCKMLWxFKfEj6tIVbaNA4ANFEPUKeFdMDyn3SjsO6ohG4N3SyRc3TMhADkmu/K5H7JoJPR/EDBwE0kV/E1jQS5urnx2Odau1Vs3I4UUP6eBlS2sgUnyo0FVgWm7te0f/JMGxemLPV/qp8GwGFRD+DKAt7JgE49L/hqcghf3JEayEP/32MmV1dvpgZ1i/srHTX0yHw/DmH+ZmSNxMYaYsRVyVtL1jFG1xczml1MC20oG6EJRSSm+IJ4Q7c41rkVrhsyFF vishalb@Vishals-MacBook-Pro.local"
                      )
      response = az.doHttpPut(az.AZURE_ENDPOINT, 
                               "/subscriptions/${pluginConfig.subscription_id}/resourcegroups/${resource_grp_name}/providers/Microsoft.ContainerService/containerServices/${acs_name}",
                               token,
                               acsPayLoad,
                               false,                       
                               az.APIV_2016_09_30)
  }
// -- Driver script end -- //
