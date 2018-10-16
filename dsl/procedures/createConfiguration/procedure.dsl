import java.io.File

procedure 'CreateConfiguration',
        description: 'Creates a configuration for Azure Container Service', {

    step 'setup',
      subproject: '/plugins/EC-Kubernetes/project',
      subprocedure: 'Setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'none',
      postProcessor: 'postp',
      releaseMode: 'none',
      condition: '$[testConnection]',
      timeLimitUnits: 'minutes', {
        actualParameter 'additionalPluginName', '@PLUGIN_KEY@'
    }

    step 'testConnection',
            command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/testConnection.groovy').text,
            errorHandling: 'abortProcedure',
            exclusiveMode: 'none',
            postProcessor: 'postp',
            releaseMode: 'none',
            shell: 'ec-groovy',
            condition: '$[testConnection]',
            resourceName: '$[grabbedResource]',
            timeLimitUnits: 'minutes'

    step 'createConfiguration',
            command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/createConfiguration.pl').text,
            errorHandling: 'abortProcedure',
            exclusiveMode: 'none',
            postProcessor: 'postp',
            releaseMode: 'none',
            shell: 'ec-perl',
            timeLimitUnits: 'minutes'

    step 'createAndAttachCredential',
        command: new File(pluginDir, 'dsl/procedures/createConfiguration/steps/createAndAttachCredential.pl').text,
        errorHandling: 'failProcedure',
        exclusiveMode: 'none',
        releaseMode: 'none',
        shell: 'ec-perl',
        timeLimitUnits: 'minutes'

}
