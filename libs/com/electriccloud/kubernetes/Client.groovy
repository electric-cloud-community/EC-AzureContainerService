package com.electriccloud.kubernetes

//@Grab('com.microsoft.azure:adal4j:1.1.3')

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.electriccloud.errors.EcException


import com.electriccloud.errors.EcException
import com.electriccloud.errors.ErrorCodes
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET

class Client {

    String endpoint
    String accessToken
    String kubernetesVersion


    static final Integer DEBUG = 1
    static final Integer INFO = 2
    static final Integer WARNING = 3
    static final Integer ERROR = 4

    static Integer logLevel = INFO

    private HTTPBuilder http
    private static final Integer SOCKET_TIMEOUT = 20 * 1000
    private static final Integer CONNECTION_TIMEOUT = 5 * 1000

    final public static String AUTH_ENDPOINT = "https://login.microsoftonline.com/"
    final public static AZURE_ENDPOINT = "https://management.azure.com"
    final public static APIV_2016_09_30 = ["api-version": "2016-09-30"]

    Client(String endpoint, String accessToken) {
        this.endpoint = endpoint
        this.accessToken = accessToken
        this.http = new HTTPBuilder(this.endpoint)
        this.http.ignoreSSLIssues()
        this.kubernetesVersion = getClusterVersion()
    }


    public static String retrieveAccessToken(tenantId, userName, password) {

        AuthenticationContext authContext = null;
        AuthenticationResult authResult = null;
        ExecutorService service = null;
        ClientCredential clientCred = null
        Future<AuthenticationResult> future = null
        String url

        try {
            service = Executors.newFixedThreadPool(1);
            url = AUTH_ENDPOINT + tenantId + "/oauth2/authorize";
            authContext = new AuthenticationContext(url,
                    false,
                    service);
            clientCred = new ClientCredential(userName, password);
            future = authContext.acquireToken(
                    AZURE_ENDPOINT + "/",
                    clientCred,
                    null);
            authResult = future.get();
            return 'Bearer ' + authResult.getAccessToken()
        }
        catch (Exception e){
            throw EcException
                    .code(ErrorCodes.ScriptError)
                    .message("exception message: " + e.getMessage() + "\n retrieveAccessToken: \n url = ${url} \n service = ${service} \n authContext = ${authContext} \n clientCred = ${clientCred} \n future = ${future} \n authResult = ${authResult}")
                    .cause(e)
                    .location(this.class.getCanonicalName())
                    .build()
        }
        finally {
            service.shutdown();
        }
    }

    public static String retrieveOrchestratorAccessToken(def publicKey,
                                                         String resourceGroupName,
                                                         String clusterName,
                                                         String token,
                                                         String adminUsername,
                                                         String masterFqdn,
                                                         String privateKey){

        if (!masterFqdn) {
            handleError("Fully qualified domain name for the master node is missing")
        }

        String passphrase = ""

        // Reference: https://kubernetes.io/docs/tasks/access-application-cluster/access-cluster/#without-kubectl-proxy-post-v13x
        def kubectlSecretExtractionCommand = "kubectl describe secret \$(kubectl get secrets | grep default | cut -f1 -d ' ') | grep -E '^token' | cut -f2 -d':' | tr -d '\\t'"
        String decodedToken = execRemoteKubectlWithOutput(masterFqdn, adminUsername, privateKey, publicKey, passphrase, kubectlSecretExtractionCommand)
        if (!decodedToken) {
            handleError("Failed to run kubectl command on remote host '$masterFqdn' to extract service account bearer token")
        }
        'Bearer ' + new String(decodedToken)
    }

    public static String getMasterFqdn(String subscription_id, String rgName, String acsName, String accessToken){
        def existingAcs = doHttpGet(AZURE_ENDPOINT,
                "/subscriptions/${subscription_id}/resourceGroups/${rgName}/providers/Microsoft.ContainerService/containerServices/${acsName}",
                accessToken,
                false,
                APIV_2016_09_30)

        return existingAcs.data.properties.masterProfile.fqdn

    }

    public static def getClusterParametersMap(def cluster){
        def result = [:]
        cluster.provisionParameters.each{ param ->
            result.put("${param.parameterName}", "${param.parameterValue}")
        }
        return result
    }
    Object doHttpRequest(Method method, String requestUri,
                         Object requestBody = null,
                         def queryArgs = null) {
        def requestHeaders = [
                'Authorization': "Bearer ${this.accessToken}"
        ]
        http.request(method, JSON) { req ->
            if (requestUri) {
                uri.path = requestUri
            }
            if (queryArgs) {
                uri.query = queryArgs
            }
            headers = requestHeaders
            body = requestBody
            req.getParams().setParameter("http.connection.timeout", CONNECTION_TIMEOUT)
            req.getParams().setParameter("http.socket.timeout", SOCKET_TIMEOUT)

            response.success = { resp, json ->
                logger DEBUG, "request was successful $resp.statusLine.statusCode $json"
                json
            }

            response.failure = { resp, reader ->
                throw EcException
                        .code(ErrorCodes.RealtimeClusterLookupFailed)
                        .message("Request for '$requestUri' failed with $resp.statusLine, code: ${resp.status}")
                        .build()
            }
        }
    }


    def getNamespaces() {
        def result = doHttpRequest(GET, "/api/v1/namespaces")
        result?.items
    }

    def getNamespace(String namespace) {
        def result = doHttpRequest(GET, "/api/v1/namespaces/${namespace}")
        result
    }

    def getClusterVersion() {
        def result = doHttpRequest(GET, "/version")
        "${result.major}.${result.minor}"
    }

