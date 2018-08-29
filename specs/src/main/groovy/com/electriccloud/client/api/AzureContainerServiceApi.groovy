package com.electriccloud.client.api

import com.microsoft.azure.AzureEnvironment
import com.microsoft.azure.credentials.ApplicationTokenCredentials
import com.microsoft.azure.management.Azure
import com.electriccloud.client.HttpClient

class AzureContainerServiceApi extends HttpClient {


    ApplicationTokenCredentials credentials
    Azure azure


    AzureContainerServiceApi(authJson, subscriptionId){
        def credFile = new File(authJson)
        this.azure = Azure.authenticate(credFile).withSubscription(subscriptionId)
    }


    AzureContainerServiceApi(client, tenant, key, subscriptionId){
        this.credentials = new ApplicationTokenCredentials(client, tenant, key, AzureEnvironment.AZURE)
        this.azure = Azure.authenticate(credentials).withSubscription(subscriptionId)
    }





}
