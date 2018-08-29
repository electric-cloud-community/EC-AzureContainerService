package dsl.azure_container_service

def names = args.names,
        envProjectName = names.envProjectName,
        environmentName = names.environmentName,
        clusterName = names.clusterName,
        namespace = names.namespace,
        projectName = names.projectName,
        azClusterName = names.azClusterName,
        azResourceGroupName = names.azResourceGroupName,
        adminUsername = "ecloudadmin",
        agentPoolCount = names.agentPoolCount,
        agentPoolDnsPrefix = "flowqeagent",
        agentPoolName = "agentflowqe",
        agentPoolVmsize = "Standard_D1",
        clientId = names.clientId,
        azureSecretKey = names.azureSecretKey,
        clusterWaitTime = "20",
        masterCount = "1",
        masterDnsPrefix = "flowqe",
        masterFqdn = "masterflowqe",
        masterZone = "eastus",
        privateKey = names.privateKey,
        publicKey = names.publicKey,
        subscriptionId = names.subscriptionId,
        tenantId = names.tenantId,
        applicationScoped = names.applicationScoped,
        applicationName = names.applicationName

// Create plugin configuration

def pluginProjectName = getPlugin(pluginName: 'EC-AzureContainerService').projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'Discover',
        actualParameter: [
                envProjectName: envProjectName,
                envName: environmentName,
                clusterName: clusterName,
                namespace: namespace,
                projName: projectName,
                ecp_azure_adminUsername: adminUsername,
                ecp_azure_agentPoolCount	: agentPoolCount,
                ecp_azure_agentPoolDnsPrefix: agentPoolDnsPrefix,
                ecp_azure_agentPoolName	 : agentPoolName,
                ecp_azure_agentPoolVmsize	: agentPoolVmsize,
                ecp_azure_applicationName	 : applicationName,
                ecp_azure_applicationScoped	: applicationScoped,
                ecp_azure_azClusterName	 	: azClusterName,
                ecp_azure_azResourceGroupName: azResourceGroupName,
                ecp_azure_azureSecretKey: azureSecretKey,
                ecp_azure_clientId: clientId,
                ecp_azure_clusterWaitTime: clusterWaitTime,
                ecp_azure_masterCount: masterCount,
                ecp_azure_masterDnsPrefix: masterDnsPrefix,
                ecp_azure_masterFqdn: masterFqdn,
                ecp_azure_masterZone: masterZone,
                ecp_azure_privateKey: privateKey,
                ecp_azure_publicKey: publicKey,
                ecp_azure_subscriptionId: subscriptionId,
                ecp_azure_tenantId: tenantId
        ]
)