    def getServices(String namespace) {
        def result = doHttpRequest(GET, "/api/v1/namespaces/${namespace}/services")
        result?.items
    }

    def getAllServices() {
        def result = doHttpRequest(GET, "/api/v1/services")
        result?.items
    }

    def getAllDeployments() {
        def result = doHttpRequest(GET, "/apis/apps/v1beta1/deployments")
        result?.items
    }

    def getAllPods() {
        def result = doHttpRequest(GET, "/api/v1/pods")
        result?.items
    }

    def getService(String namespace, String serviceName){
        def result = doHttpRequest(GET, "/api/v1/namespaces/${namespace}/services/${serviceName}")
        result
    }

    def getDeployments(String namespace, String labelSelector = null) {
        def query = [:]
        if (labelSelector) {
            query.labelSelector = labelSelector
        }
        def result = doHttpRequest(GET, "/apis/${versionSpecificAPIPath("deployments")}/namespaces/${namespace}/deployments", null, query)
        result?.items
    }

    def getServiceVolumes(String namespaceName, String serviceName) {
        def result = doHttpRequest(GET, "/apis/${versionSpecificAPIPath("deployments")}/namespaces/${namespaceName}/deployments/${serviceName}", null, [:])
        result?.spec?.template?.spec?.volumes
    }

    def getPods(String namespace, String labelSelector = null) {
        def query = [:]
        if (labelSelector) {
            query.labelSelector = labelSelector
        }
        def result= doHttpRequest(GET, "/api/v1/namespaces/${namespace}/pods", null, query)
        result?.items
    }


    def getPod(String namespace, String podId) {
        def result = doHttpRequest(GET, "/api/v1/namespaces/${namespace}/pods/${podId}")
        result
    }

    def getPodMetricsHeapster(String namespace, String podId) {
        def result = doHttpRequest(GET, "/api/v1/namespaces/kube-system/services/http:heapster:/proxy/apis/metrics/v1alpha1/namespaces/${namespace}/pods/${podId}")
        result
    }

    def getPodMetricsServerAlpha(String namespace, String podId) {
        def result = doHttpRequest(GET, "/apis/metrics/v1alpha1/namespaces/${namespace}/pods/${podId}")
        result
    }

    def getPodMetricsServerBeta(String namespace, String podId) {
        def result = doHttpRequest(GET, "/apis/metrics.k8s.io/v1beta1/namespaces/${namespace}/pods/${podId}")
        result
    }

    def getContainerLogs(String namespace, String pod, String container) {
        http.request(GET, TEXT) { req ->
            uri.path = "/api/v1/namespaces/${namespace}/pods/${pod}/log"
            uri.query = [container: container, tailLines: 500]
            headers.Authorization = "Bearer ${this.accessToken}"
            headers.Accept = "application/json"

            req.getParams().setParameter("http.connection.timeout", CONNECTION_TIMEOUT)
            req.getParams().setParameter("http.socket.timeout", SOCKET_TIMEOUT)

            response.success = { resp, reader ->
                if (reader) {
                    String logs = reader.text
                    logs
                }
                else {
                    ''
                }
            }
            response.failure = { resp, reader ->
                String result = "Failed to read container logs: ${resp.statusLine}.\nStatus: ${resp.status}"
                if (reader) {
                    result += "\n${reader.text}"
                }
                result
            }
        }
    }

    def getPodLogs(String namespace, String pod) {
        http.request(GET, TEXT) { req ->
            uri.path = "/api/v1/namespaces/${namespace}/pods/${pod}/log"
            uri.query = [tailLines: 500]
            headers.Authorization = "Bearer ${this.accessToken}"
            headers.Accept = "application/json"

            req.getParams().setParameter("http.connection.timeout", CONNECTION_TIMEOUT)
            req.getParams().setParameter("http.socket.timeout", SOCKET_TIMEOUT)

            response.success = { resp, reader ->
                if (reader) {
                    String logs = reader.text
                    logs
                }
                else {
                    ''
                }
            }
            response.failure = { resp, reader ->
                String result = "Failed to read pod logs: ${resp.statusLine}.\nStatus: ${resp.status}"
                if (reader) {
                    result += "\n${reader.text}"
                }
                result
            }
        }
    }

    def static getLogLevelStr(Integer level) {
        switch (level) {
            case DEBUG:
                return '[DEBUG] '
            case INFO:
                return '[INFO] '
            case WARNING:
                return '[WARNING] '
            default://ERROR
                return '[ERROR] '
        }
    }



    boolean isVersionGreaterThan17() {
        try {
            float version = Float.parseFloat(this.kubernetesVersion)
            version >= 1.8
        } catch (NumberFormatException ex) {
            logger WARNING, "Invalid Kubernetes version '$kubernetesVersion'"
            true
        }
    }

    boolean isVersionGreaterThan15() {
        try {
            float version = Float.parseFloat(this.kubernetesVersion)
            version >= 1.6
        } catch (NumberFormatException ex) {
            logger WARNING, "Invalid Kubernetes version '$kubernetesVersion'"
            // default to considering this > 1.5 version
            true
        }
    }


    String versionSpecificAPIPath(String resource) {
        switch (resource) {
            case 'deployments':
                return isVersionGreaterThan15() ? (isVersionGreaterThan17() ? 'apps/v1beta2' : 'apps/v1beta1') : 'extensions/v1beta1'
            default:
                throw EcException
                        .code(ErrorCodes.ScriptError)
                        .message("Unsupported resource '$resource' for determining version specific API path")
                        .build()
        }
    }

    static def logger(Integer level, def message) {
        if (level >= logLevel) {
            println getLogLevelStr(level) + message
        }
    }


}