package dsl.azure_container_service


def names = args.names,
        pluginName = 'EC-AzureContainerService',
        username = names.username,
        configName = names.configName,
        clusterName = names.clusterName,
        resourceGroup = names.resourceGroup,
        agentPoolCount = names.agentPoolCount.toString(),
        agentPoolDnsPrefix = names.agentPoolDnsPrefix,
        agentPoolName = names.agentPoolName,
        masterDnsPrefix = names.masterDnsPrefix,
        masterFqdn = names.masterFqdn



project 'acsProj', {

  environment 'acs-environment', {
    description = ''
    environmentEnabled = '1'
    projectName = 'acsProj'
    reservationRequired = '0'
    rollingDeployEnabled = null
    rollingDeployType = null

    cluster 'acs-cluster', {
      environmentName = 'acs-environment'
      pluginKey = pluginName
      pluginProjectName = null
      providerClusterName = null
      providerProjectName = null
      provisionParameter = [
              'adminUsername': username,
              'agentPoolCount': agentPoolCount, // 1 is for nginx, 2 for Motorbike Demo
              'agentPoolDnsPrefix': agentPoolDnsPrefix,
              'agentPoolName': agentPoolName,
              'agentPoolVmsize': 'Standard_D1',
              'masterVmsize': 'Standard_D1',
              'clusterName': clusterName,
              'clusterWaitime': '20',
              'clusterPrepTime': '60',
              'config': configName,
              'masterCount': '1',
              'masterDnsPrefix': masterDnsPrefix,
              'masterFqdn': masterFqdn,
              'masterZone': 'eastus',
              'orchestratorType': 'kubernetes',
              'resourceGroupName': resourceGroup,
      ]
      provisionProcedure = 'Provision Cluster'
    }
  }
}

/*
project 'acsProj', {
  resourceName = null
  workspaceName = null

  environment 'acsEnv', {
    description = ''
    environmentEnabled = '1'
    projectName = 'acsProj'
    reservationRequired = '0'
    rollingDeployEnabled = null
    rollingDeployType = null

    cluster 'qe-acs-cluster', {
      environmentName = 'acsEnv'
      pluginKey = 'EC-AzureContainerService'
      pluginProjectName = null
      providerClusterName = null
      providerProjectName = null
      provisionParameter = [
              'adminUsername': 'ecloudadmin',
              'agentPoolCount': '1', // 1 is for nginx, 2 for Motorbike Demo
              'agentPoolDnsPrefix': 'flowqeagent',
              'agentPoolName': 'agentflowqe',
              'agentPoolVmsize': 'Standard_D1',
              'masterVmsize': 'Standard_D1',
              'clusterName': 'flowqe-test-cluster',
              'clusterWaitime': '20',
              'clusterPrepTime': '60',
              'config': 'acsConf',
              'masterCount': '1',
              'masterDnsPrefix': 'flowqe',
              'masterFqdn': 'masterflowqe',
              'masterZone': 'eastus',
              'orchestratorType': 'kubernetes',
              'resourceGroupName': 'flowqe-test-resource-group',
      ]
      provisionProcedure = 'Provision Cluster'
    }
  }
}
*/
