$[/myProject/scripts/helperClasses]

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def projectId = '$[clusterProjectID]'
def clusterName = '$[clusterName]'
def zone = '$[masterZone]'
def configName = '$[config]'

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
// New parameters needed for Azure
def resourceGroupName = '$[resourceGroupName]' // Can be project ID??
def orchestratorType = '$[orchestratorType]'
//Input Parameters needed if running outside of Flow environment

def resourceGroupName = "eccontainer-test"
def clusterName = "ec-kube-test"
def orchestratorType = "kubernetes"
def publicKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDXQRP64gIXqH6OrAKm7527/xafW7BDUDyvydMxoUHJjecfKKOxxzb0tb/cLN3ByeK30b4pfq71IxwxSHMMQCKMLWxFKfEj6tIVbaNA4ANFEPUKeFdMDyn3SjsO6ohG4N3SyRc3TMhADkmu/K5H7JoJPR/EDBwE0kV/E1jQS5urnx2Odau1Vs3I4UUP6eBlS2sgUnyo0FVgWm7te0f/JMGxemLPV/qp8GwGFRD+DKAt7JgE49L/hqcghf3JEayEP/32MmV1dvpgZ1i/srHTX0yHw/DmH+ZmSNxMYaYsRVyVtL1jFG1xczml1MC20oG6EJRSSm+IJ4Q7c41rkVrhsyFF vishalb@Vishals-MacBook-Pro.local"
def pluginConfig = [ tenantId: "7b4b14e5-87f8-4f09-9c83-f91d9b8a49fd",
                clientId: "aea53f55-c6b3-484b-bef9-2a2cf972c6af",
                password: "1/sJBFs9Rj65pv9ya40OxyWx4JGcQEazYEZdTLlFOqw=",
                subscription_id: "20ab6f85-801d-4c3a-a0d4-11da3631d29c",
                publicKey: "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDXQRP64gIXqH6OrAKm7527/xafW7BDUDyvydMxoUHJjecfKKOxxzb0tb/cLN3ByeK30b4pfq71IxwxSHMMQCKMLWxFKfEj6tIVbaNA4ANFEPUKeFdMDyn3SjsO6ohG4N3SyRc3TMhADkmu/K5H7JoJPR/EDBwE0kV/E1jQS5urnx2Odau1Vs3I4UUP6eBlS2sgUnyo0FVgWm7te0f/JMGxemLPV/qp8GwGFRD+DKAt7JgE49L/hqcghf3JEayEP/32MmV1dvpgZ1i/srHTX0yHw/DmH+ZmSNxMYaYsRVyVtL1jFG1xczml1MC20oG6EJRSSm+IJ4Q7c41rkVrhsyFF vishalb@Vishals-MacBook-Pro.local"
]
// -- Driver script logic to provision cluster -- //
EFClient efClient = new EFClient()
//def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

AzureClient az = new AzureClient()
def token = az.retrieveAccessToken(pluginConfig)
println token
az.getOrCreateResourceGroup(resourceGroupName, pluginConfig.subscription_id ,token)

def deployedAcs = az.getAcs(pluginConfig.subscription_id, resourceGroupName, clusterName, token)

// Don't switch to simply checking content of object deployedAcs - that is not working in Azure
// So checking the status is a MUST
if(deployedAcs.status == 200){
      println "The ACS with name ${clusterName} exists already, updating changes"
  } else {
      println "The ACS with name ${clusterName} does not exist, creating new one"
      def acsPayLoad = az.buildContainerServicePayload(
                          location: zone, 
                          orchestratorType: orchestratorType,
                          clientId: "${pluginConfig.clientId}",
                          secret: "${pluginConfig.password}",
                          masterCount: 1,
                          masterFqdn: "ecloud",
                          masterDnsPrefix: "k-master",
                          agentPoolName: "agentPool",
                          agentPoolCount: 3,
                          agentPoolVmsize: "Standard_D2",
                          agentPoolDnsPrefix: "k-agent",
                          adminUsername: "ecloudadmin",
                          publicKey: publicKey
                      )
println acsPayLoad
      response = az.doHttpPut(az.AZURE_ENDPOINT, 
                               "/subscriptions/${pluginConfig.subscription_id}/resourcegroups/${resourceGroupName}/providers/Microsoft.ContainerService/containerServices/${clusterName}",
                               token,
                               acsPayLoad,
                               false,                       
                               az.APIV_2016_09_30)

  }

def tempSvcAccFile = "/tmp/def_serviceAcc"
def tempSecretFile = "/tmp/def_secret"
def svcAccName = "default"
def masterFqdn = az.getMasterFqdn(pluginConfig.subscription_id, resourceGroupName, clusterName, token)
def svcAccStatusCode = az.execRemoteKubectl(masterFqdn, "ecloudadmin", "~/.ssh/id_rsa_ecloud", "kubectl get serviceaccount ${svcAccName} -o json > ${tempSvcAccFile}" )
az.copyFileFromRemoteServer(masterFqdn, "ecloudadmin", "~/.ssh/id_rsa_ecloud" , tempSvcAccFile, tempSvcAccFile)
def svcAccFile = new File(tempSvcAccFile)
def svcAccJson = new JsonSlurper().parseText(svcAccFile.text)
def secretName =  svcAccJson.secrets.name[0]

def secretStatusCode = az.execRemoteKubectl(masterFqdn, "ecloudadmin", "~/.ssh/id_rsa_ecloud", "kubectl get secret ${secretName} -o json > ${tempSecretFile}" )
az.copyFileFromRemoteServer(masterFqdn, "ecloudadmin", "~/.ssh/id_rsa_ecloud" , tempSecretFile , tempSecretFile)
def secretFile = new File(tempSecretFile)
def secretJson = new JsonSlurper().parseText(secretFile.text)
println secretJson.data.token


// -- Driver script end -- //