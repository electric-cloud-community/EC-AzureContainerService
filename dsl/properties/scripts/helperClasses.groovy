@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1' )
@Grab('com.microsoft.azure:adal4j:1.1.3')
@Grab('com.jcraft:jsch:0.1.54')

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.DELETE
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT
import static groovyx.net.http.Method.HEAD

import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.FileOutputStream

import java.io.File
import java.nio.file.Files

import static Logger.*

class Logger {
    static final Integer DEBUG = 1
    static final Integer INFO = 2
    static final Integer WARNING = 3
    static final Integer ERROR = 4
    static Integer logLevel = DEBUG
}

public class AzureClient extends BaseClient {

    // Constants
    final public String AUTH_ENDPOINT = "https://login.microsoftonline.com/"
    final public String AZURE_ENDPOINT = "https://management.azure.com"

    // API Version Constants. Probably the only sane way to maintain them for now
    def APIV_2016_09_30 = ["api-version": "2016-09-30"]


    String retrieveAccessToken(def pluginConfig) {

        if (OFFLINE) return "BearerFOO"
        AuthenticationContext authContext = null;
        AuthenticationResult authResult = null;
        ExecutorService service = null;

        String

        try {
            service = Executors.newFixedThreadPool(1);
            String url = AUTH_ENDPOINT + pluginConfig.tenantId + "/oauth2/authorize";
            authContext = new AuthenticationContext(url,
                                                    false,
                                                    service);
                ClientCredential clientCred = new ClientCredential( pluginConfig.clientId, pluginConfig.password);
                Future<AuthenticationResult>  future = authContext.acquireToken(
                                                                AZURE_ENDPOINT+"/",
                                                                clientCred,
                                                                null);
            authResult = future.get();
        } catch (Exception ex) {
            ex.printStackTrace()
        } finally {
            service.shutdown();
        }
        return 'Bearer ' + authResult.getAccessToken()
    }

    String retrieveOrchestratorAccessToken(def pluginConfig, 
                                           String resourceGroupName, 
                                           String clusterName,
                                           String token,
                                           String masterFqdn,
                                           String adminUsername){
        def tempSvcAccFile = "/tmp/def_serviceAcc"
        def tempSecretFile = "/tmp/def_secret"
        def svcAccName = "default"
        def masterFqdn = getMasterFqdn(pluginConfig.subscriptionId, resourceGroupName, clusterName, token)
        def svcAccStatusCode = execRemoteKubectl(masterFqdn, adminUsername, "~/.ssh/id_rsa_ecloud", "kubectl get serviceaccount ${svcAccName} -o json > ${tempSvcAccFile}" )
        copyFileFromRemoteServer(masterFqdn, adminUsername, "~/.ssh/id_rsa_ecloud" , tempSvcAccFile, tempSvcAccFile)
        def svcAccFile = new File(tempSvcAccFile)
        def svcAccJson = new JsonSlurper().parseText(svcAccFile.text)
        def secretName =  svcAccJson.secrets.name[0]

        def secretStatusCode = execRemoteKubectl(masterFqdn, adminUsername, "~/.ssh/id_rsa_ecloud", "kubectl get secret ${secretName} -o json > ${tempSecretFile}" )
        copyFileFromRemoteServer(masterFqdn, adminUsername, "~/.ssh/id_rsa_ecloud" , tempSecretFile , tempSecretFile)
        def secretFile = new File(tempSecretFile)
        def secretJson = new JsonSlurper().parseText(secretFile.text)
        secretJson.data.token
    }

    Object getOrCreateResourceGroup(String rgName, String subscription_id, String accessToken){
      
      def api_version = ["api-version": "2016-09-01"]

      if (OFFLINE) return

      def existingRg = doHttpHead(AZURE_ENDPOINT, 
                       "/subscriptions/${subscription_id}/resourcegroups/${rgName}",
                       accessToken,
                       false,
                       api_version)
      def response
      if(existingRg.status == 204){
            logger INFO, "The Resource group ${rgName} exists already"
        } else if(existingRg.status == 404) {
            logger INFO, "The Resource group ${rgName} does not exist, creating"
            response = doHttpPut(AZURE_ENDPOINT, 
                       "/subscriptions/${subscription_id}/resourcegroups/${rgName}",
                       accessToken,
                       "{'location': 'westus'}",
                       false,                       
                       api_version)
        } 
      response
    }

