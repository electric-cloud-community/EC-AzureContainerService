package dsl.azure_container_service

def names = args.names,
    configName = names.configName,
    cluster = names.cluster,
    resourceGroup = names.resourceGroup
    projectNamespace = names.projectNamespace

runProcedure(
        projectName: '/plugins/EC-AzureContainerService/project',
        procedureName: "Cleanup Cluster - Experimental",
        actualParameter: [
                config: configName,
                clusterName: cluster,
                resourceGroupName: resourceGroup,
                namespace: projectNamespace,
                adminUsername: 'ecloudadmin'
        ]
)