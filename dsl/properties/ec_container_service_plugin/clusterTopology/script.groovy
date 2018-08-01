def projectName = args.projectName
def environmentName = args.environmentName
def clusterName = args.clusterName
def config = args.configurationParameters

def credentials = args.credential
assert credentials.size() != null
def userName = credentials[0].userName
def password = credentials[0].password
//def privateKey = credentials[1].password //wait for fix
def privateKey = """-----BEGIN RSA PRIVATE KEY-----

-----END RSA PRIVATE KEY-----"""

import com.electriccloud.errors.EcException
import com.electriccloud.errors.ErrorCodes
import com.electriccloud.kubernetes.*

def cluster = getCluster(projectName: projectName, environmentName: environmentName, clusterName: clusterName)
def clusterId = cluster.clusterId.toString()
def clusterParameters = Client.getClusterParametersMap(cluster)
//
//fill it
def azAccessToken = ''
//def azAccessToken = Client.retrieveAccessToken(config.tenantId, userName, password)
def masterFqdn = Client.getMasterFqdn(config.subscriptionId, clusterParameters.resourceGroupName, clusterParameters.clusterName, azAccessToken)
def endpoint = "https://${masterFqdn}"
def token = Client.retrieveOrchestratorAccessToken(config.publicKey,
        clusterParameters.resourceGroupName,
        clusterParameters.clusterName,
        azAccessToken,
        clusterParameters.adminUsername,
        masterFqdn,
        privateKey)


//def endpoint = "https://flowqe.eastus.cloudapp.azure.com"
//def token = ""
assert endpoint
assert token

def client = new Client(endpoint, token)
assert clusterId
assert clusterName
def clusterView = new ClusterView(kubeClient: client,
        projectName: projectName,
        environmentName: environmentName,
        clusterName: clusterName,
        clusterId: clusterId)
def response

try {
    response = clusterView.getRealtimeClusterTopology()
} catch (EcException e) {
    throw e
} catch (SocketTimeoutException | ConnectException e) {
    throw EcException
            .code(ErrorCodes.RealtimeClusterLookupFailed)
            .message("ACS API Endpoint ${endpoint} could not be reached - ${e.message}")
            .cause(e)
            .location(this.class.getCanonicalName())
            .build()
} catch (Throwable e) {
    throw EcException
            .code(ErrorCodes.ScriptError)
            .message("Exception occured while retrieving cluster topology: ${e.message}")
            .cause(e)
            .location(this.class.getCanonicalName())
            .build()
}

response