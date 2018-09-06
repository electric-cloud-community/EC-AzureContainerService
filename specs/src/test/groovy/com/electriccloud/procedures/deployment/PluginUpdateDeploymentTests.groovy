package com.electriccloud.procedures.deployment

import com.electriccloud.procedures.AzureTestBase
import io.qameta.allure.Story
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.electriccloud.helpers.enums.LogLevels.*
import static com.electriccloud.helpers.enums.ServiceTypes.*

class PluginUpdateDeploymentTests extends AzureTestBase {

    @BeforeClass
    void setUpTests(){
        ectoolApi.deletePlugin pluginName, pluginVersion
        def legacyPlugin = ectoolApi.installPlugin("${pluginName}-legacy").plugin
        ectoolApi.promotePlugin(legacyPlugin.projectName)
        acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, LogLevel.DEBUG)
        acsClient.createEnvironment(configName, adminAccount, acsClusterName, resourceGroup, 2)
    }

    @AfterClass
    void tearDownTests(){
        ectoolApi.deletePlugin pluginName, pluginLegacyVersion
        def latestPlugin = ectoolApi.installPlugin(pluginName).plugin
        ectoolApi.promotePlugin(latestPlugin.pluginName)
        acsClient.deleteConfiguration(configName)
        acsClient.client.deleteProject(projectName)
    }


    @AfterMethod
    void tearDownTest(){
        k8sApi.deleteService(serviceName)
        k8sApi.deleteDeployments()
    }




    @Test(groups = 'pluginUpdate')
    @Story('Deploy service after Plugin version update')
    void pluginUpdateDeployment(){
        acsClient.createService(2, volumes, false, ServiceType.LOAD_BALANCER)
        acsClient.deployService(projectName, serviceName)
        def plugin = ectoolApi.installPlugin(pluginName).plugin
        ectoolApi.promotePlugin(plugin.projectName)
        acsClient.deleteConfiguration(configName)
        acsClient.createConfiguration(configName, publicKey, privateKey, credPrivateKey, credClientId, tenantId, subscriptionId, true, LogLevel.DEBUG)
        acsClient.updateService(3, volumes, false, ServiceType.LOAD_BALANCER)
        acsClient.deployService(projectName, serviceName)
        def deployments = k8sApi.getDeployments().json.items
        def services = k8sApi.getServices().json.items
        def pods = k8sApi.getPods().json.items
        def resp = req.get("http://${services[1].status.loadBalancer.ingress[0].ip}:81")
        assert services.size() == 2
        assert pods.size() == 3
        assert services[1].metadata.name == serviceName
        assert services[1].metadata.namespace == "default"
        assert services[1].spec.type == ServiceTypes.ServiceType.LOAD_BALANCER.value
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
        assert resp.body().asString().contains("Welcome to nginx!")
    }




}
