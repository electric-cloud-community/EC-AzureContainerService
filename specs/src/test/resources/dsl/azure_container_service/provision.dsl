package dsl.azure_container_service

def names = args.names,
    project = names.projectName,
    environment = names.environmentName,
    cluster = names.cluster

provisionCluster(
        projectName: project,
        environmentName: environment,
        cluster:[
                cluster
        ]
)