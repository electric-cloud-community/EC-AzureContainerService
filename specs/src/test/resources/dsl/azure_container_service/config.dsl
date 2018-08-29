package dsl.azure_container_service


def names = args.names,
        configName = names.configName,
        pluginName = 'EC-AzureContainerService',
        logLevel = '1',
        desc = 'EC-AzureContainerService Config',
        publicKey = names.publicKey,
        privateKey = names.privateKey,
        credPrivateKey = names.credPrivateKey,
        credClientId = names.credClientId,
        tenantId = names.tenantId,
        subscriptionId = names.subscriptionId,
        testConnection = names.testConnection.toString()

// Create plugin configuration

def pluginProjectName = getPlugin(pluginName: pluginName).projectName
runProcedure(
        projectName: pluginProjectName,
        procedureName: 'CreateConfiguration',
        actualParameter: [
                config: configName,
                credential: configName,
                desc: desc,
                keypair: 'keypair',
                publicKey: publicKey,
                subscriptionId: subscriptionId,
                tenantId: tenantId,
                testConnection: testConnection,
                logLevel: logLevel
        ],
        credential: [
                [
                        credentialName: configName,
                        userName: credClientId,
                        password: credPrivateKey
                ],[
                        credentialName: 'keypair',
                        userName: credPrivateKey,
                        password: privateKey
                ]
        ]
)
