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
        } catch (Exception ex) {
            ex.printStackTrace()
        } finally {
            service.shutdown();
        }
        println "AzureToken="+authResult.getAccessToken()
        return 'Bearer ' + authResult.getAccessToken()
    }

    String retrieveOrchestratorAccessToken(def pluginConfig, 
                                           String resourceGroupName, 
                                           String clusterName,
                                           String token,                                      
                                           String adminUsername,
                                           String masterFqdn){
        def tempSvcAccFile = "/tmp/def_serviceAcc"
        def tempSecretFile = "/tmp/def_secret"
        def svcAccName = "default"
        String passphrase = ""
        String publicKey = pluginConfig.publicKey
        String privateKey = pluginConfig.privateKey
        println "publicKey="+publicKey
        println "Before privateKey="+privateKey
        //privateKey = '''ENTER_PRIVATE_KEY_HERE_FOR_TEST'''
        println "After privateKey="+privateKey
        def svcAccStatusCode = execRemoteKubectl(masterFqdn, adminUsername, privateKey, publicKey, passphrase, "kubectl get serviceaccount ${svcAccName} -o json > ${tempSvcAccFile}" )
        
        copyFileFromRemoteServer(masterFqdn, adminUsername, privateKey, publicKey , passphrase, tempSvcAccFile, tempSvcAccFile)
        def svcAccFile = new File(tempSvcAccFile)
        def svcAccJson = new JsonSlurper().parseText(svcAccFile.text)
        def secretName =  svcAccJson.secrets.name[0]

        def secretStatusCode = execRemoteKubectl(masterFqdn, adminUsername, privateKey, publicKey, passphrase, "kubectl get secret ${secretName} -o json > ${tempSecretFile}" )
        copyFileFromRemoteServer(masterFqdn, adminUsername, privateKey, publicKey, passphrase, tempSecretFile , tempSecretFile)
        def secretFile = new File(tempSecretFile)
        def secretJson = new JsonSlurper().parseText(secretFile.text)
        String encodedToken = secretJson.data.token
        'Bearer '+new String(encodedToken.decodeBase64())
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

    def copyFileFromRemoteServer(String hostName, String username, String privateKey, String publicKey, String passphrase, String remoteFilePath, String localDropPath){
        ChannelSftp channel = null
        Session session = null
        InputStream inputStream  = null
        OutputStream outputStream = null
        // Validation before we proceed, may be abstract in a separate method later
        if(hostName == null){ 
            println "#### Something wrong" // TBD
        }

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

          } finally {
              channel.disconnect()
              session.disconnect() 
          }
          returnCode
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


}