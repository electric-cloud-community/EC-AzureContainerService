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
import static com.electriccloud.helpers.enums.ServiceTypes.*

class ServiceTypeDeploymentTests extends AzureTestBase {



    @BeforeClass
    void setUpTests(){
        acsClient.deleteConfiguration(configName)
        acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, LogLevel.DEBUG)
    }

    @BeforeMethod
    void setUpTest(){
        acsClient.createEnvironment(configName, adminAccount, acsClusterName, resourceGroup, 2)
    }

    @AfterMethod
    void tearDownTest(){
        acsClient.undeployService(projectName, serviceName)
        acsClient.client.deleteProject(projectName)
    }

    @AfterClass
    void tearDownTests(){
        acsClient.deleteConfiguration(configName)
    }


    @Test
    @TmsLink("")
    @Story("Deploy service using LoadBalancer service type")
    @Description(" Deploy Project-level Microservice with LoadBalancer service type")
    void deployMicroserviceWithLoadBalancer(){
        acsClient.createService(2, volumes, false, ServiceType.LOAD_BALANCER)
        acsClient.deployService(projectName, serviceName)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def endpoint = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert pods.size() == 2
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == ServiceType.LOAD_BALANCER.value
        assert services[1].spec.ports.first().port == 81
        assert services[1].spec.ports[0].nodePort != null
        assert services[1].status.loadBalancer.ingress[0].ip != null
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
        assert endpoint.statusCode() == 200
        assert endpoint.body().asString() == "Hello World!\n"
    }


    @Test
    @TmsLink("")
    @Story("Deploy service using ClusterIP service type")
    @Description("Deploy Project-level Microservice with ClusterIP service type")
    void deployMicroserviceWithClusterIP(){
        acsClient.createService(2, volumes, false, ServiceType.CLUSTER_IP)
        acsClient.deployService(projectName, serviceName)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        assert services.size() == 2
        assert pods.size() == 2
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == ServiceType.CLUSTER_IP.value
        assert services[1].status.loadBalancer.ingress == null
        assert services[1].spec.ports[0].nodePort == null
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
    }


    @Test
    @TmsLink("")
    @Story("Deploy service using NodePort service type")
    @Description("Deploy Project-level Microservice with NodePort service type")
    void deployMicroserviceWithNodePort(){
        acsClient.createService(2, volumes, false, ServiceType.NODE_PORT)
        acsClient.deployService(projectName, serviceName)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        assert services.size() == 2
        assert pods.size() == 2
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == ServiceType.NODE_PORT.value
        assert services[1].spec.ports.first().port == 81
        assert services[1].status.loadBalancer.ingress == null
        assert services[1].spec.ports[0].nodePort != null
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
    }




}
