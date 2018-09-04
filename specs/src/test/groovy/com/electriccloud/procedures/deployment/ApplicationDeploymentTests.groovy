package com.electriccloud.procedures.deployment


import com.electriccloud.procedures.AzureTestBase
import io.qameta.allure.Description
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static com.electriccloud.helpers.enums.LogLevels.*
import static org.awaitility.Awaitility.await
import static com.electriccloud.helpers.enums.ServiceTypes.*

class ApplicationDeploymentTests extends AzureTestBase {


    @BeforeClass
    void setUpTests(){
        acsClient.deleteConfiguration(configName)
        acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, LogLevel.DEBUG)
    }

    @BeforeMethod
    void setUpTest(){
        acsClient.createEnvironment(configName, adminAccount, acsClusterName, resourceGroup, 2)
        acsClient.createApplication(2, volumes, false, ServiceType.LOAD_BALANCER)
    }

    @AfterClass
    void tearDownTests(){
        acsClient.deleteConfiguration(configName)
    }

    @AfterMethod
    void tearDownTest(){
        acsClient.undeployApplication(projectName, applicationName)
        acsClient.client.deleteProject(projectName)
    }



    @Test
    @TmsLink("")
    @Story("Deploy Microservcice")
    @Description("Deploy Application-Level Microservice")
    void deployApplicationLevelMicroservice(){
        acsClient.deployApplication(projectName, applicationName)
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
    }




    @Test
    @TmsLink("")
    @Story('Update Microservice')
    @Description("Update Application-level Microservice with the same data")
    void updateApplicationLevelMicroserviceWithSameData(){
        acsClient.deployApplication(projectName, applicationName)
        acsClient.createApplication(2, volumes, false, ServiceType.LOAD_BALANCER)
        acsClient.deployApplication(projectName, applicationName)
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
    }




    @Test
    @TmsLink("")
    @Story('Update Microservice')
    @Description("Update Application-level Microservice")
    void updateApplicationLevelMicroservice(){
        acsClient.deployApplication(projectName, applicationName)
        acsClient.createApplication(3, volumes, false, ServiceType.LOAD_BALANCER)
        acsClient.deployApplication(projectName, applicationName)
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
    }



    @Test
    @TmsLink("")
    @Story('Canary deploy of Microservice')
    @Description("Canary Deploy for Application-level Microservice")
    void preformCanaryDeploymentForApplicationLevelMicroservice() {
        acsClient.deployApplication(projectName, applicationName)
        acsClient.createApplication(2, volumes, true, ServiceType.LOAD_BALANCER)
        acsClient.deployApplication(projectName, applicationName)
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
    }



    @Test
    @TmsLink("")
    @Story('Undeploy Microservice')
    @Description("Undeploy Application-level Microservice")
    void undeployApplicationLevelMicroservice() {
        acsClient.deployApplication(projectName, applicationName)
        acsClient.undeployApplication(projectName, applicationName)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        await("Wait for Deployment size to be: \'0\'").until {
            pods.size() == 0
            deployments.size() == 0
        }
        assert deployments.size() == 0
        assert services.size() == 1
        assert pods.size() == 0
        assert services[0].metadata.name == "kubernetes"
    }



    @Test
    @TmsLink("")
    @Story('Undeploy Microservice after Canary deployment')
    @Description("Undeploy Application-level Microservice after Canary Deploy")
    void undeployApplicationLevelMicroserviceAfterCanaryDeployment() {
        acsClient.deployApplication(projectName, applicationName)
        acsClient.updateApplication(2, volumes, true, ServiceType.LOAD_BALANCER)
        acsClient.deployApplication(projectName, applicationName)
        acsClient.undeployApplication(projectName, applicationName)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        await("Wait for Deployment size to be: \'1\'").until {
            pods.size() == 2
            deployments.size() == 1
        }
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
    }









}
