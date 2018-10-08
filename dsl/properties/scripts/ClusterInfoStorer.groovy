class ClusterInfoStorer {
    public final static String ENDPOINT_PROPERTY = 'ec_clusterEndPoint'
    public final static String TOKEN_PROPERTY = 'ec_clusterAccessTokenEncrypted'
    public final static String TOKEN_IV_PROPERTY = 'ec_clusterAccessTokenEncryptionIv'

    public static void storeClusterInfo(
            String clusterEndPoint,
            String accessToken,
            def pluginConfig,
            EFClient efClient,
            ClusterInfoCrypter clusterInfoCrypter,
            String clusterOrEnvProjectName,
            String environmentName,
            String clusterName
    ) {
        // store gathered information within cluster properties
        ClusterInfoCrypter.EncryptionResult encryptionResult = clusterInfoCrypter.encrypt(
                accessToken,
                pluginConfig.credential.password,
                pluginConfig.credential.userName
        )
        String accessTokenEncrypted = encryptionResult.getEncryptedSensitiveData()
        String iv = encryptionResult.getIv()

        String clusterEndpointPropertyPath = "/projects/$clusterOrEnvProjectName/environments/$environmentName/clusters/$clusterName/$ENDPOINT_PROPERTY"
        String clusterAccessTokenEncryptedPropertyPath = "/projects/$clusterOrEnvProjectName/environments/$environmentName/clusters/$clusterName/$TOKEN_PROPERTY"
        String clusterAccessTokenEncryptionIvPropertyPath = "/projects/$clusterOrEnvProjectName/environments/$environmentName/clusters/$clusterName/$TOKEN_IV_PROPERTY"

        createOrUpdateProperty(efClient, clusterEndpointPropertyPath, clusterEndPoint)
        createOrUpdateProperty(efClient, clusterAccessTokenEncryptedPropertyPath, accessTokenEncrypted)
        createOrUpdateProperty(efClient, clusterAccessTokenEncryptionIvPropertyPath, iv)
    }

    public static void createOrUpdateProperty(EFClient efClient, String propertyPath, String value) {
        if (efClient.getEFProperty(propertyPath, true).data) {
            efClient.setEFProperty(propertyPath, value)
        } else {
            efClient.createProperty(propertyPath, value)
        }
    }
}