    Object getAcs(String subscription_id, String rgName, String acsName, String accessToken ){
        if (OFFLINE) return

        def api_version = ["api-version": "2016-09-30"]

        def existingAcs = doHttpGet(AZURE_ENDPOINT,
                          "/subscriptions/${subscription_id}/resourceGroups/${rgName}/providers/Microsoft.ContainerService/containerServices/${acsName}",
                          accessToken,
                          false,
                          api_version)
        existingAcs
    }

    Object buildContainerServicePayload(Map args){
        def containerService = [ 
                location: args.location,
                properties:[ orchestratorProfile: [
                                orchestratorType: args.orchestratorType
                              ],
                              servicePrincipalProfile: [
                                clientId: args.clientId,
                                secret: args.secret
                              ],
                              masterProfile: [
                                  count: args.masterCount,
                                  fqdn: args.masterFqdn,
                                  dnsPrefix: args.masterDnsPrefix
                              ],
                              agentPoolProfiles: [[
                                  name: args.agentPoolName,
                                  count: args.agentPoolCount,
                                  vmSize: args.agentPoolVmsize,
                                  dnsPrefix: args.agentPoolDnsPrefix
                              ]],
                              linuxProfile: [
                                  adminUsername: args.adminUsername,
                                  ssh: [
                                      publicKeys: [[
                                        keyData: args.publicKey
                                      ]]
                                  ]
                              ]
                ]

        ]
        def json = new JsonBuilder(containerService)
        return json.toPrettyString()

    }

    String getMasterFqdn(String subscription_id, String rgName, String acsName, String accessToken){
        if (OFFLINE) return

        def existingAcs = doHttpGet(AZURE_ENDPOINT,
                          "/subscriptions/${subscription_id}/resourceGroups/${rgName}/providers/Microsoft.ContainerService/containerServices/${acsName}",
                          accessToken,
                          false,
                          APIV_2016_09_30)

        return existingAcs.data.properties.masterProfile.fqdn

    }

    def copyFileFromRemoteServer(String hostName, String username, String privateKey, String remoteFilePath, String localDropPath){
        ChannelSftp channel = null
        Session session = null
        InputStream inputStream  = null
        OutputStream outputStream = null
        // Validation before we proceed, may be abstract in a separate method later
        if(hostName == null){ 
            println "#### Something wrong"
        }

        try{
          JSch jsch = new JSch()
          jsch.addIdentity(privateKey)
          session = jsch.getSession(username, hostName)
          session.setConfig("StrictHostKeyChecking", "no")
          session.connect()
          channel = (ChannelSftp)session.openChannel("sftp");
          channel.connect();
          inputStream = channel.get(remoteFilePath);
          outputStream = new FileOutputStream(localDropPath)
          int read = 0;
          byte[] bytes = new byte[1024];

          while ((read = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, read);
          }          
        } catch(Exception exc){
            exc.printStackTrace()
        } finally {
          channel.disconnect()
          session.disconnect() 
          inputStream.close()
          outputStream.close()         
        }

    }

    def execRemoteKubectl(String hostName, String username, String privateKey, String command){
        Channel channel = null
        Session session = null
        int returnCode = 1

        try{
              JSch jsch = new JSch()
              jsch.addIdentity(privateKey)
              session = jsch.getSession(username, hostName)
              session.setConfig("StrictHostKeyChecking", "no")
              session.connect()
              channel = session.openChannel("exec")
              ((ChannelExec)channel).setCommand(command)
              channel.connect()
              returnCode = channel.getExitStatus()
          } catch(Exception ex){
              ex.printStackTrace()

          } finally {
              channel.disconnect()
              session.disconnect() 
          }
          returnCode
    }

    /**
     * Retrieves the cluster from Azure and returns null if not found
     *
     */
    Object getCluster(String projectId, String zone, String clusterName, String accessToken ){
      // TBD
    }

