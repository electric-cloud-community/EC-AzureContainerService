import java.io.File

procedure 'Gather Cluster Info',
	description: 'Retrieves token and endpoint for Smart Map.', {

    step 'setup',
      subproject: '/plugins/EC-Kubernetes/project',
      subprocedure: 'Setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'none',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes', {

        actualParameter 'additionalPluginName', '@PLUGIN_KEY@'
    }

	step 'gatherClusterInfo',
	  command: new File(pluginDir, 'dsl/procedures/gatherClusterInfo/steps/gatherClusterInfo.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  resourceName: '$[grabbedResource]',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'

}

