import java.io.File

procedure 'Provision Cluster',
	description: 'Provisions a Azure Container Service cluste. Pods, services, and replication controllers all run on top of a cluster.', {

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

	step 'provisionCluster',
	  command: new File(pluginDir, 'dsl/procedures/provisionCluster/steps/provisionCluster.groovy').text,
	  errorHandling: 'failProcedure',
	  exclusiveMode: 'none',
	  postProcessor: 'postp',
	  releaseMode: 'none',
	  resourceName: '$[grabbedResource]',
	  shell: 'ec-groovy',
	  timeLimitUnits: 'minutes'

}