    Object getAgentPool(String projectId, String zone, String clusterName,String nodePoolName, String accessToken ){
      // TBD
    }

    def pollTillCompletion(String operationUrl, String accessToken, int timeInSeconds, String pollingMsg) {
      // TBD
    }

    Object doHttpHead(String requestUrl, String requestUri, String accessToken, boolean failOnErrorCode = true, Map queryArgs){
        doHttpRequest(HEAD,
                      requestUrl,
                      requestUri,
                      ['Authorization' : accessToken],
                      failOnErrorCode,
                      null,
                      queryArgs)
    }

    Object doHttpGet(String requestUrl, String requestUri, String accessToken, boolean failOnErrorCode = true, Map queryArgs) {

        doHttpRequest(GET,
                requestUrl,
                requestUri,
                ['Authorization' : accessToken],
                failOnErrorCode,
                null,
                queryArgs)
    }

    Object doHttpPost(String requestUri, String accessToken, Object requestBody, boolean failOnErrorCode = true) {

        doHttpRequest(POST,
                /*requestUrl*/ CONTAINER_ENGINE_API,
                requestUri,
                ['Authorization' : accessToken],
                failOnErrorCode,
                requestBody)
    }

    Object doHttpPut(String requestUrl, String requestUri, String accessToken, Object requestBody, boolean failOnErrorCode = true, Map queryArgs) {
        doHttpRequest(PUT,
                      requestUrl,
                      requestUri,
                      ['Authorization' : accessToken],
                      failOnErrorCode,
                      requestBody,
                      queryArgs)
    }

    Object doHttpDelete(String requestUrl, String requestUri, String accessToken, boolean failOnErrorCode = true) {

        doHttpRequest(DELETE,
                requestUrl,
                requestUri,
                ['Authorization' : accessToken],
                failOnErrorCode)
    }


}

public class KubernetesClient extends AzureClient {

    /**
     * Retrieves the Deployment instance from GCE Kubernetes cluster.
     * Returns null if no Deployment instance by the given name is found.
     */
    def getDeployment(String clusterEndPoint, String deploymentName, String accessToken) {

        if (OFFLINE) return null

        def response = doHttpGet(clusterEndPoint,
                "/apis/extensions/v1beta1/namespaces/default/deployments/${formatName(deploymentName)}",
                accessToken, /*failOnErrorCode*/ false)
        response.status == 200 ? response.data : null
    }

    /**
     * Retrieves the Service instance from GCE Kubernetes cluster.
     * Returns null if no Service instance by the given name is found.
     */
    def getService(String clusterEndPoint, String serviceName, String accessToken) {

        if (OFFLINE) return null

        def response = doHttpGet(clusterEndPoint,
                "/api/v1/namespaces/default/services/$serviceName",
                accessToken, /*failOnErrorCode*/ false)
        response.status == 200 ? response.data : null
    }

    def createOrUpdateService(String clusterEndPoint, def serviceDetails, String accessToken) {

        String serviceName = formatName(serviceDetails.serviceName)
        def deployedService = getService(clusterEndPoint, serviceName, accessToken)

        def serviceDefinition = buildServicePayload(serviceDetails, deployedService)

        if (OFFLINE) return

        if(deployedService){
            logger INFO, "Updating deployed service $serviceName"
            doHttpRequest(PUT,
                    clusterEndPoint,
                    "/api/v1/namespaces/default/services/$serviceName",
                    ['Authorization' : accessToken],
                    /*failOnErrorCode*/ true,
                    serviceDefinition)

        } else {
            logger INFO, "Creating service $serviceName"
            doHttpRequest(POST,
                    clusterEndPoint,
                    '/api/v1/namespaces/default/services',
                    ['Authorization' : accessToken],
                    /*failOnErrorCode*/ true,
                    serviceDefinition)
        }
    }

