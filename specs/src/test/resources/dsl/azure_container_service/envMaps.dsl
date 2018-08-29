package dsl.azure_container_service

def names = args.names,
        projectName = names.projectName,
        serviceName = names.serviceName

getEnvironmentMaps(
        projectName: projectName,
        serviceName: serviceName
)