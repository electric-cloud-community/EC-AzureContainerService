@Grab(group='net.sf.json-lib', module='json-lib', version='2.3', classifier = 'jdk15')
@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1' )
@Grab('com.microsoft.azure:adal4j:1.1.3')
@Grab('com.jcraft:jsch:0.1.54')

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;

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

import static groovyx.net.http.Method.DELETE
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT
import static groovyx.net.http.Method.HEAD

import java.io.InputStream
import java.io.FileOutputStream
import java.io.File

public class AzureClient extends KubernetesClient {

    // Constants
    final public String AUTH_ENDPOINT = "https://login.microsoftonline.com/"
    final public String AZURE_ENDPOINT = "https://management.azure.com"

    // API Version Constants. Probably the only sane way to maintain them for now
    def APIV_2016_09_30 = ["api-version": "2016-09-30"]
    def APIV_2017_07_01 = ["api-version": "2017-07-01"]

    String retrieveAccessToken(def pluginConfig) {

        if (OFFLINE) return "BearerFOO"
        AuthenticationContext authContext = null;
        AuthenticationResult authResult = null;
        ExecutorService service = null;

        try {
            service = Executors.newFixedThreadPool(1);
            String url = AUTH_ENDPOINT + pluginConfig.tenantId + "/oauth2/authorize";
            authContext = new AuthenticationContext(url,
                                                    false,
                                                    service);
                ClientCredential clientCred = new ClientCredential( pluginConfig.credential.userName, pluginConfig.credential.password);
                Future<AuthenticationResult>  future = authContext.acquireToken(
                                                                AZURE_ENDPOINT+"/",
                                                                clientCred,
                                                                null);
            authResult = future.get();
            return 'Bearer ' + authResult.getAccessToken()
        } finally {
            service.shutdown();
        }
    }

    String retrieveOrchestratorAccessToken(def pluginConfig,
                                           String resourceGroupName,
                                           String clusterName,
                                           String token,
                                           String adminUsername,
                                           String masterFqdn,
                                           String privateKey){

        if (!masterFqdn) {
            handleError("Fully qualified domain name for the master node is missing")
        }
        String publicKey = pluginConfig.publicKey
        String passphrase = ""

        // Reference: https://kubernetes.io/docs/tasks/access-application-cluster/access-cluster/#without-kubectl-proxy-post-v13x
        def kubectlSecretExtractionCommand = "kubectl describe secret \$(kubectl get secrets | grep default | cut -f1 -d ' ') | grep -E '^token' | cut -f2 -d':' | tr -d '\\t'"
        String decodedToken = execRemoteKubectlWithOutput(masterFqdn, adminUsername, privateKey, publicKey, passphrase, kubectlSecretExtractionCommand)
        if (!decodedToken) {
            handleError("Failed to run kubectl command on remote host '$masterFqdn' to extract service account bearer token")
        }
        'Bearer ' + new String(decodedToken)
    }

