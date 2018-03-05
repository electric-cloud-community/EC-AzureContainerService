import java.io.File

procedure 'Undeploy Service',
	description: 'Undeploys a previously deployed service on the Azure Container Service', {

	step 'setup',
      subproject: '/plugins/EC-Kubernetes/project',
      subprocedure: 'Setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'call',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes'

	step 'undeployService',
	  command: new File(pluginDir, 'dsl/procedures/undeployService/steps/undeployService.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'

}