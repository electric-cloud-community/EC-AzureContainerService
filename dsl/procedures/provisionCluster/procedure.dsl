import java.io.File

procedure 'Provision Cluster', 
	description: 'Provisions a Azure Container Service cluste. Pods, services, and replication controllers all run on top of a cluster.', {

    step 'setup',
      subproject: '/plugins/EC-Kubernetes/project',
      subprocedure: 'Setup',
      command: null,
      errorHandling: 'failProcedure',
      exclusiveMode: 'none',
      postProcessor: 'postp',
      releaseMode: 'none',
      timeLimitUnits: 'minutes', {

        actualParameter 'additionalArtifactVersion', 'com.electriccloud:EC-AzureContainerService-Grapes:1.0.1'
    }

	step 'provisionCluster', 
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/provisionCluster.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'
	  
}
  
