package dsl.azure_container_service

def names = args.names,
    project = names.project,
    appName = names.appName
    tierMapName = names.tierMapName

runProcess(
        projectName: project,
        applicationName: appName,
        processName: 'Deploy',
        tierMapName: tierMapName,
        rollingDeployEnabled: false
)