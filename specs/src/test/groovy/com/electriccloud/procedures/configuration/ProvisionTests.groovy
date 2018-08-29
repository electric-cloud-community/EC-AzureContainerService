package com.electriccloud.procedures.configuration

import com.electriccloud.helpers.enums.LogLevels
import com.electriccloud.procedures.AzureTestBase
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test


@Feature('Environment Provision')
class ProvisionTests extends AzureTestBase {

    @BeforeClass
    void setUpTests(){
        acsClient.deleteConfiguration(configName)
        acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, LogLevels.LogLevel.DEBUG)
        acsClient.createEnvironment(configName, adminAccount, acsClusterName, resourceGroup, 2)
    }

    @AfterClass
    void tearDownTests(){
        acsClient.deleteConfiguration(configName)
        acsClient.client.deleteProject(environmentProjectName)
    }



    @Test
    @TmsLink("")
    @Story("Provisioning of openshift environment")
    @Description("Provision Existing Azure cluster")
    void provisionCluster() {
        def resp = acsClient.provisionEnvironment(projectName, environmentName, clusterName).json
        def jobStatus = acsClient.client.getJobStatus(resp.jobId)
        def jobLogs = acsClient.client.getJobLogs(resp.jobId)
        assert jobStatus.json.outcome == "success"
        assert jobStatus.json.status == "completed"
        assert jobLogs.contains("The service is reachable at ${clusterEndpoint}. Health check at ${clusterEndpoint}.")
    }




    @Test(dataProvider = 'invalidData')
    @TmsLink("")
    @Story('Provisioning of openshift environment with invalid data')
    @Description("Provision Openshift cluster with invalid data")
    void invalidClusterProvisioning(project, environment, cluster, message){
        try {
            acsClient.provisionEnvironment(project, environment, cluster).json
        } catch (e){
            assert e.cause.message.contains(message)
        }
    }


    @DataProvider(name = 'invalidData')
    def getProvisionData(){
        def data = [
                ["test", environmentName, clusterName, "NoSuchProject: Project 'test' does not exist"],
                ["Default", environmentName, clusterName, "NoSuchEnvironment: Environment '${environmentName}' does not exist in project 'Default'"],
                [projectName, "test", clusterName, "NoSuchEnvironment: Environment 'test' does not exist in project '${projectName}'"],
                [projectName, environmentName, "test-cluster", "NoSuchCluster: Cluster 'test-cluster' does not exist in environment '${environmentName}'"],
        ]
        return data as Object[][]
    }



}