package com.electriccloud.procedures.gather_cluster_info

import com.electriccloud.helpers.enums.LogLevels
import com.electriccloud.helpers.enums.ServiceTypes
import com.electriccloud.procedures.AzureTestBase
import io.qameta.allure.Feature
import io.qameta.allure.Issue
import io.qameta.allure.TmsLink
import org.junit.jupiter.api.Assertions
import org.testng.Assert
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import org.testng.asserts.Assertion
import org.testng.asserts.SoftAssert

import static org.testng.Assert.expectThrows


@Feature("Gather Cluster Info")
class GatherClusterInfoTests extends AzureTestBase {

    @BeforeClass
    void setUpTests(){
        k8sClient.deleteConfiguration(configName)
        acsClient.deleteConfiguration(configName)
        k8sClient.createConfiguration(configName, clusterEndpoint, adminAccount, clusterToken, "1.8", true, '/api/v1/namespaces')
        acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, LogLevels.LogLevel.DEBUG)
        acsClient.createEnvironment(configName, adminAccount, acsClusterName, resourceGroup, 2)
    }


    @AfterClass
    void tearDownTests(){
        acsClient.client.deleteProject(projectName)
    }

    @Test
    @Issue("ECAZCS-108")
    @TmsLink("")
    void gatherInfoFromCluster(){
        def jobId = acsClient.gatherClusterInfo(environmentProjectName, environmentName, clusterName).json.jobId
        def jobStatus = acsClient.client.getJobStatus(jobId).json
        def jobLog = acsClient.client.getJobLogs(jobId)
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert jobLog.contains("ec_clusterAccessTokenEncrypted")
        assert jobLog.contains("acsConfig_keypair")
        assert !jobLog.contains(clusterToken)
    }





    @Test(dataProvider = "invalidData")
    @TmsLink("")
    @Issue("")
    void gatherInfoFromClusterWithInvalidData(envProjectName, envName, clusterName, errorMessage){
        try {
            acsClient.gatherClusterInfo(envProjectName, envName, clusterName)
        } catch(e) {
            def jobId = e.cause.message
            def jobStatus = acsClient.client.getJobStatus(jobId).json
            def jobLog = acsClient.client.getJobLogs(jobId)
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
            assert jobLog.contains(errorMessage)
            assert !jobLog.contains(clusterToken)
        }

    }


    @DataProvider(name = "invalidData")
    def invalidData(){
        def data = [
                [
                        "", environmentName, clusterName,
                        "'projectName' is required and must be between 1 and 255 characters"
                ],
                [
                        environmentProjectName, "", clusterName,
                        "Cluster 'acs-cluster' does not exist in project 'acsProj'"
                ],
                [
                        environmentProjectName, environmentName, "",
                        "[ERROR] Response: [error:[where:, message:, details:, code:NotImplemented]]"
                ],
                [
                        "test", environmentName, clusterName,
                        "Project \'test\' does not exist"
                ],
                [
                        environmentProjectName, "test", clusterName,
                        "Environment \'test\' does not exist in project \'${environmentProjectName}\'"
                ],
                [
                        environmentProjectName, environmentName, "test",
                        "Cluster \'test\' does not exist in environment \'${environmentName}\'"
                ],

        ]
        return data as Object[][]
    }


}
