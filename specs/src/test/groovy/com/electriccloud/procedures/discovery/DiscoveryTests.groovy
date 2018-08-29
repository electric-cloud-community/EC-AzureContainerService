package com.electriccloud.procedures.discovery

import com.electriccloud.procedures.AzureTestBase
import io.qameta.allure.*
import org.testng.annotations.*

import static com.electriccloud.helpers.enums.LogLevels.*
import static com.electriccloud.helpers.enums.ServiceTypes.*

@Feature("Discovery")
class DiscoveryTests extends AzureTestBase {


    @BeforeClass
    void setUpTests(){
        acsClient.deleteConfiguration(configName)
        acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, LogLevel.DEBUG)
        acsClient.createEnvironment(configName, adminAccount, acsClusterName, resourceGroup, 2)
        acsClient.createService(2, volumes, false, ServiceType.LOAD_BALANCER)
        acsClient.deployService(projectName, serviceName)
    }

    @BeforeMethod
    void setUpTest(){
        acsClient.client.deleteService(projectName, serviceName)
        acsClient.client.deleteApplication(projectName, applicationName)
    }

    @AfterMethod
    void tearDownTest(){
        acsClient.client.deleteService(projectName, serviceName)
        acsClient.client.deleteApplication(projectName, applicationName)
        acsClient.client.deleteProject('MyProject')
    }

    @AfterClass
    void tearDownTests(){
        acsClient.createService(2, volumes, false, ServiceType.LOAD_BALANCER)
        acsClient.undeployService(projectName, serviceName)
        acsClient.client.deleteProject(projectName)
        acsClient.deleteConfiguration(configName)
    }



    @Test
    @TmsLink("363534")
    @Story("Microservice discovery")
    @Description("Discover Project-level Microservice")
    void discoverProjectLevelMicroservice(){
        acsClient.discoverService(projectName,
                environmentProjectName,
                environmentName,
                clusterName)
        def services = acsClient.client.getServices(projectName).json.service
        def service = acsClient.client.getService(projectName, serviceName).json.service
        def container = acsClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        def mapping = acsClient.getServiceMappings(projectName, serviceName)[0].serviceClusterMappings.serviceClusterMapping[0]
        assert services.size() == 1
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.environmentMapCount == '1'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html-content\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert mapping.actualParameters.parameterDetail[0].parameterName == "serviceType"
        assert mapping.actualParameters.parameterDetail[0].parameterValue == ServiceType.LOAD_BALANCER.value
    }


    @Test
    @TmsLink("363536")
    @Story("Microservice discovery")
    @Description("Discover Project-level Microservice with environment generation")
    void discoverProjectLevelMicroserviceWithEnvironmentGeneration(){
        acsClient.discoverService(projectName,
                environmentProjectName,
                "my-environment",
                clusterName,
                'default',
                acsClusterName,
                resourceGroup,
                1,
                credClientId,
                credPrivateKey,
                privateKey,
                publicKey,
                subscriptionId,
                tenantId,
                false, null)
        def services = acsClient.client.getServices(projectName).json.service
        def service = acsClient.client.getService(projectName, serviceName).json.service
        def environment = acsClient.client.getEnvironment(projectName, "my-environment").json.environment
        def container = acsClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        def mappings = acsClient.getServiceMappings(projectName, serviceName)
        def mapping = mappings[0].serviceClusterMappings.serviceClusterMapping[0]
        assert services.size() == 1
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.environmentMapCount == '1'
        assert service.defaultCapacity == '2'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html-content\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert environment.environmentName == "my-environment"
        assert environment.environmentEnabled == '1'
        assert environment.projectName == projectName
        assert mappings.size() == 1
        assert mappings[0].environmentName == "my-environment"
        assert mapping.actualParameters.parameterDetail[0].parameterName == "serviceType"
        assert mapping.actualParameters.parameterDetail[0].parameterValue == ServiceType.LOAD_BALANCER.value
    }



    @Test
    @TmsLink("363537")
    @Story("Microservice discovery")
    @Description("Discover Project-level Microservice with project generation ")
    void discoverProjectLevelMicroserviceWithProjectGeneration(){
        acsClient.discoverService(projectName,
                "MyProject",
                environmentName,
                clusterName,
                'default',
                acsClusterName,
                resourceGroup,
                1,
                credClientId,
                credPrivateKey,
                privateKey,
                publicKey,
                subscriptionId, tenantId)
        def services = acsClient.client.getServices(projectName).json.service
        def service = acsClient.client.getService(projectName, serviceName).json.service
        def environment = acsClient.client.getEnvironment("MyProject", environmentName).json.environment
        def container = acsClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        def mappings = acsClient.getServiceMappings(projectName, serviceName)
        def mapping = mappings[0].serviceClusterMappings.serviceClusterMapping[0]
        assert services.size() == 1
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.environmentMapCount == '1'
        assert service.defaultCapacity == '2'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html-content\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert environment.environmentName == environmentName
        assert environment.environmentEnabled == '1'
        assert environment.projectName == 'MyProject'
        assert mappings.size() == 1
        assert mappings[0].environmentName == environmentName
        assert mapping.actualParameters.parameterDetail[0].parameterName == "serviceType"
        assert mapping.actualParameters.parameterDetail[0].parameterValue == ServiceType.LOAD_BALANCER.value
    }



    @Test
    @TmsLink("363538")
    @Story("Application discovery")
    @Description("Discover Application-level Microservice")
    void discoverApplicationLevelMicroservice(){
        acsClient.discoverService(projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                'default',
                null,
                null,
                1,
                null,
                null,
                "",
                "",
                null,
                null,
                true, applicationName)
        def applications = acsClient.client.getApplications(projectName).json.application
        def application = acsClient.client.getApplication(projectName, applicationName).json.application
        def container = acsClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        def mapping = acsClient.getAppMappings(projectName, applicationName)[0].serviceClusterMappings.serviceClusterMapping[0]
        assert applications.size() == 1
        assert application.applicationName == applicationName
        assert application.containerCount == '1'
        assert application.description == 'Created by EF Discovery'
        assert application.projectName == projectName
        assert application.tiermapCount == '1'
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert mapping.actualParameters.parameterDetail[0].parameterName == "serviceType"
        assert mapping.actualParameters.parameterDetail[0].parameterValue == ServiceType.LOAD_BALANCER.value
    }


    @Test
    @TmsLink("363539")
    @Story("Application discovery")
    @Description("Discover Application-level Microservice with environment generation")
    void discoverApplicationLevelMicroserviceWithEnvironmentGeneration(){
        acsClient.discoverService(projectName,
                environmentProjectName,
                "my-environment",
                clusterName,
                'default',
                acsClusterName,
                resourceGroup,
                1,
                credClientId,
                credPrivateKey,
                privateKey,
                publicKey,
                subscriptionId,
                tenantId,
                true, applicationName)
        def applications = acsClient.client.getApplications(projectName).json.application
        def application = acsClient.client.getApplication(projectName, applicationName).json.application
        def environment = acsClient.client.getEnvironment(projectName, "my-environment").json.environment
        def container = acsClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        def mappings = acsClient.getAppMappings(projectName, applicationName)
        def mapping = mappings[0].serviceClusterMappings.serviceClusterMapping[0]
        assert applications.size() == 1
        assert application.applicationName == applicationName
        assert application.containerCount == '1'
        assert application.description == 'Created by EF Discovery'
        assert application.projectName == projectName
        assert application.tiermapCount == '1'
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert environment.environmentName == "my-environment"
        assert environment.environmentEnabled == '1'
        assert environment.projectName == projectName
        assert mappings.size() == 1
        assert mappings[0].environmentName == "my-environment"
        assert mapping.actualParameters.parameterDetail[0].parameterName == "serviceType"
        assert mapping.actualParameters.parameterDetail[0].parameterValue == ServiceType.LOAD_BALANCER.value
    }


    @Test
    @TmsLink("363540")
    @Story("Microservice discovery")
    @Description(" Discover Application-level Microservice with project generation")
    void discoverApplicationLevelMicroserviceWithProjectGeneration(){
        acsClient.discoverService(projectName,
                "MyProject",
                environmentName,
                clusterName,
                "default",
                acsClusterName,
                resourceGroup,
                1,
                credClientId,
                credPrivateKey,
                privateKey,
                publicKey,
                subscriptionId,
                tenantId,
                true, applicationName)
        def applications = acsClient.client.getApplications(projectName).json.application
        def application = acsClient.client.getApplication(projectName, applicationName).json.application
        def environment = acsClient.client.getEnvironment("MyProject", environmentName).json.environment
        def container = acsClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        def mappings = acsClient.getAppMappings(projectName, applicationName)
        def mapping = mappings[0].serviceClusterMappings.serviceClusterMapping[0]
        assert applications.size() == 1
        assert application.applicationName == applicationName
        assert application.containerCount == '1'
        assert application.description == 'Created by EF Discovery'
        assert application.projectName == projectName
        assert application.tiermapCount == '1'
        assert container.containerName == containerName
        assert container.imageName == "tomaskral/nonroot-nginx"
        assert container.imageVersion == "latest"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable.first().environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable.first().value == "8080"
        assert environment.environmentName == environmentName
        assert environment.environmentEnabled == '1'
        assert environment.projectName == "MyProject"
        assert mappings.size() == 1
        assert mappings[0].environmentName == environmentName
        assert mapping.actualParameters.parameterDetail[0].parameterName == "serviceType"
        assert mapping.actualParameters.parameterDetail[0].parameterValue == ServiceType.LOAD_BALANCER.value
    }




    @Test
    @TmsLink("363542")
    @Story("Invalid Microservice discovery")
    @Description("Unable to discover Project-level Microservice that already exist")
    void unableToDiscoverExistingProjectLevelMicroservice() {
        try {
            acsClient.discoverService(projectName,
                    environmentProjectName,
                    environmentName,
                    clusterName)
            acsClient.discoverService(projectName,
                    environmentProjectName,
                    environmentName,
                    clusterName)
        } catch (e) {
            def jobId = e.cause.message
            String errorLog = acsClient.client.getJobLogs(jobId)
            def jobStatus = acsClient.client.getJobStatus(jobId).json
            assert errorLog.contains("Service ${serviceName} already exists")
            assert errorLog.contains("Container ${containerName} already exists")
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }



    @Test
    @TmsLink("363564")
    @Story("Invalid Application discovery")
    @Description("Unable to Discover Application-level Microservice that already exist")
    void unableToDiscoverExistingApplicationLevelMicroservice() {
        try {
            acsClient.discoverService(projectName,
                    environmentProjectName,
                    environmentName,
                    clusterName,
                    'default',
                    null,
                    null,
                    1,
                    null,
                    null,
                    "",
                    "",
                    null,
                    null,
                    true, applicationName)
            acsClient.discoverService(projectName,
                    environmentProjectName,
                    environmentName,
                    clusterName,
                    'default',
                    null,
                    null,
                    1,
                    null,
                    null,
                    "",
                    "",
                    null,
                    null,
                    true, applicationName)
        } catch (e) {
            def jobId = e.cause.message
            String errorLog = acsClient.client.getJobLogs(jobId)
            def jobStatus = acsClient.client.getJobStatus(jobId).json
            assert errorLog.contains("Application ${applicationName} already exists in project ${projectName}")
            assert errorLog.contains("Process \'Deploy\' already exists in application \'${applicationName}\'")
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }



    @Test
    @TmsLink("363543")
    @Story("Invalid Microservice discovery")
    @Description("Unable to discover Project-level Microservice with invalid Namespace ")
    void unableToDiscoverMicroserviceWithInvalidNamespace(){
        def resp =  acsClient.discoverService(projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                "my-namespace").json
        def jobStatus = acsClient.client.getJobStatus(resp.jobId).json
        String jobLog = acsClient.client.getJobLogs(resp.jobId)
        assert jobStatus.outcome == "warning"
        assert jobStatus.status == "completed"
        assert jobLog.contains("No services found on the cluster https://flowqe.eastus.cloudapp.azure.com")
        assert jobLog.contains("Discovered services: 0")
    }



    @Test(priority = 1)
    @TmsLink("363542")
    @Story("Invalid Microservice discovery")
    @Description("Unable to Discover Microservice without plugin configuration")
    void unableToDiscoverMicroserviceWithoutConfiguration() {
        try {
            acsClient.deleteConfiguration(configName)
            acsClient.discoverService(projectName,
                    environmentProjectName,
                    environmentName,
                    clusterName)
        } catch (e) {
            def jobId = e.cause.message
            String errorLog = acsClient.client.getJobLogs(jobId)
            def jobStatus = acsClient.client.getJobStatus(jobId).json
            assert errorLog.contains("Configuration ${configName} does not exist!")
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }





    @Test(dataProvider = 'invalidDiscoveryData')
    @TmsLinks([
            @TmsLink("363545"),
            @TmsLink("363546"),
            @TmsLink("363547"),
            @TmsLink("363558"),
            @TmsLink("363559"),
            @TmsLink("363560"),
            @TmsLink("363561"),
            @TmsLink("363562")
    ])
    @Story("Invalid Microservice discovery")
    @Description("Microservice Discovery with Invalid Data")
    void projectLevelMicroserviceDiscoveryWithInvalidInputData(project, envProject, envName, clusterName, namespace, azCluster, resourceGroup, agentPoolCount, credClientId, credPrivateKey, privateKey, publicKey, subscribtionId, tenantId,  errorMessage) {
        try {
            acsClient.discoverService(project,
                    envProject,
                    envName,
                    clusterName,
                    namespace,
                    azCluster,
                    resourceGroup,
                    agentPoolCount,
                    credClientId,
                    credPrivateKey,
                    privateKey,
                    publicKey,
                    subscribtionId,
                    tenantId,
                    false, null)
        } catch (e){
            def jobId = e.cause.message
            String errorLog = acsClient.client.getJobLogs(jobId)
            println errorLog
            def jobStatus = acsClient.client.getJobStatus(jobId).json
            assert errorLog.contains(errorMessage)
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }


    @DataProvider(name = 'invalidDiscoveryData')
    def getDiscoveryData() {
        def data = [
               [ '', environmentProjectName, environmentName, clusterName, "default", acsClusterName, resourceGroup, 1, credClientId, credPrivateKey, privateKey, publicKey, subscriptionId, tenantId, 'One or more arguments are missing. Please provide the following arguments: projectName' ],
               [ projectName, '', environmentName, clusterName, "default", null, null, 1, null, null, "", "", null, null, 'One or more arguments are missing. Please provide the following arguments: projectName' ],
               [ projectName, environmentProjectName, '', clusterName, "default", null, null, 1, null, null, "", "", null, null, '\'environmentName\' must be between 1 and 255 characters' ],
               [ projectName, environmentProjectName, environmentName, '', "default", null, null, 1, null, null, "", "", null, null, 'Please provide the following arguments: clusterName' ],
               [ 'MyTestProject', environmentProjectName, environmentName, clusterName, "default", null, null, 1, null, null, "", "", null, null, 'Project \'MyTestProject\' does not exist' ]

        ]
        return data as Object[][]
    }



}
