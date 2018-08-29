package dsl.azure_container_service

def names = args.names,
    pluginName = 'EC-AzureContainerService',
    configName = names.configName

def pluginProjectName = getPlugin(pluginName: pluginName).projectName

runProcedure(
        projectName: "/plugins/${pluginProjectName}/project",
        procedureName: "DeleteConfiguration",
        actualParameter: [
                config: configName
        ]
)