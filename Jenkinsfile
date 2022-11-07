pipeline {
    environment {
        ARTIFACTORY_URL = credentials('ARTIFACTORY_URL')
    }

    agent {
        label 'build'
    }

    tools {
        gradle "gradle-7.2"
        jdk 'adoptjdk13'
    }

    stages {

        stage ('Build Lambdas') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                    sh './gradlew -b build.gradle '
                }
            }
        }
        stage ('Test Lambdas') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                    sh './gradlew clean test --info -b build.gradle'
                }
            }
        }

        stage ('Build Jars') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                    sh './gradlew buildZip --info -b build.gradle'
                }
            }
        }
        stage('SonarQube Analysis') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                     Automatically saves the an id for the SonarQube build
                    withSonarQubeEnv('CMSSonar') {
                        sh './gradlew sonarqube -Dsonar.projectKey=ab2d-lambdas -Dsonar.host.url=https:sonarqube.cloud.cms.gov'
                    }
                }
            }
        }
        stage("Quality Gate") {
            options {
                timeout(time: 10, unit: 'MINUTES')
            }
            steps {
                 Parameter indicates whether to set pipeline to UNSTABLE if Quality Gate fails
                 true = set pipeline to UNSTABLE, false = don't
                waitForQualityGate abortPipeline: true
            }
        }

        stage ('Publish Lambdas') {
            //when {
          //      branch 'main'
          //  }
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactoryuserpass', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_PASSWORD')]) {
                    script {
                        def deployScript = '';

                        //Calls a gradle task to check if the version of each build has already been deployed
                        //Each build is broken up by '''.
                        def versionPublishedList = sh(
                                script: './gradlew -q lookForArtifacts',
                                returnStdout: true
                        ).trim().split("'''")

                        //First value represents the build name and the second value is if the version is already deployed.
                        for (int i = 1; i < versionPublishedList.size(); i++) {
                            def artifactoryInfo = versionPublishedList[i].split(":")
                            if (artifactoryInfo[1] == 'false') {
                                echo "Deploying ${artifactoryInfo[0]}"
                                deployScript += "${artifactoryInfo[0]}:artifactoryPublish "
                            }
                        }

                        //deployScript represents what we are publishing. Insert it into the gradle command to do the publishing.
                        //ex. ab2d-fhir:artifactoryPublish"
                        //If nothing is there, skip publishing
                        if(deployScript != '') {
                            sh "./gradlew ${deployScript} -b build.gradle"
                        }
                    }
                }
            }
        }
    }
}