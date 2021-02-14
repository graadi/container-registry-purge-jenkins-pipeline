pipelineJob("Docker and Azure ACR Registry Data Purge") {

    description("Keep the local docker engine repository and Azure ACR registry clean of unused, old images.")

    parameters {

        
        booleanParam('RUN_AGGREGATE_REPORT', false, 'When tru, the ACR purge command will run in dry-mode. No changes will be performed.')
        
        stringParam( "ACR_PURGE_COMMAND_TIMEOUT", "3")
        stringParam( "ACR_PURGE_IMAGE_OLDER_THAN", "30")
        
        stringParam( "ACR_MINIMUM_IMAGES_TO_KEEP", "3" )
        stringParam( "EMAIL_RECIPIENTS", "mail@example.com", "Email addressed that will receive the build report.")
    }

    // Define the pipeline script which is located in Git
    definition {
        cpsScm {
            scm {
                git {
                    branch("master")
                    remote {
                        name("origin")
                        url("git@github.com:graadi/container-registry-purge-jenkins-pipeline.git")
                    }
                }
            }
            // The path within source control to the pipeline jobs Jenkins file
            scriptPath("jenkins-pipeline.groovy")
        }
    }
}