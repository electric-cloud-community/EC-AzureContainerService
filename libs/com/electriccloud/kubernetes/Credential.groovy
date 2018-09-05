package com.electriccloud.kubernetes

import com.electriccloud.errors.EcException
import com.electriccloud.errors.ErrorCodes
import com.electriccloud.kubernetes.ClusterInfoCrypter

class Credential {
    def efContext
    def userName
    def password
    def projectName
    def environmentName
    def clusterName

    private final String TOKEN_PROPERTY = 'ec_clusterAccessTokenEncrypted'
    private final String ENDPOINT_PROPERTY = 'ec_clusterEndPoint'


    def getToken() {
        def accessTokenEncrypted = efContext.getProperty(clusterName: clusterName,
            projectName: projectName,
            environmentName: environmentName,
            propertyName: TOKEN_PROPERTY)?.value

        if (!accessTokenEncrypted) {
            throw EcException
                .code(ErrorCodes.ScriptError)
                .message("No token found in the cluster properties")
                .cause(e)
                .location(this.class.getCanonicalName())
                .build()
        }

        ClusterInfoCrypter clusterInfoCrypter = new ClusterInfoCrypter(
                password,
                userName
        )

        String accessTokenDecrypted = clusterInfoCrypter.decrypt(accessTokenEncrypted)

        return accessTokenDecrypted
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