    def getDeployedServiceEndpoint(String clusterEndPoint, def serviceDetails, String accessToken) {

        def lbEndpoint
        def elapsedTime = 0;
        def timeInSeconds = 5*60
        String serviceName = formatName(serviceDetails.serviceName)
        while (elapsedTime <= timeInSeconds) {
            def before = System.currentTimeMillis()
            Thread.sleep(10*1000)

            def deployedService = getService(clusterEndPoint, serviceName, accessToken)
            def lbIngress = deployedService?.status?.loadBalancer?.ingress.find {
                it.ip != null || it.hostname != null
            }

            if (lbIngress) {
                lbEndpoint = lbIngress.ip?:lbIngress.hostname
                break
            }
            logger INFO, "Waiting for service status to publish loadbalancer ingress... \nElapsedTime: $elapsedTime seconds"

            def now = System.currentTimeMillis()
            elapsedTime = elapsedTime + (now - before)/1000
        }

        if (!lbEndpoint) {
            logger INFO, "Loadbalancer ingress not published yet. Defaulting to specified loadbalancer IP."
            def value = getServiceParameter(serviceDetails, 'loadBalancerIP')
            lbEndpoint = value
        }
        lbEndpoint
    }

def createOrUpdateSecret(def secretName, def username, def password, def repoBaseUrl,
                         String clusterEndPoint, String accessToken){
        def existingSecret = getSecret(secretName, clusterEndPoint, accessToken)
        def secret = buildSecretPayload(secretName, username, password, repoBaseUrl)
        if (OFFLINE) return null
        if (existingSecret) {
                    logger INFO, "Updating existing Secret $secretName"
                    doHttpRequest(PUT,
                            clusterEndPoint,
                            "/api/v1/namespaces/default/secrets/${secretName}",
                            ['Authorization' : accessToken],
                            /*failOnErrorCode*/ true,
                            secret)

                } else {
                    logger INFO, "Creating deployment $secretName"
                    doHttpRequest(POST,
                            clusterEndPoint,
                            '/api/v1/namespaces/default/secrets',
                            ['Authorization' : accessToken],
                            /*failOnErrorCode*/ true,
                            secret)
                }        
    }

    def getSecret(def secretName, def clusterEndPoint, def accessToken) {

        if (OFFLINE) return null

        def response = doHttpGet(clusterEndPoint,
                "/api/v1/namespaces/default/secrets/${secretName}",
                accessToken, /*failOnErrorCode*/ false)
        response.status == 200 ? response.data : null
    }

    def buildSecretPayload(def secretName, def username, def password, def repoBaseUrl){
        def encodedCreds = (username+":"+password).bytes.encodeBase64().toString()
        def dockerCfgData = ["${repoBaseUrl}": [ username: username, 
                                                password: password, 
                                                email: "none",
                                                auth: encodedCreds]
                            ]
        def dockerCfgJson = new JsonBuilder(dockerCfgData)
        def dockerCfgEnoded = dockerCfgJson.toString().bytes.encodeBase64().toString()
        def secret = [ apiVersion: "v1",
                       kind: "Secret",
                       metadata: [name: secretName],
                       data: [".dockercfg": dockerCfgEnoded],
                       type: "kubernetes.io/dockercfg"]        

        def secretJson = new JsonBuilder(secret)
        return secretJson.toPrettyString()
    }
     
    def constructSecretName(String imageUrl, String username){
        def imageDetails = imageUrl.tokenize('/')
        if (imageDetails.size() < 2) {
            handleError("Please check that the registry url was specified for the image.")
        }
        String repoBaseUrl = imageDetails[0]
        def secretName = repoBaseUrl + "-" + username
        return [repoBaseUrl, secretName.replaceAll(':', '-').replaceAll('/', '-')]
    }

