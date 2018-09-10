package dsl.azure_container_service

def names = args.names,
        cluster = names.cluster,
        envProject = names.envProject,
        environment = names.environment



runProcedure(
        projectName: '/plugins/EC-AzureContainerService/project',
        procedureName: 'Gather Cluster Info',
        actualParameter: [
                clusterName: cluster,
                clusterOrEnvProjectName: envProject,
                environmentName: environment
        ]

)