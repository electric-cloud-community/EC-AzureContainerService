import java.io.File

procedure 'Deploy Service',
	description: 'Creates or updates the Kubernetes service and the Deployment configuration for Pods and ReplicaSets on the Kubernetes cluster running on the Azure Container Service platform.', {

    step 'setup',
      subprocedure: 'flowpdk-setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'none',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes', {

        actualParameter 'dependsOnPlugins', 'EC-Kubernetes'
    }

	step 'createOrUpdateDeployment',
	  command: new File(pluginDir, 'dsl/procedures/deployService/steps/createOrUpdateDeployment.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  resourceName: '$[grabbedResource]',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'

}

