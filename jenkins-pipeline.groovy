/*

Jenkins declarative pipeline for:
- Docker repository purge
- Azure ACR Repositories Purge

Authors: Adrian Gramada
Date:January 2021

*/

def dryrunJobParameter
def dryrun
def timeout
def olderThan
def minimumToKeep

pipeline {

    agent any

    stages {

        stage('Pre Cleanup') {

            steps {
                
                print 'Pre Cleanup'
                deleteDir()
            }
        }

        stage('Read Parameters') {

            steps {

                script {

                    try {
                        dryrunJobParameter = "${ACR_REGISTRY_PURGE_DRY_RUN}"
                        echo "Dry run mode? " + "${ACR_REGISTRY_PURGE_DRY_RUN}"
                    } catch (err) {
                        dryrunJobParameter = false
                    } finally {
                        dryrunJobParameter = dryrunJobParameter.toBoolean()
                    }

                    dryrun = (dryrunJobParameter) ? ' --dry-run' : ''

                    try {
                        timeout = "${ACR_PURGE_COMMAND_TIMEOUT}"
                        timeout = timeout.toInteger()                    
                    } catch (err) {
                        timeout = 1
                    }
                    timeout = timeout * 60 * 60
                    print "ACR purge command timeout: " + timeout

                    try {
                        olderThan = "${ACR_PURGE_IMAGE_OLDER_THAN}"
                        olderThan = olderThan.toInteger()                    
                    } catch (err) {
                        olderThan = 30
                    }
                    print "ACR purge images older than: " + olderThan

                    try {
                        minimumToKeep = "${ACR_MINIMUM_IMAGES_TO_KEEP}"
                        minimumToKeep = minimumToKeep.toInteger()                    
                    } catch (err) {
                        minimumToKeep = 3
                    }
                    print "ACR minimum amount of images to be kept, no matter how old they are: " + minimumToKeep                                                      
                }
            }
        }

        stage('Jenkins Docker Repository Purge') {

            when {
                
                expression {

                    // skip this stage when ACR dry run mode is active
                    !dryrunJobParameter
                }
            }

            steps {

                script {

                    try {
                      sh 'sudo -S docker images -q --filter "dangling=true" | sudo -S xargs -r docker rmi --force'
                    } catch(err) {
                      print 'Purge the local Docker registry command has failed. Reason: ' + err.toString()
                    }
                }
            }
        }

        stage('Jenkins Docker Repository Status') {

            when {

                expression {

                    // skip this stage when ACR dry run mode is active
                    !dryrunJobParameter
                }
            }

            steps {

                script {

                    try {
                      sh 'sudo -S docker images'
                    } catch(err) {
                      print 'Status print command has failed. Reason: ' + err.toString()
                    }
                }
            }
        }

        stage('Jenkins Docker Repository Post Purge Check') {
            
            when {

                expression {

                    // skip this stage when ACR dry run mode is active
                    !dryrunJobParameter
                }
            }

            steps {

                script {

                    try {
                      sh 'sudo -S docker images --filter "dangling=true"'
                    } catch(err) {
                      print 'Listing dangling images command has failed. Reason: ' + err.toString()
                    }
                }
            }
        }

        stage('System Disk Space Status') {

            when {

                expression {
                    // skip this stage when ACR dry run mode is active
                    !dryrunJobParameter
                }
            }

            steps {

                script {

                    try {
                      sh 'df -h'
                    } catch(err) {
                      print 'Checking the disk space status print command has failed. Reason: ' + err.toString()
                    }
                }
            }
        }
        
        stage('Azure Container Registries Purge') {

            steps {
                    
                script {
                    
                    // replace the 'credentialsId' with a value of your own
                    withCredentials([azureServicePrincipal(credentialsId: 'jenkinsServicePrincipal',
                                                                    subscriptionIdVariable: 'SUBS_ID',
                                                                    clientIdVariable: 'CLIENT_ID',
                                                                    clientSecretVariable: 'CLIENT_SECRET',
                                                                    tenantIdVariable: 'TENANT_ID')]) {

                        def acrRegistries = sh(returnStdout: true, script: "az acr list --subscription '${SUBS_ID}' --output json")
                        acrRegistries = readJSON text: acrRegistries                            
                        
                        def registries = []

                        for (int i = 0; i < acrRegistries.size(); i++) {

                            registries[i] = acrRegistries[i]['name']
                        }

                        for(int j = 0; j < registries.size(); j++) {

                            def registryName = registries[j]

                            def acrRegistryImages = sh(returnStdout: true, script: "az acr repository list --name '${registryName}' --output json")
                            acrRegistryImages = readJSON text: acrRegistryImages

                            print 'ACR Repository: ' + registryName + ' - ACR Registry Images (Count: ' + acrRegistryImages.size() + ')'
                            print groovy.json.JsonOutput.prettyPrint(acrRegistryImages.toString())

                            for (int k = 0; k < acrRegistryImages.size(); k++) {

                                sh "az acr run --cmd \"acr purge --filter '${acrRegistryImages[k]}:.*' --ago ${olderThan}d --keep ${minimumToKeep} --untagged${dryrun}\" --registry ${registryName} --timeout ${timeout} /dev/null"
                            }
                        }
                    }
                }
            }
        }                                 
    }

    post {

        success {
            print 'Build has successfully finished, sending email notification.'
            script {
                sendEmailNotification()
            }
        }

        failure {
            print 'Build has failed, sending email notification.'
            script {
                sendEmailNotification()
            }
        }

        unstable {
            print 'The build has become unstable, sending email notification.'
            script {
                sendEmailNotification()
            }
        }

        aborted {
            print 'The build has been aborded due to a failure in one of the external services. Please check the console logs.'
            script {
                sendEmailNotification()
            }
        }

        cleanup {
            print 'Cleaning up the current build workspace folder.'
            deleteDir()
        }
    }
}

void sendEmailNotification() {

    emailext attachLog: true,
            body: '''${SCRIPT, template="groovy-html.template"}''',
            mimeType: 'text/html',
            subject: "[Container Registries Purge] - ${currentBuild.fullDisplayName}",
            to: "${EMAIL_RECIPIENTS}",
            replyTo: "${EMAIL_RECIPIENTS}"
}