    def createOrUpdateDeployment(String clusterEndPoint, def serviceDetails, String accessToken) {

        // Use the same name as the service name to create a Deployment in Kubernetes
        // that will drive the deployment of the service pods.
        def imagePullSecrets = []
        serviceDetails.container.collect { svcContainer ->
            //Prepend the registry to the imageName
            //if it does not already include it.
            if (svcContainer.registryUri) {
                String image = svcContainer.imageName
                if (!image.startsWith("${svcContainer.registryUri}/")) {
                    svcContainer.imageName = "${svcContainer.registryUri}/$image"
                }
            }

            if(svcContainer.credentialName){

                EFClient efClient = new EFClient()
                def cred = efClient.getCredentials(svcContainer.credentialName)
                def (repoBaseUrl, secretName) = constructSecretName(svcContainer.imageName, cred.userName)
                createOrUpdateSecret(secretName, cred.userName, cred.password, repoBaseUrl,
                        clusterEndPoint, accessToken)
                if (!imagePullSecrets.contains(secretName)) {
                    imagePullSecrets.add(secretName)
                }
            }
        }

        def deploymentName = formatName(serviceDetails.serviceName)
        def existingDeployment = getDeployment(clusterEndPoint, deploymentName, accessToken)
        def deployment = buildDeploymentPayload(serviceDetails, existingDeployment, imagePullSecrets)
        logger DEBUG, "Deployment payload:\n $deployment"


        if (OFFLINE) return null

        if (existingDeployment) {
            logger INFO, "Updating existing deployment $deploymentName"
            doHttpRequest(PUT,
                    clusterEndPoint,
                    "/apis/extensions/v1beta1/namespaces/default/deployments/$deploymentName",
                    ['Authorization' : accessToken],
                    /*failOnErrorCode*/ true,
                    deployment)

        } else {
            logger INFO, "Creating deployment $deploymentName"
            doHttpRequest(POST,
                    clusterEndPoint,
                    '/apis/extensions/v1beta1/namespaces/default/deployments',
                    ['Authorization' : accessToken],
                    /*failOnErrorCode*/ true,
                    deployment)
        }

    }

    Object getJsonFromXML(Object xmlData){
        if(!xmlData){
            return []
        }
        def parsed

        try {
            parsed = new JsonSlurper().parseText(xmlData)
        } catch (Exception e) {
            logger(ERROR, "Cannot parse mount points json: $json")
            System.exit(-1)
        }
        if (!(parsed instanceof List)) {
                parsed = [ parsed ]
            }
        parsed
    }

    def convertVolumes(xmlData){
        def jsonData = getJsonFromXML(xmlData)
        def result = []
        for (item in jsonData){
            def name = formatName(item.name)
            if(item.hostPath){
                result << [name: name, hostPath: [item.hostPath]]
            } else {
                result << [name: name, emptyDir: {}]
            }
        }
        return (new JsonBuilder(result))

    }


    String buildDeploymentPayload(def args, def existingDeployment, def imagePullSecretsList){

        def json = new JsonBuilder()
        //Get the message calculation out of the way
        int maxSurgeValue = args.maxCapacity ? (args.maxCapacity.toInteger() - args.defaultCapacity.toInteger()) : 1
        int maxUnavailableValue =  args.minCapacity ?
                (args.defaultCapacity.toInteger() - args.minCapacity.toInteger()) : 1

        def volumeData = convertVolumes(args.volumes)
        def serviceName = formatName(args.serviceName)
        def result = json {
            kind "Deployment"
            apiVersion "extensions/v1beta1"
            metadata {
                name serviceName
            }
            spec {
                replicas args.defaultCapacity.toInteger()
                strategy {
                    rollingUpdate {
                        maxUnavailable maxUnavailableValue
                        maxSurge maxSurgeValue
                    }
                }
                selector {
                    matchLabels {
                        "ec-svc" serviceName
                    }
                }
                template {
                    metadata {
                        name serviceName
                        labels {
                            "ec-svc" serviceName
                        }
                    }
                    spec{
                        containers(args.container.collect { svcContainer ->
                            def limits = [:]
                            if (svcContainer.memoryLimit) {
                                limits.memory = "${svcContainer.memoryLimit}M"
                            }
                            if (svcContainer.cpuLimit) {
                                Integer cpu = convertCpuToMilliCpu(svcContainer.cpuLimit.toFloat())
                                limits.cpu = "${cpu}m"
                            }

                            def requests = [:]
                            if (svcContainer.memorySize) {
                                requests.memory = "${svcContainer.memorySize}M"
                            }
                            if (svcContainer.cpuCount) {
                                Integer cpu = convertCpuToMilliCpu(svcContainer.cpuCount.toFloat())
                                requests.cpu = "${cpu}m"
                            }

                            def containerResources = [:]
                            if (limits) {
                                containerResources.limits = limits
                            }
                            if (requests) {
                                containerResources.requests = requests
                            }

                            [
                                    name: formatName(svcContainer.containerName),
                                    image: "${svcContainer.imageName}:${svcContainer.imageVersion?:'latest'}",
                                    command: svcContainer.entryPoint?.split(','),
                                    args: svcContainer.command?.split(','),
                                    ports: svcContainer.port?.collect { port ->
                                        [
                                                name: formatName(port.portName),
                                                containerPort: port.containerPort.toInteger(),
                                                protocol: "TCP"
                                        ]
                                    },
                                    volumeMounts: (getJsonFromXML(svcContainer.volumeMounts)).collect { mount ->
                                                        [
                                                            name: formatName(mount.name),
                                                            mountPath: mount.mountPath
                                                        ]

                                        },
                                    env: svcContainer.environmentVariable?.collect { envVar ->
                                        [
                                                name: envVar.environmentVariableName,
                                                value: envVar.value
                                        ]
                                    },
                                    resources: containerResources
                            ]
                        })
                        imagePullSecrets( imagePullSecretsList?.collect { pullSecret ->
                            [name: pullSecret]
                        })
                        volumes(volumeData.content)
                    }
                }

            }
        }

        def payload = existingDeployment
        if (payload) {
            payload = mergeObjs(payload, result)
        } else {
            payload = result
        }
        return ((new JsonBuilder(payload)).toPrettyString())
    }

