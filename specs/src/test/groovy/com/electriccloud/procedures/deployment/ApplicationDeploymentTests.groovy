package com.electriccloud.procedures.deployment


import com.electriccloud.procedures.AzureTestBase
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Flaky
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import java.util.concurrent.TimeUnit

import static com.electriccloud.helpers.enums.LogLevels.*
import static org.awaitility.Awaitility.await
import static com.electriccloud.helpers.enums.ServiceTypes.*

@Feature("Deployment")
class ApplicationDeploymentTests extends AzureTestBase {


    @BeforeClass
    void setUpTests(){
        k8sClient.deleteConfiguration(configName)
        acsClient.deleteConfiguration(configName)
        k8sClient.createConfiguration(configName, clusterEndpoint, adminAccount, clusterToken, "1.8", true, '/api/v1/namespaces')
        acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, LogLevel.DEBUG)
    }


    @BeforeMethod
    void setUpTest(){
        acsClient.createEnvironment(configName, adminAccount, acsClusterName, resourceGroup, 2)
        acsClient.createApplication(2, volumes, false, ServiceType.LOAD_BALANCER)
    }


    @AfterMethod
    void tearDownTest(){
        k8sClient.cleanUpCluster(configName, "default")
        await().atMost(50, TimeUnit.SECONDS).until { k8sApi.getPods().json.items.size() == 0 }
        acsClient.client.deleteProject(projectName)
    }



    @Test(testName = "Deploy Application-Level Microservice")
    @TmsLink("")
    @Story("Deploy Microservcice")
    @Description("Deploy Application-Level Microservice")
    void deployApplicationLevelMicroservice(){
        def jobId = acsClient.deployApplication(projectName, applicationName).json.jobId
        def deploymentLog = acsClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert pods.size() == 2
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == ServiceType.LOAD_BALANCER.value
        assert services[1].spec.ports.first().port == 81
        assert deployments.size() == 1
        assert deployments[0].metadata.name == serviceName
        assert deployments[0].metadata.labels."ec-track" == "stable"
        assert deployments[0].spec.replicas == 2
        assert pods.last().metadata.generateName.startsWith('nginx-service-')
        assert pods.first().metadata.namespace == "default"
        assert pods.first().spec.containers.first().name == containerName
        assert pods.first().metadata.labels.get('ec-svc') == serviceName
        assert pods.first().metadata.labels.get('ec-track') == "stable"
        assert pods.first().spec.containers.first().image == "tomaskral/nonroot-nginx:latest"
        assert pods.first().spec.containers.first().ports.first().containerPort == 8080
        assert pods.first().spec.containers.first().volumeMounts.first().mountPath == "/usr/share/nginx/html"
        assert pods.first().spec.containers.first().env.first().value == "8080"
        assert pods.first().spec.containers.first().env.first().name == "NGINX_PORT"
        assert pods.first().status.phase == "Running"
        assert resp.statusCode() == 200
        assert resp.body().asString() == "Hello World!\n"
        assert !deploymentLog.contains(clusterToken)
    }




    @Test(testName = "Update Application-Level Microservice")
    @TmsLink("")
    @Story('Update Microservice')
    @Description("Update Application-level Microservice with the same data")
    void updateApplicationLevelMicroserviceWithSameData(){
        acsClient.deployApplication(projectName, applicationName)
        acsClient.createApplication(2, volumes, false, ServiceType.LOAD_BALANCER)
        def jobId = acsClient.deployApplication(projectName, applicationName).json.jobId
        def deploymentLog = acsClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert pods.size() == 2
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == ServiceType.LOAD_BALANCER.value
        assert services[1].spec.ports.first().port == 81
        assert deployments.size() == 1
        assert deployments[0].metadata.name == serviceName
        assert deployments[0].metadata.labels."ec-track" == "stable"
        assert deployments[0].spec.replicas == 2
        assert pods.last().metadata.generateName.startsWith('nginx-service-')
        assert pods.first().metadata.namespace == "default"
        assert pods.first().spec.containers.first().name == containerName
        assert pods.first().metadata.labels.get('ec-svc') == serviceName
        assert pods.first().metadata.labels.get('ec-track') == "stable"
        assert pods.first().spec.containers.first().image == "tomaskral/nonroot-nginx:latest"
        assert pods.first().spec.containers.first().ports.first().containerPort == 8080
        assert pods.first().spec.containers.first().volumeMounts.first().mountPath == "/usr/share/nginx/html"
        assert pods.first().spec.containers.first().env.first().value == "8080"
        assert pods.first().spec.containers.first().env.first().name == "NGINX_PORT"
        assert pods.first().status.phase == "Running"
        assert resp.statusCode() == 200
        assert resp.body().asString() == "Hello World!\n"
        assert !deploymentLog.contains(clusterToken)
    }




    @Test(testName = "Scale Application-Level Microservice ")
    @TmsLink("")
    @Story('Update Microservice')
    @Description("Update Application-level Microservice")
    void updateApplicationLevelMicroservice(){
        acsClient.deployApplication(projectName, applicationName)
        acsClient.createApplication(3, volumes, false, ServiceType.LOAD_BALANCER)
        def jobId = acsClient.deployApplication(projectName, applicationName).json.jobId
        def deploymentLog = acsClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert pods.size() == 3
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == ServiceType.LOAD_BALANCER.value
        assert services[1].spec.ports.first().port == 81
        assert deployments.size() == 1
        assert deployments[0].metadata.name == serviceName
        assert deployments[0].metadata.labels."ec-track" == "stable"
        assert deployments[0].spec.replicas == 3
        assert pods.last().metadata.generateName.startsWith('nginx-service-')
        assert pods.first().metadata.namespace == "default"
        assert pods.first().spec.containers.first().name == containerName
        assert pods.first().metadata.labels.get('ec-svc') == serviceName
        assert pods.first().metadata.labels.get('ec-track') == "stable"
        assert pods.first().spec.containers.first().image == "tomaskral/nonroot-nginx:latest"
        assert pods.first().spec.containers.first().ports.first().containerPort == 8080
        assert pods.first().spec.containers.first().volumeMounts.first().mountPath == "/usr/share/nginx/html"
        assert pods.first().spec.containers.first().env.first().value == "8080"
        assert pods.first().spec.containers.first().env.first().name == "NGINX_PORT"
        assert pods.first().status.phase == "Running"
        assert resp.statusCode() == 200
        assert resp.body().asString() == "Hello World!\n"
        assert !deploymentLog.contains(clusterToken)
    }



    @Test(testName = "Deploy Canary Application-Level Microservice ")
    @TmsLink("")
    @Story('Canary deploy of Microservice')
    @Description("Canary Deploy for Application-level Microservice")
    void preformCanaryDeploymentForApplicationLevelMicroservice() {
        acsClient.deployApplication(projectName, applicationName)
        acsClient.createApplication(2, volumes, true, ServiceType.LOAD_BALANCER)
        def jobId = acsClient.deployApplication(projectName, applicationName).json.jobId
        def deploymentLog = acsClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert pods.size() == 4
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == ServiceType.LOAD_BALANCER.value
        assert services[1].spec.ports.first().port == 81
        assert deployments.size() == 2
        assert deployments[0].metadata.name == serviceName
        assert deployments[1].metadata.name == "nginx-service-canary"
        assert deployments[0].metadata.labels."ec-track" == "stable"
        assert deployments[1].metadata.labels."ec-track" == "canary"
        assert deployments[0].spec.replicas == 2
        assert deployments[1].spec.replicas == 2
        assert pods.last().metadata.generateName.startsWith('nginx-service-')
        assert pods.first().metadata.namespace == "default"
        assert pods.first().metadata.labels.get('ec-svc') == serviceName
        assert pods.first().metadata.labels.get('ec-track') == "stable"
        assert pods.last().metadata.generateName.startsWith('nginx-service-canary-')
        assert pods.last().metadata.namespace == "default"
        assert pods.last().metadata.labels.get('ec-svc') == serviceName
        assert pods.last().metadata.labels.get('ec-track') == "canary"
        pods.each {
            assert it.spec.containers.first().image == "tomaskral/nonroot-nginx:latest"
            assert it.spec.containers.first().ports.first().containerPort == 8080
            assert it.spec.containers.first().volumeMounts.first().mountPath == "/usr/share/nginx/html"
            assert it.spec.containers.first().env.first().value == "8080"
            assert it.spec.containers.first().env.first().name == "NGINX_PORT"
            assert it.status.phase == "Running"
        }
        assert resp.statusCode() == 200
        assert resp.body().asString() == "Hello World!\n"
        assert !deploymentLog.contains(clusterToken)
    }



    @Test(testName = "Undeploy Application-Level Microservice")
    @TmsLink("")
    @Story('Undeploy Microservice')
    @Description("Undeploy Application-level Microservice")
    void undeployApplicationLevelMicroservice() {
        acsClient.deployApplication(projectName, applicationName)
        def jobId = acsClient.undeployApplication(projectName, applicationName).json.jobId
        def deploymentLog = acsClient.client.getJobLogs(jobId)
        await("Pods size to be: 0").until {
            k8sApi.getPods().json.items.size() == 0
        }
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        assert deployments.size() == 0
        assert services.size() == 1
        assert pods.size() == 0
        assert services[0].metadata.name == "kubernetes"
        assert !deploymentLog.contains(clusterToken)
    }



    @Test(testName = "Undeploy Canary Application-Level Microservice")
    @Flaky
    @TmsLink("")
    @Story('Undeploy Microservice after Canary deployment')
    @Description("Undeploy Application-level Microservice after Canary Deploy")
    void undeployApplicationLevelMicroserviceAfterCanaryDeployment() {
        acsClient.deployApplication(projectName, applicationName)
        acsClient.updateApplication(2, volumes, true, ServiceType.LOAD_BALANCER)
        acsClient.deployApplication(projectName, applicationName)
        def jobId = acsClient.undeployApplication(projectName, applicationName).json.jobId
        await("Pods size to be: 2").until {
            k8sApi.getPods().json.items.size() == 2
        }
        def deploymentLog = acsClient.client.getJobLogs(jobId)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert deployments.size() == 1
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == ServiceType.LOAD_BALANCER.value
        assert services[1].spec.ports.first().port == 81
        assert deployments[0].metadata.name == serviceName
        assert deployments[0].metadata.labels."ec-track" == "stable"
        assert deployments[0].spec.replicas == 2
        assert pods.last().metadata.generateName.startsWith('nginx-service-')
        assert pods.first().metadata.namespace == "default"
        assert pods.first().spec.containers.first().name == containerName
        assert pods.first().metadata.labels.get('ec-svc') == serviceName
        assert pods.first().metadata.labels.get('ec-track') == "stable"
        assert pods.first().spec.containers.first().image == "tomaskral/nonroot-nginx:latest"
        assert pods.first().spec.containers.first().ports.first().containerPort == 8080
        assert pods.first().spec.containers.first().volumeMounts.first().mountPath == "/usr/share/nginx/html"
        assert pods.first().spec.containers.first().env.first().value == "8080"
        assert pods.first().spec.containers.first().env.first().name == "NGINX_PORT"
        assert pods.first().status.phase == "Running"
        assert resp.statusCode() == 200
        assert resp.body().asString() == "Hello World!\n"
        assert !deploymentLog.contains(clusterToken)
    }









}
