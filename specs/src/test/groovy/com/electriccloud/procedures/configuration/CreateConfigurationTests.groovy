package com.electriccloud.procedures.configuration

import com.electriccloud.procedures.AzureTestBase
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Issue
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static com.electriccloud.helpers.enums.LogLevels.*

@Feature('Configuration')
class CreateConfigurationTests extends AzureTestBase {

    @BeforeClass
    void setUpTests(){
        acsClient.deleteConfiguration(configName)
    }

    @AfterMethod
    void tearDownTest() {
        acsClient.deleteConfiguration(configName)
        acsClient.client.deleteProject(projectName)
    }



    @Test
    @TmsLink("")
    @Story("Create Configuration with test connection")
    @Description("Create Configuration with azure-cloud test connection ")
    void createConfigurationWithTestConnection() {
        def job = acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, LogLevel.DEBUG)
        String logs = acsClient.client.getJobLogs(job.json.jobId)
        def jobStatus = acsClient.client.getJobStatus(job.json.jobId).json
        def jobSteps = acsClient.client.getJobSteps(job.json.jobId).json.object
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert logs.contains("Successfully connected to the Azure Container Service using the given account details")
        assert jobSteps[0].jobStep.combinedStatus.status == "completed_success"
        assert jobSteps[1].jobStep.combinedStatus.status == "completed_success"
    }


    @Test
    @TmsLink("")
    @Story("Create Configuration without test connection")
    @Description("Create configuration without azure-cloud test connection")
    void createConfigurationWithoutTestConnection() {
        def job = acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, false, LogLevel.DEBUG)
        def jobStatus = acsClient.client.getJobStatus(job.json.jobId).json
        def jobSteps = acsClient.client.getJobSteps(job.json.jobId).json.object
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert jobSteps[0].jobStep.combinedStatus.status == "skipped"
        assert jobSteps[1].jobStep.combinedStatus.status == "skipped"
    }


    @Test(dataProvider = "logLevels")
    @Issue("ECAZCS-83")
    @TmsLink("")
    @Story("Log Level Configuration")
    @Description("Create Configuration for different log Levels")
    void createConfigurationForDifferentLogLevels(logLevel, message, desiredLog, missingLog){
        def job = acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, logLevel)
        acsClient.createEnvironment(configName, adminAccount, acsClusterName, resourceGroup, 2)
        def resp = acsClient.provisionEnvironment(projectName, environmentName, clusterName)
        def jobStatus = acsClient.client.getJobStatus(job.json.jobId).json
        def jobSteps = acsClient.client.getJobSteps(job.json.jobId).json.object
        String logs = acsClient.client.getJobLogs(resp.json.jobId)
        assert job.resp.statusLine.toString().contains("200 OK")
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert jobSteps[2].jobStep.command.contains(message)
        assert logs.contains(desiredLog)
        assert !logs.contains(missingLog)
    }


    @Test(dataProvider = "configData")
    @Story("Create Configuration with fields validation")
    @Description("Create Configuration with fields validation")
    void createConfigurationWithFieldsValidation(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, logLevel, message, outcome, status) {
        def job = acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, logLevel)
        String logs = acsClient.client.getJobLogs(job.json.jobId)
        def jobStatus = acsClient.client.getJobStatus(job.json.jobId).json
        def jobSteps = acsClient.client.getJobSteps(job.json.jobId).json.object
        assert jobStatus.outcome == outcome
        assert jobStatus.status == status
        assert logs.contains(message)
        assert jobSteps[0].jobStep.combinedStatus.status == "completed_success"
        assert jobSteps[1].jobStep.combinedStatus.status == "completed_success"
    }


    @Test
    @TmsLink("")
    @Story("Invalid configuration")
    @Description("Unable to create configuration that already exist ")
    void unableToCreateExistingConfiguration(){
        try {
            acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, LogLevel.DEBUG)
            acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, LogLevel.DEBUG)
        } catch (e){
            def jobId = e.cause.message
            def jobStatus = acsClient.client.getJobStatus(jobId).json
            String logs = acsClient.client.getJobLogs(jobId)
            assert logs.contains("A configuration named '${configName}' already exists.")
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }




    @Test(dataProvider = "invalidData")
    @Story("Invalid configuration")
    @TmsLink("")
    @Description("Unable to configure with invalid data")
    void unableToConfigureWithInvalidData(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, logLevel, errorMessage, outcome, status){
        try {
            acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, logLevel)
        } catch (e){
            def jobStatus = acsClient.client.getJobStatus(e.cause.message).json
            String logs = acsClient.client.getJobLogs(e.cause.message)

            println logs
            assert logs.contains(errorMessage), 'The Procedure passed with invalid credentials!!!'
            assert jobStatus.outcome == outcome
            assert jobStatus.status == status
        }
    }





    @DataProvider(name = "logLevels")
    def getLogLevels(){
        return [
                [LogLevel.DEBUG, "logger DEBUG", "[DEBUG]", "[ERROR]"],
                [LogLevel.INFO, "logger INFO", "[INFO]", "[DEBUG]"],
                [LogLevel.WARNING, "logger WARNING", "[INFO]", "[ERROR]"],
                [LogLevel.ERROR, "logger ERROR", "[INFO]", "[WARNING]"],
        ] as Object[][]
    }

    @DataProvider(name = "configData")
    def getConfigData(){
        return [
                [configName, publicKey.substring(8), privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, LogLevel.DEBUG, 'Successfully connected to the Azure Container Service using the given account details', 'success', 'completed'],
                [configName, "", privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, LogLevel.DEBUG, 'Successfully connected to the Azure Container Service using the given account details', 'success', 'completed'],
                [configName, publicKey, "", credPrivateKey, credClientId, tenantId, subscriptionId, LogLevel.DEBUG, "Successfully connected to the Azure Container Service using the given account details", 'success', 'completed'],
                [configName, publicKey, privateKey.substring(40), credPrivateKey, credClientId, tenantId, subscriptionId, LogLevel.DEBUG, 'Successfully connected to the Azure Container Service using the given account details', 'success', 'completed'],
        ] as Object[][]
    }


    @DataProvider(name = "invalidData")
    def getInvalidData(){
        return [
                [" ", publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, LogLevel.DEBUG, 'Error creating configuration credential: \'credentialName\' is required and must be between 1 and 255 characters (InvalidCredentialName)', 'error', 'completed'],
                [configName, publicKey, privateKey, "", credClientId, tenantId, subscriptionId, LogLevel.DEBUG, "ERROR: Service principal Client ID or Key not specified\n", "error", "completed"],
                [configName, publicKey, privateKey, credPrivateKey.substring(5), credClientId, tenantId, subscriptionId, LogLevel.DEBUG, "Invalid client secret is provided.", 'error', 'completed'],
                [configName, publicKey, privateKey, credPrivateKey, "", tenantId, subscriptionId, LogLevel.DEBUG, "ERROR: Service principal Client ID or Key not specified", 'error', 'completed'],
                [configName, publicKey, privateKey, credPrivateKey, credClientId.substring(5), tenantId, subscriptionId, LogLevel.DEBUG, "Application with identifier '${credClientId.substring(5)}' was not found", 'error', 'completed'],
                [configName, publicKey, privateKey, credPrivateKey, credClientId, "", subscriptionId, LogLevel.DEBUG, "ERROR: Error while connecting to the Azure Container Service using the given account details", 'error', 'completed'],
                [configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId.substring(5), subscriptionId, LogLevel.DEBUG, "ERROR: Error while connecting to the Azure Container Service using the given account details", 'error', 'completed'],
                [configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, "", LogLevel.DEBUG, "Invalid client secret is provided.", 'error', 'completed'],
                [configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId.substring(5), LogLevel.DEBUG, "Invalid client secret is provided.", 'error', 'completed']
        ] as Object[][]
    }


    @Test
    void printsomething(){
        println 'Retrieved and copied grape dependencies from /opt/EC/artifact-cache/com.electriccloud/EC-Kubernetes-Grapes/1.0.2 to /opt/EC/grape/grapes\nRetrieved and copied grape dependencies from /opt/EC/artifact-cache/com.electriccloud/EC-AzureContainerService-Grapes/1.0.0 to /opt/EC/grape/grapes\nGrabbed Resource: local\n[INFO] Plugin configuration values: [additionalArtifactVersion:com.electriccloud:EC-AzureContainerService-Grapes:1.0.0, credential:acsConfig, desc:EC-AzureContainerService Config, keypair:acsConfig_keypair, logLevel:1, publicKey:ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDlNyjTfvz5vrjFT/p+3qzlCokc5qLn989pAOpLwKe5xfHt2hHLjQe/sKt/8eq21Efg5LA2agRGoOwJr//z40WHHIMYmQOvtZb6kO7Uy4r81aX5dP/dZcMbe6U/EAT0VVP7VlET4Z+3mQFvj0/DbTa2dM7SQ/d46QhyOS57hHIdE5a9DnDWF99WhI2CGJ+JX6GFUT2XnwsRqDrAw+bT7NsMeBywza03q50UoCsMfqwjr7HVQaX95ANHCWsn6cAkVEvehGj9CX6zQwQYkS7fqZtt3YN2RBEe81diZ+ka14ygSRJAkywgtxw4vW+kQJ1nQwY1QPcyUpTN0xLXEIlwKx2V, subscriptionId:20ab6f85-801d-4c3a-a0d4-11da3631d29c, tenantId:7b4b14e5-87f8-4f09-9c83-f91d9b8a49fd, testConnection:true]\n[pool-1-thread-1] INFO com.microsoft.aad.adal4j.AuthenticationAuthority - [Correlation ID: df10804e-4b3b-4a76-8b2d-635f192fd5ee] Instance discovery was successful\n[DEBUG] Request details:\n  requestUrl: \'https://management.azure.com\' \n  method: \'HEAD\' \n  URI: \'/subscriptions/20ab6f85-801d-4c3a-a0d4-11da3631d29c/resourcegroups/flowqe-test-resource-group\'\n[DEBUG] queryArgs: \'[api-version:2016-09-01]\'\n[DEBUG] URL: \'https://management.azure.com/subscriptions/20ab6f85-801d-4c3a-a0d4-11da3631d29c/resourcegroups/flowqe-test-resource-group\'\n[DEBUG] request was successful 204 null\n[INFO] The Resource group flowqe-test-resource-group exists already\n[DEBUG] Request details:\n  requestUrl: \'https://management.azure.com\' \n  method: \'GET\' \n  URI: \'/subscriptions/20ab6f85-801d-4c3a-a0d4-11da3631d29c/resourceGroups/flowqe-test-resource-group/providers/Microsoft.ContainerService/containerServices/flowqe-test-cluster\'\n[DEBUG] queryArgs: \'[api-version:2016-09-30]\'\n[DEBUG] URL: \'https://management.azure.com/subscriptions/20ab6f85-801d-4c3a-a0d4-11da3631d29c/resourceGroups/flowqe-test-resource-group/providers/Microsoft.ContainerService/containerServices/flowqe-test-cluster\'\n[DEBUG] request was successful 200 [type:Microsoft.ContainerService/ContainerServices, location:eastus, id:/subscriptions/20ab6f85-801d-4c3a-a0d4-11da3631d29c/resourceGroups/flowqe-test-resource-group/providers/Microsoft.ContainerService/containerServices/flowqe-test-cluster, name:flowqe-test-cluster, properties:[provisioningState:Succeeded, orchestratorProfile:[orchestratorType:Kubernetes], masterProfile:[count:1, dnsPrefix:flowqe, fqdn:flowqe.eastus.cloudapp.azure.com, vmSize:Standard_D2_v2], agentPoolProfiles:[[name:agentflowqe, count:2, vmSize:Standard_D1, dnsPrefix:flowqeagent, fqdn:, osType:Linux]], linuxProfile:[ssh:[publicKeys:[[keyData:ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDlNyjTfvz5vrjFT/p+3qzlCokc5qLn989pAOpLwKe5xfHt2hHLjQe/sKt/8eq21Efg5LA2agRGoOwJr//z40WHHIMYmQOvtZb6kO7Uy4r81aX5dP/dZcMbe6U/EAT0VVP7VlET4Z+3mQFvj0/DbTa2dM7SQ/d46QhyOS57hHIdE5a9DnDWF99WhI2CGJ+JX6GFUT2XnwsRqDrAw+bT7NsMeBywza03q50UoCsMfqwjr7HVQaX95ANHCWsn6cAkVEvehGj9CX6zQwQYkS7fqZtt3YN2RBEe81diZ+ka14ygSRJAkywgtxw4vW+kQJ1nQwY1QPcyUpTN0xLXEIlwKx2V]]], adminUsername:ecloudadmin], servicePrincipalProfile:[clientId:a90f65da-47a1-4547-aa41-6ca06cf2551a], diagnosticsProfile:[vmDiagnostics:[enabled:false]]]]\nThe ACS with name flowqe-test-cluster exists already, updating changes\n[DEBUG] Request details:\n  requestUrl: \'https://management.azure.com\' \n  method: \'PUT\' \n  URI: \'/subscriptions/20ab6f85-801d-4c3a-a0d4-11da3631d29c/resourcegroups/flowqe-test-resource-group/providers/Microsoft.ContainerService/containerServices/flowqe-test-cluster\'\n[DEBUG] queryArgs: \'[api-version:2017-07-01]\'\n[DEBUG] URL: \'https://management.azure.com/subscriptions/20ab6f85-801d-4c3a-a0d4-11da3631d29c/resourcegroups/flowqe-test-resource-group/providers/Microsoft.ContainerService/containerServices/flowqe-test-cluster\'\n[DEBUG] Payload: {\n    "location": "eastus",\n    "properties": {\n        "orchestratorProfile": {\n            "orchestratorType": "kubernetes"\n        },\n        "masterProfile": {\n            "count": 1,\n            "fqdn": "masterflowqe",\n            "dnsPrefix": "flowqe",\n            "vmSize": "Standard_D1"\n        },\n        "agentPoolProfiles": [\n            {\n                "name": "agentflowqe",\n                "count": 2,\n                "vmSize": "Standard_D1",\n                "dnsPrefix": "flowqeagent"\n            }\n        ],\n        "linuxProfile": {\n            "adminUsername": "ecloudadmin",\n            "ssh": {\n                "publicKeys": [\n                    {\n                        "keyData": "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDlNyjTfvz5vrjFT/p+3qzlCokc5qLn989pAOpLwKe5xfHt2hHLjQe/sKt/8eq21Efg5LA2agRGoOwJr//z40WHHIMYmQOvtZb6kO7Uy4r81aX5dP/dZcMbe6U/EAT0VVP7VlET4Z+3mQFvj0/DbTa2dM7SQ/d46QhyOS57hHIdE5a9DnDWF99WhI2CGJ+JX6GFUT2XnwsRqDrAw+bT7NsMeBywza03q50UoCsMfqwjr7HVQaX95ANHCWsn6cAkVEvehGj9CX6zQwQYkS7fqZtt3YN2RBEe81diZ+ka14ygSRJAkywgtxw4vW+kQJ1nQwY1QPcyUpTN0xLXEIlwKx2V"\n                    }\n                ]\n            }\n        },\n        "servicePrincipalProfile": {\n            "clientId": "a90f65da-47a1-4547-aa41-6ca06cf2551a",\n            "secret": "SzqYyyQU1a+hJIYxJxWAAo15Br0oMTXUzvHd6qP/1qM="\n        }\n    }\n}\n[DEBUG] request was successful 200 [type:Microsoft.ContainerService/ContainerServices, location:eastus, id:/subscriptions/20ab6f85-801d-4c3a-a0d4-11da3631d29c/resourceGroups/flowqe-test-resource-group/providers/Microsoft.ContainerService/containerServices/flowqe-test-cluster, name:flowqe-test-cluster, properties:[provisioningState:Updating, orchestratorProfile:[orchestratorType:Kubernetes], masterProfile:[count:1, dnsPrefix:flowqe, vmSize:Standard_D2_v2], agentPoolProfiles:[[name:agentflowqe, count:2, vmSize:Standard_D1, dnsPrefix:flowqeagent, fqdn:, osType:Linux]], linuxProfile:[ssh:[publicKeys:[[keyData:ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDlNyjTfvz5vrjFT/p+3qzlCokc5qLn989pAOpLwKe5xfHt2hHLjQe/sKt/8eq21Efg5LA2agRGoOwJr//z40WHHIMYmQOvtZb6kO7Uy4r81aX5dP/dZcMbe6U/EAT0VVP7VlET4Z+3mQFvj0/DbTa2dM7SQ/d46QhyOS57hHIdE5a9DnDWF99WhI2CGJ+JX6GFUT2XnwsRqDrAw+bT7NsMeBywza03q50UoCsMfqwjr7HVQaX95ANHCWsn6cAkVEvehGj9CX6zQwQYkS7fqZtt3YN2RBEe81diZ+ka14ygSRJAkywgtxw4vW+kQJ1nQwY1QPcyUpTN0xLXEIlwKx2V]]], adminUsername:ecloudadmin], servicePrincipalProfile:[clientId:a90f65da-47a1-4547-aa41-6ca06cf2551a], diagnosticsProfile:[vmDiagnostics:[enabled:false]]]]\n[DEBUG] Request details:\n  requestUrl: \'https://management.azure.com\' \n  method: \'GET\' \n  URI: \'/subscriptions/20ab6f85-801d-4c3a-a0d4-11da3631d29c/resourcegroups/flowqe-test-resource-group/providers/Microsoft.ContainerService/containerServices/flowqe-test-cluster\'\n[DEBUG] queryArgs: \'[api-version:2016-09-30]\'\n[DEBUG] URL: \'https://management.azure.com/subscriptions/20ab6f85-801d-4c3a-a0d4-11da3631d29c/resourcegroups/flowqe-test-resource-group/providers/Microsoft.ContainerService/containerServices/flowqe-test-cluster\'\n[DEBUG] request was successful 200 [type:Microsoft.ContainerService/ContainerServices, location:eastus, id:/subscriptions/20ab6f85-801d-4c3a-a0d4-11da3631d29c/resourceGroups/flowqe-test-resource-group/providers/Microsoft.ContainerService/containerServices/flowqe-test-cluster, name:flowqe-test-cluster, properties:[provisioningState:Succeeded, orchestratorProfile:[orchestratorType:Kubernetes], masterProfile:[count:1, dnsPrefix:flowqe, fqdn:flowqe.eastus.cloudapp.azure.com, vmSize:Standard_D2_v2], agentPoolProfiles:[[name:agentflowqe, count:2, vmSize:Standard_D1, dnsPrefix:flowqeagent, fqdn:, osType:Linux]], linuxProfile:[ssh:[publicKeys:[[keyData:ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDlNyjTfvz5vrjFT/p+3qzlCokc5qLn989pAOpLwKe5xfHt2hHLjQe/sKt/8eq21Efg5LA2agRGoOwJr//z40WHHIMYmQOvtZb6kO7Uy4r81aX5dP/dZcMbe6U/EAT0VVP7VlET4Z+3mQFvj0/DbTa2dM7SQ/d46QhyOS57hHIdE5a9DnDWF99WhI2CGJ+JX6GFUT2XnwsRqDrAw+bT7NsMeBywza03q50UoCsMfqwjr7HVQaX95ANHCWsn6cAkVEvehGj9CX6zQwQYkS7fqZtt3YN2RBEe81diZ+ka14ygSRJAkywgtxw4vW+kQJ1nQwY1QPcyUpTN0xLXEIlwKx2V]]], adminUsername:ecloudadmin], servicePrincipalProfile:[clientId:a90f65da-47a1-4547-aa41-6ca06cf2551a], diagnosticsProfile:[vmDiagnostics:[enabled:false]]]]\n[INFO] Container cluster Update complete\n'
    }

}