    def addServiceParameters(def json, Map args) {

        def value = getServiceParameter(args, 'loadBalancerIP')
        if (value != null) {
            json.loadBalancerIP value
        }

        value = getServiceParameter(args, 'sessionAffinity', 'None')
        if (value != null) {
            json.sessionAffinity value
        }

        value = getServiceParameterArray(args, 'loadBalancerSourceRanges')
        if (value != null) {
            json.loadBalancerSourceRanges value
        }
    }

    def getServiceParameter(Map args, String parameterName, def defaultValue = null) {
        def result = args.parameterDetail?.find {
            it.parameterName == parameterName
        }?.parameterValue

        return result != null ? result : defaultValue
    }

    def getServiceParameterArray(Map args, String parameterName, String defaultValue = null) {
        def value = getServiceParameter(args, parameterName, defaultValue)
        value?.toString()?.tokenize(',')
    }

    String buildServicePayload(Map args, def deployedService){

        def serviceName = formatName(args.serviceName)
        def json = new JsonBuilder()
        def result = json {
            kind "Service"
            apiVersion "v1"

            metadata {
                name serviceName
            }
            //GCE plugin injects this service selector
            //to link the service to the pod that this
            //Deploy service encapsulates.
            spec {
                //service type is currently hard-coded to LoadBalancer
                type "LoadBalancer"
                this.addServiceParameters(delegate, args)

                selector {
                    "ec-svc" serviceName
                }
                ports(args.port.collect { svcPort ->
                    [
                            port: svcPort.listenerPort.toInteger(),
                            //name is required for Kubernetes if more than one port is specified so auto-assign
                            name: formatName(svcPort.portName),
                            targetPort: svcPort.subport?:svcPort.listenerPort.toInteger(),
                            // default to TCP which is the default protocol if not set
                            //protocol: svcPort.protocol?: "TCP"
                            protocol: "TCP"
                    ]
                })
            }
        }

        def payload = deployedService
        if (payload) {
            payload = mergeObjs(payload, result)
        } else {
            payload = result
        }
        return (new JsonBuilder(payload)).toPrettyString()
    }

    def convertCpuToMilliCpu(float cpu) {
        return cpu * 1000 as int
    }
}

public class EFClient extends BaseClient {

    def getServerUrl() {
        def commanderServer = System.getenv('COMMANDER_SERVER')
        def secure = Integer.getInteger("COMMANDER_SECURE", 0).intValue()
        def protocol = secure ? "https" : "http"
        def commanderPort = secure ? System.getenv("COMMANDER_HTTPS_PORT") : System.getenv("COMMANDER_PORT")
        def url = "$protocol://$commanderServer:$commanderPort"
        logger DEBUG, "Using ElectricFlow server url: $url"
        url
    }

