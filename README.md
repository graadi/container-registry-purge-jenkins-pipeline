## Local Docker registry and Azure ACR purge tool

Jenkins pipeline that will purge the local Docker registry of all untagged and dangling images as well as all the Azure ACR active instances within a subscription. The latter one will require the configuration of Azure credentials object in Jenkins. This is achieve by installing the Azure Credentials plugin.

### Parameters:

| Name                       | Description                                                  |
| -------------------------- | ------------------------------------------------------------ |
| ACR_REGISTRY_PURGE_DRY_RUN | Applicable to the Azure ACR stage. This is going to run the purge command in dry-mode. That means no actual changes will take place. Very useful for testing |
| ACR_PURGE_COMMAND_TIMEOUT  | Applicable to the Azure ACR stage. When cleansing out large ACR registries, it is advisable to add the timeout parameter as the command might need to run longer than the default timeout time of 600 seconds. The value must be an integer and it reflects number of hours. The default value is 1 hour. |
| ACR_PURGE_IMAGE_OLDER_THAN | A Go-style [duration string](https://golang.org/pkg/time/) to indicate a duration beyond which images are deleted. The duration consists of a sequence of one or more decimal numbers, each with a unit suffix. For example, a value of `30` selects all filtered images last modified more than 30 days ago. |
| ACR_MINIMUM_IMAGES_TO_KEEP | Specifies that the latest x number of to-be-deleted tags are retained. The value must be an integer. The default value is 3. |

