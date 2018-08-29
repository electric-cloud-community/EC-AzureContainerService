package com.electriccloud

import com.electriccloud.client.api.AzureContainerServiceApi
import com.electriccloud.client.api.KubernetesApi
import com.electriccloud.client.ectool.EctoolApi
import com.electriccloud.client.plugin.AzureContainerServiceClient
import com.electriccloud.client.plugin.KubernetesClient
import org.assertj.core.internal.bytebuddy.utility.RandomString

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

    // Naming Helpers

    String unique(objectName) {
//        new SimpleDateFormat("${objectName}yyyyMMddHHmmssSSS".toString()).format(new Date())
        objectName + (new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()))
    }

    String characters(objectName, num) {
        num = num as Integer
        def _num
        if(num != 0) {
            _num = new RandomString(num).nextString()
            return "${objectName}${_num}".toString()
        } else {
            return ''
        }
    }

    String characters(num) {
        characters('', num)
    }

}