    Object doHttpGet(String requestUri, boolean failOnErrorCode = true, def query = null) {
        def sessionId = System.getenv('COMMANDER_SESSIONID')
        doHttpRequest(GET, getServerUrl(), requestUri, ['Cookie': "sessionId=$sessionId"],
                failOnErrorCode, /*requestBody*/ null, query)
    }

    Object doHttpPost(String requestUri, Object requestBody, boolean failOnErrorCode = true, def query = null) {
        def sessionId = System.getenv('COMMANDER_SESSIONID')
        doHttpRequest(POST, getServerUrl(), requestUri, ['Cookie': "sessionId=$sessionId"], failOnErrorCode, requestBody, query)
    }

    def getConfigValues(def configPropertySheet, def config, def pluginProjectName) {

        // Get configs property sheet
        def result = doHttpGet("/rest/v1.0/projects/$pluginProjectName/$configPropertySheet", /*failOnErrorCode*/ false)

        def configPropSheetId = result.data?.property?.propertySheetId
        if (!configPropSheetId) {
            throw new RuntimeException("No plugin configurations exist!")
        }

        result = doHttpGet("/rest/v1.0/propertySheets/$configPropSheetId", /*failOnErrorCode*/ false)
        // Get the property sheet id of the config from the result
        def configProp = result.data.propertySheet.property.find{
            it.propertyName == config
        }

        if (!configProp) {
            throw new RuntimeException("Configuration $config does not exist!")
        }

        result = doHttpGet("/rest/v1.0/propertySheets/$configProp.propertySheetId")

        def values = result.data.propertySheet.property.collectEntries{
            [(it.propertyName): it.value]
        }

        logger(INFO, "Config values: " + values)

        def cred = getCredentials(config)
        values << [credential: [userName: cred.userName, password: cred.password]]

        //Set the log level using the plugin configuration setting
        logLevel = (values.logLevel?: INFO).toInteger()

        values
    }

    def getProvisionClusterParameters(String clusterName,
                                      String clusterOrEnvProjectName,
                                      String environmentName) {

        def partialUri = environmentName ?
                "projects/$clusterOrEnvProjectName/environments/$environmentName/clusters/$clusterName" :
                "projects/$clusterOrEnvProjectName/clusters/$clusterName"

        def result = doHttpGet("/rest/v1.0/$partialUri")

        def params = result.data.cluster?.provisionParameters?.parameterDetail

        if(!params) {
            handleError("No provision parameters found for cluster $clusterName!")
        }

        def provisionParams = params.collectEntries {
            [(it.parameterName): it.parameterValue]
        }

        logger DEBUG, "Cluster params from Deploy: $provisionParams"

        return provisionParams
    }

    def getServiceDeploymentDetails(String serviceName,
                                    String serviceProjectName,
                                    String applicationName,
                                    String applicationRevisionId,
                                    String clusterName,
                                    String clusterProjectName,
                                    String environmentName) {

        def partialUri = applicationName ?
                "projects/$serviceProjectName/applications/$applicationName/services/$serviceName" :
                "projects/$serviceProjectName/services/$serviceName"
        def queryArgs = [
                request: 'getServiceDeploymentDetails',
                clusterName: clusterName,
                clusterProjectName: clusterProjectName,
                environmentName: environmentName,
                applicationEntityRevisionId: applicationRevisionId
        ]
        def result = doHttpGet("/rest/v1.0/$partialUri", /*failOnErrorCode*/ true, queryArgs)

        def svcDetails = result.data.service
        logger DEBUG, "Service Details: " + JsonOutput.toJson(svcDetails)

        svcDetails
    }

    def getActualParameters() {
        def jobId = '$[/myJob/jobId]'
        def result = doHttpGet("/rest/v1.0/jobs/$jobId")
        (result.data.job.actualParameter?:[:]).collectEntries {
            [(it.actualParameterName): it.value]
        }
    }

