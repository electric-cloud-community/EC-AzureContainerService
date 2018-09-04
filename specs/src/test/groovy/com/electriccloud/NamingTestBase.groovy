package com.electriccloud

import com.electriccloud.client.api.AzureContainerServiceApi
import com.electriccloud.client.api.KubernetesApi
import com.electriccloud.client.ectool.EctoolApi
import com.electriccloud.client.plugin.AzureContainerServiceClient
import com.electriccloud.client.plugin.KubernetesClient

import java.text.SimpleDateFormat

trait NamingTestBase {

    def configName
    def projectName
    def environmentProjectName
    def environmentName
    def clusterName
    def serviceName
    def applicationName
    def containerName
    def acsClusterName
    def resourceGroup
    def certsPath
    def clusterEndpoint
    def clusterToken

    def pluginName
    def adminAccount
    def pluginVersion
    def pluginLegacyVersion
    def nodeEndpoint

    EctoolApi ectoolApi
    AzureContainerServiceClient acsClient
    KubernetesClient k8sClient
    AzureContainerServiceApi acsApi
    KubernetesApi k8sApi

}