    Object getOrCreateResourceGroup(String rgName, String subscription_id, String accessToken, String zone){

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
                       "{'location': '${zone}'}",
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
                              masterProfile: [
                                  count: args.masterCount.toInteger(),
                                  fqdn: args.masterFqdn,
                                  dnsPrefix: args.masterDnsPrefix,
                                  vmSize: args.masterVmsize
                              ],
                              agentPoolProfiles: [[
                                  name: args.agentPoolName,
                                  count: args.agentPoolCount.toInteger(),
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

        if(args.orchestratorType == "kubernetes"){

            def servicePrincipal = [
                                      servicePrincipalProfile: [
                                          clientId: args.clientId,
                                          secret: args.secret
                                      ]
                                    ]
            containerService.properties << servicePrincipal
        }
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

    String getAgentFqdn(String subscription_id, String rgName, String acsName, String accessToken){
        if (OFFLINE) return

        def existingAcs = doHttpGet(AZURE_ENDPOINT,
                          "/subscriptions/${subscription_id}/resourceGroups/${rgName}/providers/Microsoft.ContainerService/containerServices/${acsName}",
                          accessToken,
                          false,
                          APIV_2016_09_30)

        return existingAcs.data.properties.agentPoolProfiles[0].fqdn

    }

    def copyFileFromRemoteServer(String hostName, String username, String privateKey, String publicKey, String passphrase,
                                 String remoteFilePath, File localFile){
        ChannelSftp channel = null
        Session session = null
        InputStream inputStream  = null
        OutputStream outputStream = null

        try{
          JSch jsch = new JSch()
          jsch.addIdentity("ecloudKey",
                           privateKey.getBytes(),
                           publicKey.getBytes(),
                           passphrase.getBytes())
          session = jsch.getSession(username, hostName)
          session.setConfig("StrictHostKeyChecking", "no")
          session.connect()
          channel = (ChannelSftp)session.openChannel("sftp");
          channel.connect();
          inputStream = new BufferedInputStream(channel.get(remoteFilePath))
          outputStream = new BufferedOutputStream(new FileOutputStream(localFile))
          int read = 0;
          byte[] bytes = new byte[1024];
          while ((read = inputStream.read(bytes)) > 0) {
            logger DEBUG, "Writing bytes count: $read"
            outputStream.write(bytes, 0, read);
          }
          outputStream.flush()

        } catch(Exception exc){
            exc.printStackTrace()
            handleError("Failed to retrieve service account information from remote host '$hostName'")
        } finally {
          channel?.disconnect()
          session?.disconnect()
          inputStream?.close()
          outputStream?.close()
        }

    }

    def execRemoteKubectl(String hostName, String username, String privateKey, String publicKey, String passphrase, String command){
        Channel channel = null
        Session session = null
        int returnCode = 1

        try{
              JSch jsch = new JSch()
              jsch.addIdentity("ecloudKey",
                           privateKey.getBytes(),
                           publicKey.getBytes(),
                           passphrase.getBytes())
              session = jsch.getSession(username, hostName)
              session.setConfig("StrictHostKeyChecking", "no")
              session.connect()
              channel = session.openChannel("exec")
              ((ChannelExec)channel).setCommand(command)
              channel.connect()
              returnCode = channel.getExitStatus()
          } catch(Exception ex){
            ex.printStackTrace()
            handleError("Failed to run kubectl command on remote host '$hostName'")
          } finally {
              channel?.disconnect()
              session?.disconnect()
          }
          returnCode
    }

    def execRemoteKubectlWithOutput(String hostName, String username, String privateKey, String publicKey, String passphrase, String command){
        Channel channel = null
        Session session = null
        def response = ''
        try{
            logger DEBUG, "Running remote command: $command"
            logger DEBUG, "\ton host: $hostName"
            JSch jsch = new JSch()
            jsch.addIdentity("ecloudKey",
                    privateKey.getBytes(),
                    publicKey.getBytes(),
                    passphrase.getBytes())
            session = jsch.getSession(username, hostName)
            session.setConfig("StrictHostKeyChecking", "no")
            session.connect()
            channel = session.openChannel("exec")
            ((ChannelExec)channel).setCommand(command)

            // Reference for reading output stream from ChannelExec: http://www.jcraft.com/jsch/examples/Exec.java.html
            channel.setInputStream(null);
            ((ChannelExec)channel).setErrStream(null);
            InputStream inputStream =channel.getInputStream();

            channel.connect()
            byte[] tmp = new byte[1024];
            while(true){
                while(inputStream.available() > 0){
                    int i = inputStream.read(tmp, 0, 1024);
                    if(i < 0)break;
                    response += new String(tmp, 0, i);
                }
                if(channel.isClosed()){
                    if(inputStream.available() > 0) continue;
                    //System.out.println("exit-status: "+channel.getExitStatus());
                    break;
                }
                try{Thread.sleep(1000);}catch(Exception ex){}
            }
            response = response.trim()

        } catch(Exception ex){
            ex.printStackTrace()
            handleError("Failed to run kubectl command on remote host '$hostName'")
        } finally {
            channel?.disconnect()
            session?.disconnect()
        }
        response
    }

    def pollTillCompletion(String operationUrl, String accessToken, int timeInSeconds, String pollingMsg) {
        def elapsedTime = 0;
        def response
        String provisioningState = ""

        while (elapsedTime <= timeInSeconds) {
            def before = System.currentTimeMillis()
            Thread.sleep(10*1000)

            def responseObject = doHttpGet(AZURE_ENDPOINT,
                    operationUrl,
                    accessToken,
                    /*failOnErrorCode*/ true,
                    APIV_2016_09_30)
            response = responseObject.data
            provisioningState = (response.properties.provisioningState)

            if (provisioningState == 'Succeeded'){
                break
            }
            if (provisioningState == 'Failed'){
                break
            }

            logger INFO, "$pollingMsg\nElapsedTime: $elapsedTime seconds"
            if (provisioningState) {
                logger INFO, "Progress: $provisioningState"
            }

            def now = System.currentTimeMillis()
            elapsedTime = elapsedTime + (now - before)/1000
        }

        if (provisioningState != 'Succeeded') {
            handleError("Operation failed to complete in $timeInSeconds seconds. Status= $provisioningState.\n$response")
        }
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
                ['Authorization' : accessToken, 'Content-Type': 'application/json'],
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

     /* Unused method
     def openSSHTunnel(def masterFqdn, def adminUsername, def privateKey){

        def identityFilePath = System.getenv("COMMANDER_WORKSPACE") + "/id_rsa"
        File identityFile = new File(identityFilePath)
        identityFile.text = privateKey

        def localHost = '127.0.0.1'

        def targetHost = masterFqdn
        def targetUser = adminUsername
        def targetSSHPort = 22
        def targetDockerPort = 2375
        def localDockerPort = 2375

        logger INFO, "Opening connection to ${targetUser}@${targetHost}:${targetSSHPort}"

        Properties config = new Properties()
        config.put("StrictHostKeyChecking", "no")
        JSch jsch = new JSch()
        jsch.addIdentity(identityFilePath)

        Session sshSession = jsch.getSession(targetUser, targetHost, targetSSHPort)
        sshSession.setConfig(config)
        sshSession.connect()

        logger INFO, "Connected to ${targetHost}:${targetSSHPort}"
        logger INFO, "Forwarding local port ${localDockerPort} to ${targetHost}:${targetDockerPort}"

        def assignedPort = sshSession.setPortForwardingL(localDockerPort, localHost, targetDockerPort)
        logger INFO, "Established SSH tunnel : ${sshSession.getPortForwardingL()}"

        return sshSession
    }
    */

    def closeSSHTunnel(def sshSession){
        sshSession.disconnect()
    }

    def constructUniqueString() {
        def now = System.currentTimeMillis()
        def randomInt = (new Random()).nextInt()
        now + '-' + randomInt
    }
}
