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

    private final String ENDPOINT_PROPERTY = 'ec_clusterEndPoint'
    private final String TOKEN_PROPERTY = 'ec_clusterAccessTokenEncrypted'
    private final String TOKEN_IV_PROPERTY = 'ec_clusterAccessTokenEncryptionIv'


    def getToken() {
        def accessTokenEncrypted = efContext.getProperty(clusterName: clusterName,
                projectName: projectName,
                environmentName: environmentName,
                propertyName: TOKEN_PROPERTY)?.value

        def accessTokenEncryptionIv = efContext.getProperty(clusterName: clusterName,
                projectName: projectName,
                environmentName: environmentName,
                propertyName: TOKEN_IV_PROPERTY)?.value

        if (!accessTokenEncrypted) {
            throw EcException
                    .code(ErrorCodes.ScriptError)
                    .message("No token found in the cluster properties")
                    .location(this.class.getCanonicalName())
                    .build()
        }

        if (!accessTokenEncryptionIv) {
            throw EcException
                    .code(ErrorCodes.ScriptError)
                    .message("No token iv found in the cluster properties")
                    .location(this.class.getCanonicalName())
                    .build()
        }

        ClusterInfoCrypter clusterInfoCrypter = new ClusterInfoCrypter();
        String accessTokenDecrypted = clusterInfoCrypter.decrypt(accessTokenEncrypted, password, userName, accessTokenEncryptionIv);

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
                    .location(this.class.getCanonicalName())
                    .build()
        }
        return endpoint
    }

    def getVersion() {
        return '1.6'
    }
}
