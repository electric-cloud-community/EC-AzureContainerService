package com.electriccloud.client.api

import com.electriccloud.client.HttpClient
import groovy.json.JsonBuilder

import static groovyx.net.http.Method.DELETE
import static groovyx.net.http.Method.GET
import static com.electriccloud.helpers.config.ConfigHelper.message

class KubernetesApi extends HttpClient {


    def baseUri
    def token
    def defaultHeaders() { ["Authorization": "Bearer ${getToken()}", Accept: "application/json"] }
    def json = new JsonBuilder()


    KubernetesApi() {
        this.baseUri = pluginsConf.containerPlugins.plugins.kubernetes.endpoint
        this.token = pluginsConf.containerPlugins.plugins.kubernetes.token
        log.info("Connecting cluster endpoint ${this.baseUri}")
        log.info("Cluster access token: ${this.token}")
    }

    KubernetesApi(baseUri, token) {
        this.baseUri = baseUri
        this.token = token
        log.info("Connecting cluster endpoint ${this.baseUri}")
        log.info("Cluster access token: ${this.token}")
    }

    def deleteService(name) {
        message("deleting ${name} service")
        def uri = "api/v1/namespaces/default/services/${name}"
        def resp = request(baseUri, uri, DELETE, null, defaultHeaders(), null, false)
        log.info(resp.json)
    }

    def getService(name) {
        message("getting ${name} service")
        def uri = "api/v1/namespaces/default/services/${name}"
        request(baseUri, uri, GET, null, defaultHeaders(), null, false)
    }

    def getDeployment(name) {
        message("getting ${name} deployment")
        def uri = "apis/apps/v1beta1/namespaces/default/deployments/${name}"
        request(baseUri, uri, GET, null, defaultHeaders(), null, false)
    }

    def getServices() {
        message("getting services")
        def uri = "api/v1/namespaces/default/services"
        def resp = request(baseUri, uri, GET, null, defaultHeaders(), null, false)
        log.info("Got services:")
        resp.json.items.forEach { log.info(" ${it.metadata.name} | type: ${it.spec.type} | clusterIP: ${it.spec.clusterIP} | externalIP: ${it.status.loadBalancer} | exposePort: ${it.spec.ports.port}") }
        resp
    }

    def getDeployments() {
        message("getting deployments")
        def uri = "apis/apps/v1beta1/namespaces/default/deployments"
        def resp = request(baseUri, uri, GET, null, defaultHeaders(), null, false)
        log.info("Got deployments:")
        resp.json.items.forEach { log.info(" ${it.metadata.name} | pods: ${it.spec.replicas} | Available: ${it.status.availableReplicas}") }
        resp
    }

    def getPods() {
        message("getting pods")
        def uri = "api/v1/namespaces/default/pods"
        def resp = request(baseUri, uri, GET, null, defaultHeaders(), null, false)
        log.info("Got pods:")
        resp.json.items.forEach { log.info("Pod Name: ${it.metadata.name} | Pod IP: ${it.status.podIP} | Host IP: ${it.status.hostIP}") }
        resp
    }

}
