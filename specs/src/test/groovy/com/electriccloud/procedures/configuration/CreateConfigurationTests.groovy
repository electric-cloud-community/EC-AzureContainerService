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
        String logs = acsClient.client.getJobLogs(job.json.jobId)
        def jobStatus = acsClient.client.getJobStatus(job.json.jobId).json
        def jobSteps = acsClient.client.getJobSteps(job.json.jobId).json.object
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert logs.contains("Successfully connected to the Azure Container Service using the given account details")
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
                [LogLevel.WARNING, "logger WARNING", "[INFO]", "[DEBUG]"],
                [LogLevel.ERROR, "logger ERROR", "[INFO]", "[DEBUG]"],
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
                [configName, publicKey, privateKey, credPrivateKey, credClientId, "", subscriptionId, LogLevel.DEBUG, "Invalid client secret is provided.", 'error', 'completed'],
                [configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId.substring(5), subscriptionId, LogLevel.DEBUG, "Invalid client secret is provided.", 'error', 'completed'],
                [configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, "", LogLevel.DEBUG, "Invalid client secret is provided.", 'error', 'completed'],
                [configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId.substring(5), LogLevel.DEBUG, "Invalid client secret is provided.", 'error', 'completed']
        ] as Object[][]
    }


}