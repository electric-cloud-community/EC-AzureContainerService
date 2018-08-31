package com.electriccloud.kubernetes
import com.electriccloud.errors.EcException
import com.electriccloud.errors.ErrorCodes

class Credential {
    def efContext
    def secret
    def projectName
    def environmentName
    def clusterName

    private final String TOKEN_PROPERTY = 'ec_token'
    private final String ENDPOINT_PROPERTY = 'ec_endpoint'


    def getToken() {
        def token = efContext.getProperty(clusterName: clusterName,
            projectName: projectName,
            environmentName: environmentName,
            propertyName: TOKEN_PROPERTY)?.value

        if (!token) {
            throw EcException
                .code(ErrorCodes.ScriptError)
                .message("No token found in the cluster properties")
                .cause(e)
                .location(this.class.getCanonicalName())
                .build()
        }
//        TODO decrypt
        return token
    }

    def getEndpoint() {
        def endpoint = efContext.getProperty(clusterName: clusterName,
            projectName: projectName,
            environmentName: environmentName, propertyName: ENDPOINT_PROPERTY)?.value
        if (!endpoint) {
            throw EcException
                .code(ErrorCodes.ScriptError)
                .message("No endpoint found in the cluster properties")
                .cause(e)
                .location(this.class.getCanonicalName())
                .build()
        }
        return endpoint
    }

    def getVersion() {
        return '1.6'
    }
}
