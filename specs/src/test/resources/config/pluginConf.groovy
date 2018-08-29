package config


containerPlugins {

    author   = 'Electric Cloud'
    category = 'Container Management'
    version  = '(promoted)'
    actions  = [ 'Demote', 'Configure', 'Help', 'Uninstall' ]

    plugins {

        azureContainerService {
            name                 = 'EC-AzureContainerService'
            buildVersion         = '1.1.0.59'
            legacyVersion        = '1.0.3.48'
            description          = 'Integrates with Azure Container Service to run Docker containers on the Azure Cloud Platform.'
            certsDir             = 'src/test/resources/certs'
            admin                = 'ecloudadmin'
            subscriptionId       = '20ab6f85-801d-4c3a-a0d4-11da3631d29c'
            tenantId             = '7b4b14e5-87f8-4f09-9c83-f91d9b8a49fd'
            credClientId         = 'a90f65da-47a1-4547-aa41-6ca06cf2551a'
            credPrivateKey       = 'SzqYyyQU1a+hJIYxJxWAAo15Br0oMTXUzvHd6qP/1qM='
            // Get cluster credentials (server address and token): https://kubernetes.io/docs/tasks/access-application-cluster/access-cluster/
            clusterEndpoint      = 'https://flowqe.eastus.cloudapp.azure.com'
            nodeEndpoint         = 'http://10.240.0.5'
            clusterToken         = 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImRlZmF1bHQtdG9rZW4td2I4dGQiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGVmYXVsdCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI3MWZmM2MxLTkzZDctMTFlOC1hYjhlLTAwMGQzYTE3OTZkZSIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OmRlZmF1bHQifQ.k1BnbsWXJQewGzOvSEtAcyXTlVg2pbi7BuF47JTro0oIUGzAHhMEOVrDaHhlLQ_MMVv33LCBYoIHpyXrE6jSRXg_MeHtx1tMCJxYa5Bw1c0pz-vVYwsjtzLHk9nK5hoHNon9QtYSzvu0rjEux3FCANmhWtl-aCTlM6wThVvJQtREp2ZeqrnADtIziiUDMpIxqBdJIecC3G4YcsAlVmFmnNEbMC_udLEDIWIZ6Ngq4I3gik-xXejlEHoD3__yStmZFzs5xWuatGaqX8z_7Z0WbCwYh3M2jyRGyuZs3sCamH_f0H-6RoodGeAO1WmRx0B9hSnPXlnXYbqqooUf0HOzcQ'
        }

    }

}

pathToPlugins = '/ec-plugins'
fileExtention = '.jar'
