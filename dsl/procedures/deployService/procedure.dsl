import java.io.File

procedure 'Deploy Service',
	description: 'Creates or updates the Kubernetes service and the Deployment configuration for Pods and ReplicaSets on the Kubernetes cluster running on the Azure Container Service platform.', {

    step 'setup',
      subproject: '/plugins/EC-Kubernetes/project',
      subprocedure: 'Setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'none',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes', {

        actualParameter 'additionalArtifactVersion', 'com.electriccloud:EC-AzureContainerService-Grapes:1.0.0'
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
  
