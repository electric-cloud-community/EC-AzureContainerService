package dsl.azure_container_service

def names = args.names,
    replicas = names.replicas.toString(),
    sourceVolume = names.sourceVolume,
    targetVolume = names.targetVolume,
    isCanary = names.isCanary.toString(),
    servType = names.servType


project 'acsProj', {

    service 'nginx-service', {
        defaultCapacity = replicas.toString()
        maxCapacity = (replicas + 1).toString()
        minCapacity = '1'
        volume = sourceVolume

        container 'nginx-container', {
            description = ''
            cpuCount = '0.1'
            cpuLimit = '2'
            imageName = 'tomaskral/nonroot-nginx'
            imageVersion = 'latest'
            memoryLimit = '256'
            memorySize = '128'
            registryUri = null
            serviceName = 'nginx-service'
            volumeMount = targetVolume
            environmentVariable 'NGINX_PORT', {
                type = 'string'
                value = '8080'
            }

            port 'http', {
                containerName = 'nginx-container'
                containerPort = '8080'
                projectName = 'acsProj'
                serviceName = 'nginx-service'
            }
        }

        environmentMap 'nginxMappings', {
            environmentName = 'acs-environment'
            environmentProjectName = 'acsProj'
            projectName = 'acsProj'
            serviceName = 'nginx-service'

            serviceClusterMapping 'acsClusterMappings', {
                actualParameter = [
                        'canaryDeployment': isCanary,
                        'numberOfCanaryReplicas': replicas.toString(),
                        'createOrUpdateResource': '0',
                        'deploymentTimeoutInSec': '120',
                        'namespace': 'default',
                        'requestType': 'create',
                        'sessionAffinity': 'None',
                        'serviceType': servType
                ]
                clusterName = 'acs-cluster'
                environmentMapName = 'nginxMappings'
                serviceName = 'nginx-service'

                serviceMapDetail 'nginx-container', {
                    serviceMapDetailName = '2d3db366-545d-11e8-8b54-00155d01ef00'
                    serviceClusterMappingName = 'acsClusterMappings'
                }
            }
        }

        port '_servicehttpnginx-container01525961833676', {
            applicationName = null
            listenerPort = '81'
            projectName = 'acsProj'
            serviceName = 'nginx-service'
            subcontainer = 'nginx-container'
            subport = 'http'
        }

        process 'Deploy', {
            applicationName = null
            processType = 'DEPLOY'
            serviceName = 'nginx-service'
            smartUndeployEnabled = null
            timeLimitUnits = null
            workingDirectory = null
            workspaceName = null

            formalParameter 'ec_enforceDependencies', defaultValue: '0', {
                expansionDeferred = '1'
                label = null
                orderIndex = null
                required = '0'
                type = 'checkbox'
            }

            processStep 'deploy', {
                afterLastRetry = null
                alwaysRun = '0'
                applicationTierName = null
                componentRollback = null
                dependencyJoinType = null
                errorHandling = 'failProcedure'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                processStepType = 'service'
                retryCount = null
                retryInterval = null
                retryType = null
                rollbackSnapshot = null
                rollbackType = null
                rollbackUndeployProcess = null
                skipRollbackIfUndeployFails = null
                smartRollback = null
                subcomponent = null
                subcomponentApplicationName = null
                subcomponentProcess = null
                subprocedure = null
                subproject = null
                subservice = null
                subserviceProcess = null
                timeLimitUnits = null
                useUtilityResource = '0'
                utilityResourceName = null
                workingDirectory = null
                workspaceName = null
            }
            property 'ec_deploy', {
                ec_notifierStatus = '0'
            }
        }

        process 'Undeploy', {
            applicationName = null
            processType = 'UNDEPLOY'
            serviceName = 'nginx-service'
            smartUndeployEnabled = null
            timeLimitUnits = null
            workingDirectory = null
            workspaceName = null

            formalParameter 'ec_enforceDependencies', defaultValue: '0', {
                expansionDeferred = '1'
                label = null
                orderIndex = null
                required = '0'
                type = 'checkbox'
            }

            processStep 'Undeploy', {
                afterLastRetry = null
                alwaysRun = '0'
                applicationTierName = null
                componentRollback = null
                dependencyJoinType = 'and'
                errorHandling = 'abortJob'
                instruction = null
                notificationEnabled = null
                notificationTemplate = null
                processStepType = 'service'
                retryCount = null
                retryInterval = null
                retryType = null
                rollbackSnapshot = null
                rollbackType = null
                rollbackUndeployProcess = null
                skipRollbackIfUndeployFails = null
                smartRollback = null
                subcomponent = null
                subcomponentApplicationName = null
                subcomponentProcess = null
                subprocedure = null
                subproject = null
                subservice = 'nginx-service'
                subserviceProcess = null
                timeLimitUnits = null
                useUtilityResource = '0'
                utilityResourceName = null
                workingDirectory = null
                workspaceName = null
                property 'ec_deploy', {
                    ec_notifierStatus = '0'
                }
            }
            property 'ec_deploy', {
                ec_notifierStatus = '0'
            }
        }
        property 'ec_deploy', {
            ec_notifierStatus = '0'
        }
    }
}

