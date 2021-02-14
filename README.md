## Local Docker registry and Azure ACR purge tool

Jenkins pipeline that will purge the local Docker registry of all untagged and dangling images as well as all the Azure ACR active instances within a subscription. The latter one will require the configuration of Azure credentials object in Jenkins. This is achieve by installing the Azure Credentials plugin.

### Using AzureCredentials

The below paragraph has been taken as it is from the [plugin documentation website](https://plugins.jenkins.io/azure-credentials/).

Custom binding for AzureCredentials to support reading Azure service principal in both freestyle and pipeline using Credentials Binding plugin.

In freestyle jobs, click `Use secret text(s) or file(s)` in the `Build Environment` in the configuration page and add a `Microsoft Azure Service Principal` item, which allows you add credential bindings where the *Variable* value will be used as the name of the environment variable that your build can use to access the value of the credential. With the default variable names you can reference the service principal as the following:

```groovy
echo "My client id is $AZURE_CLIENT_ID"
echo "My client secret is $AZURE_CLIENT_SECRET"
echo "My tenant id is $AZURE_TENANT_ID"
echo "My subscription id is $AZURE_SUBSCRIPTION_ID"
```

In pipelines, there're two ways to construct this binding:

1. With defaults, which will read specified service principal into four predefined environment variables: `AZURE_SUBSCRIPTION_ID`, `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET`, `AZURE_TENANT_ID`. Sample pipeline code:

   ```groovy
   withCredentials([azureServicePrincipal('credentials_id')]) {
       sh 'az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $AZURE_TENANT_ID'
   }
   ```

2. With custom name, where you can control the names of the variables. Sample pipeline code:

   ```groovy
   withCredentials([azureServicePrincipal(credentialsId: 'credentials_id',
                                       subscriptionIdVariable: 'SUBS_ID',
                                       clientIdVariable: 'CLIENT_ID',
                                       clientSecretVariable: 'CLIENT_SECRET',
                                       tenantIdVariable: 'TENANT_ID')]) {
       sh 'az login --service-principal -u $CLIENT_ID -p $CLIENT_SECRET -t $TENANT_ID'
   }
   ```

### Parameters:

| Name                       | Description                                                  |
| -------------------------- | ------------------------------------------------------------ |
| ACR_REGISTRY_PURGE_DRY_RUN | Applicable to the Azure ACR stage. This is going to run the purge command in dry-mode. That means no actual changes will take place. Very useful for testing |
| ACR_PURGE_COMMAND_TIMEOUT  | Applicable to the Azure ACR stage. When cleansing out large ACR registries, it is advisable to add the timeout parameter as the command might need to run longer than the default timeout time of 600 seconds. The value must be an integer and it reflects number of hours. The default value is 1 hour. |
| ACR_PURGE_IMAGE_OLDER_THAN | A Go-style [duration string](https://golang.org/pkg/time/) to indicate a duration beyond which images are deleted. The duration consists of a sequence of one or more decimal numbers, each with a unit suffix. For example, a value of `30` selects all filtered images last modified more than 30 days ago. |
| ACR_MINIMUM_IMAGES_TO_KEEP | Specifies that the latest x number of to-be-deleted tags are retained. The value must be an integer. The default value is 3. |

### Stages:

<img src="https://github.com/graadi/container-registry-purge-jenkins-pipeline/blob/main/img/pipline.png" alt="Pipeline Stages"/>

### Local Docker registry

The idea behing assumes that there is a Jenkins server, with Docker installed and the engine is orchestrated by the running declarative pipeline. The need of a purge came as a need to keep the disk space within a certain boundary as the registry storage tends to become a very disk space consuming component.