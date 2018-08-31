def projectName = args.projectName
def environmentName = args.environmentName
def clusterName = args.clusterName
def config = args.configurationParameters
def objectType = args.objectType
def objectIdentifier = args.objectId

def credentials = args.credential
assert credentials.size() == 1
def userName = credentials[0].userName
def password = credentials[0].password

def credential = new Credential(projectName: projectName,
    environmentName: environmentName,
    clusterName: clusterName,
    efContext: this,
    secret: password)
def token = credential.token
def endpoint = credential.endpoint

def cluster = getCluster(projectName: projectName, environmentName: environmentName, clusterName: clusterName)
def clusterId = cluster.clusterId.toString()


import com.electriccloud.errors.EcException
import com.electriccloud.errors.ErrorCodes
import com.electriccloud.kubernetes.*

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
    response = clusterView.getPodDetails(objectIdentifier)
} catch (EcException e) {
    throw e
} catch (SocketTimeoutException | ConnectException e) {
    throw EcException
            .code(ErrorCodes.RealtimeClusterLookupFailed)
            .message("Kubernetes API Endpoint ${endpoint} could not be reached - ${e.message}")
            .cause(e)
            .location(this.class.getCanonicalName())
            .build()
} catch (Throwable e) {
    throw EcException
        .code(ErrorCodes.ScriptError)
        .message("Exception occured while retrieving pod details: ${e.message}")
        .cause(e)
        .location(this.class.getCanonicalName())
        .build()
}
response