    def getCredentials(def credentialName) {
        def jobStepId = '$[/myJobStep/jobStepId]'
        // Use the new REST mapping for getFullCredential with 'credentialPaths'
        // which works around the restMapping matching issue with the credentialName being a path.
        def result = doHttpGet("/rest/v1.0/jobSteps/$jobStepId/credentialPaths/$credentialName")
        result.data.credential
    }

    def createProperty(String propertyName, String value, Map additionalArgs = [:]) {
        // Creating the property in the context of a job-step by default
        def jobStepId = '$[/myJobStep/jobStepId]'
        def payload = [:]
        payload << additionalArgs
        payload << [
                propertyName: propertyName,
                value: value,
                jobStepId: jobStepId
        ]

        doHttpPost("/rest/v1.0/properties", /* request body */ payload)
    }
}

public class BaseClient {

    //Meant for use during development if there is no internet access
    //in which case Google/GCE API calls will become no-ops.
    final boolean OFFLINE = false

    Object doHttpRequest(Method method, String requestUrl,
                         String requestUri, def requestHeaders,
                         boolean failOnErrorCode = true,
                         Object requestBody = null,
                         def queryArgs = null) {

        logger DEBUG, "requestUrl: $requestUrl"
        logger DEBUG, "method: $method"
        logger DEBUG, "URI: $requestUri"
        if (queryArgs) {
            logger DEBUG, "queryArgs: '$queryArgs'"
        }
        logger DEBUG, "URL: '$requestUrl$requestUri'"
        if (requestBody) logger DEBUG, "Payload: $requestBody"

        def http = new HTTPBuilder(requestUrl)
        http.ignoreSSLIssues()

        http.request(method, JSON) {
            if (requestUri) {
                uri.path = requestUri
            }
            if (queryArgs) {
                uri.query = queryArgs
            }
            headers = requestHeaders
            body = requestBody

            response.success = { resp, json ->
                logger DEBUG, "request was successful $resp.statusLine.statusCode $json"
                [statusLine: resp.statusLine,
                 status: resp.status,
                 data      : json]
            }

            response.failure = { resp, reader ->
                if (failOnErrorCode) {
                    logger ERROR, "Error details: $reader"
                    handleError("Request failed with $resp.statusLine")
                } else {
                    logger INFO, "Response: $reader"
                }

                [statusLine: resp.statusLine,
                 status: resp.status]
            }
        }
    }

    def mergeObjs(def dest, def src) {
        //Converting both object instances to a map structure
        //to ease merging the two data structures
        logger DEBUG, "Source to merge: " + JsonOutput.toJson(src)
        def result = mergeJSON((new JsonSlurper()).parseText((new JsonBuilder(dest)).toString()),
                (new JsonSlurper()).parseText((new JsonBuilder(src)).toString()))
        logger DEBUG, "After merge: " + JsonOutput.toJson(result)
        return result
    }

    def mergeJSON(def dest, def src) {
        src.each { prop, value ->
            logger DEBUG, "Has property $prop? value:" + dest[prop]
            if(dest[prop] != null && dest[prop] instanceof Map) {
                mergeJSON(dest[prop], value)
            } else {
                dest[prop] = value
            }
        }
        return dest
    }

    /**
     * Based on plugin parameter value truthiness
     * True if value == true or value == '1'
     */
    boolean toBoolean(def value) {
        return value != null && (value == true || value == 'true' || value == 1 || value == '1')
    }

    def handleConfigurationError(String msg) {
        createProperty('/myJob/configError', msg)
        handleProcedureError(msg)
    }

    def handleProcedureError (String msg) {
        createProperty('summary', "ERROR: $msg")
        handleError(msg)
    }

    def handleError (String msg) {
        println "ERROR: $msg"
        System.exit(-1)
    }

    def logger(Integer level, def message) {
        if ( level >= logLevel ) {
            println message
        }
    }

    String formatName(String name){
        return name.replaceAll(' ', '-').replaceAll('_', '-').replaceAll("^-+", "").replaceAll("-+\$", "").toLowerCase()
    }
}