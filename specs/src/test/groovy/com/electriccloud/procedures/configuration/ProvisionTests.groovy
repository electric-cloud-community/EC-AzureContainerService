package com.electriccloud.procedures.configuration

import com.electriccloud.helpers.enums.LogLevels
import com.electriccloud.procedures.AzureTestBase
import com.microsoft.azure.management.compute.VirtualMachine
import com.microsoft.azure.management.containerservice.ContainerService
import com.microsoft.azure.management.resources.ResourceGroup
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

    def testResGroup = "flowqe-test-resource-group2"
    def testContService = "flowqe-test-cluster2"


    @BeforeClass
    void setUpTests(){
        acsClient.deleteConfiguration(configName)
        acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, LogLevels.LogLevel.DEBUG)
    }

    @AfterClass
    void tearDownTests(){
        acsClient.client.deleteProject(environmentProjectName)
        println "Deleting the resource group: $testResGroup"
        acsApi.azure.resourceGroups().beginDeleteByName(testResGroup)
    }




    @Test(testName = "Provision new cluster")
    @TmsLink("")
    @Story("Environment Provisioning")
    @Description("Provision New Azure cluster")
    void provisionNewCluster() {
        acsClient.createEnvironment(configName,
                adminAccount,
                testContService,
                testResGroup,
                2,
                "flowqeagent2",
                "agentflowqe2",
                "flowqe2",
                "masterflowqe2")
        def resp = acsClient.provisionEnvironment(projectName, environmentName, clusterName, 750).json
        def jobStatus = acsClient.client.getJobStatus(resp.jobId)
        def jobLogs = acsClient.client.getJobLogs(resp.jobId)
        def resGroups = acsApi.azure.resourceGroups()
        def contServices = acsApi.azure.containerServices()
        def contService = contServices.getByResourceGroup(testResGroup, testContService)
        assert jobStatus.json.outcome == "success"
        assert jobStatus.json.status == "completed"
        assert jobLogs.contains("Container cluster creation complete")
        assert contServices.list().findAll { it.name() == testContService}.size() == 1
        assert contService.region().toString() == "eastus"
        assert contService.orchestratorType().toString() == "Kubernetes"
        assert contService.resourceGroupName() == testResGroup
        assert resGroups.list().findAll { it.name() == testResGroup } .size() == 1
        assert acsApi.azure.virtualMachines().listByResourceGroup(testResGroup).size() == 3
    }


    @Test(testName = "Provision cluster that allready exits")
    @TmsLink("")
    @Story("Environment Provisioning")
    @Description("Provision Existing Azure cluster")
    void provisionExistingCluster() {
        acsClient.createEnvironment(configName,
                adminAccount,
                acsClusterName,
                resourceGroup,
                2)
        def resp = acsClient.provisionEnvironment(projectName, environmentName, clusterName).json
        def jobStatus = acsClient.client.getJobStatus(resp.jobId)
        def jobLogs = acsClient.client.getJobLogs(resp.jobId)
        def resGroups = acsApi.azure.resourceGroups()
        def contServices = acsApi.azure.containerServices()
        def contService = contServices.getByResourceGroup(resourceGroup, acsClusterName)
        assert jobStatus.json.outcome == "success"
        assert jobStatus.json.status == "completed"
        assert jobLogs.contains("Container cluster Update complete")
        assert contServices.list().findAll { it.name() == acsClusterName}.size() == 1
        assert contService.region().toString() == "eastus"
        assert contService.orchestratorType().toString() == "Kubernetes"
        assert contService.resourceGroupName() == resourceGroup
        assert resGroups.list().findAll { it.name() == resourceGroup } .size() == 1
        assert  acsApi.azure.virtualMachines().listByResourceGroup(resourceGroup).size() == 3
    }


    @Test(dataProvider = 'invalidData',
            testName = "Provision cluster with invalid data")
    @TmsLink("")
    @Story("Environment Provisioning with invalid Data")
    @Description("Provision Openshift cluster with invalid data")
    void invalidClusterProvisioning(project, environment, cluster, message){
        acsClient.createEnvironment(configName, adminAccount, acsClusterName, resourceGroup, 2)
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






    /*@Test
    void testCluster(){
        ResourceGroup resGroup = acsApi.azure.resourceGroups().getByName(resourceGroup)
        println    resGroup.name()
        println  resGroup.type()
        println resGroup.inner()
        println resGroup.key()
        println resGroup.region()
        ContainerService cs = acsApi.azure.containerServices().getByResourceGroup(resourceGroup, acsClusterName)


        println "------------------"
        println cs.name()
        println cs.region()
        println cs.orchestratorType()
        println cs.masterFqdn()
        println cs.masterDnsPrefix()
        println cs.masterNodeCount()
        println cs.inner().agentPoolProfiles()
        VirtualMachine vm = acsApi.azure.virtualMachines().getByResourceGroup(resourceGroup, 'k8s-agent-E6A2B3B5-0')
        println '-----------'
        println vm.name()
        println vm.region()
        println vm.primaryPublicIPAddress
        println vm.size()

        acsApi.azure.virtualMachines().list().findAll {
            it.resourceGroupName() == resourceGroup
        }.each {
            println it.name()
        }

    }*/



}