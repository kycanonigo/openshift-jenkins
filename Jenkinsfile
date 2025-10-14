pipeline {
    agent any

    environment {
        JIRA_BASE_URL = 'https://yourdomain.atlassian.net'       // 🔁 Replace with your Jira URL
        JIRA_PROJECT_KEY = 'CICD'                                // 🔁 Replace with your Jira project key
        JIRA_CREDENTIALS_ID = 'jira-api-token'                  // 🔁 Jenkins credentials ID with Jira API token
    }

    stages {

        stage('Build App') {
            agent {
                kubernetes {
                    defaultContainer 'maven'
                    yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: maven
    image: maven:3.8.6-openjdk-18
    command:
    - cat
    tty: true
    volumeMounts:
    - mountPath: /home/jenkins/.m2
      name: maven-cache
  volumes:
  - name: maven-cache
    emptyDir: {}
"""
                }
            }
            steps {
                container('maven') {
                    script {
                        def localRepo = "/home/jenkins/.m2/repository"
                        withEnv([
                            "MAVEN_CONFIG=/home/jenkins/.m2",
                            "MAVEN_OPTS=-Dmaven.repo.local=${localRepo}"
                        ]) {
                            git branch: 'main', url: 'https://github.com/jbramon/kylecanonigo-project.git'
                            def pom = readMavenPom file: 'pom.xml'
                            version = pom.version
                            sh "mvn clean install -Dmaven.repo.local=${localRepo}"
                        }

                        stash name: 'built-jar', includes: 'target/*.jar'
                    }
                }
            }
        }

        stage('Create Image Builder') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject() {
                            def bcName = "sample-app-jenkins-new"
                            def bcSelector = openshift.selector("bc", bcName)
                            def isSelector = openshift.selector("is", bcName)

                            if (bcSelector.exists()) {
                                echo "BuildConfig '${bcName}' exists. Deleting it first..."
                                bcSelector.delete()
                            }

                            if (isSelector.exists()) {
                                echo "ImageStream '${bcName}' exists. Deleting it..."
                                isSelector.delete()
                            }

                            timeout(time: 30, unit: 'SECONDS') {
                                waitUntil {
                                    return !openshift.selector("bc", bcName).exists() &&
                                           !openshift.selector("is", bcName).exists()
                                }
                            }

                            echo "Creating new BuildConfig..."
                            openshift.newBuild(
                                "--name=${bcName}",
                                "--image-stream=openjdk-11-rhel7:1.14",
                                "--binary=true"
                            )
                        }
                    }
                }
            }
        }

        stage('Build Image') {
            steps {
                script {
                    unstash 'built-jar'

                    sh "rm -rf ocp && mkdir -p ocp/deployments"
                    sh "cp target/*.jar ocp/deployments"

                    openshift.withCluster() {
                        openshift.withProject() {
                            openshift.selector("bc", "sample-app-jenkins-new")
                                     .startBuild("--from-dir=./ocp", "--follow", "--wait=true")
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                expression {
                    openshift.withCluster() {
                        openshift.withProject() {
                            return !openshift.selector('dc', 'sample-app-jenkins-new').exists()
                        }
                    }
                }
            }
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject() {
                            def app = openshift.newApp("sample-app-jenkins-new", "--as-deployment-config")
                            app.narrow("svc").expose()
                        }
                    }
                }
            }
        }
    }

    // ⛑️ FAILURE HANDLING TO CREATE JIRA ISSUE
    post {
        failure {
            script {
                def jobName = env.JOB_NAME
                def buildNumber = env.BUILD_NUMBER
                def buildUrl = env.BUILD_URL
                def summary = "CI/CD Pipeline Failed: ${jobName} #${buildNumber}"
                def description = """\
*Pipeline Failed*

Job: ${jobName}  
Build: [#${buildNumber}](${buildUrl})  
Timestamp: ${new Date().format("yyyy-MM-dd HH:mm:ss")}  
Node: ${env.NODE_NAME}

Please investigate the failure.
"""

                def payload = """{
                  "fields": {
                    "project": {
                      "key": "${env.JIRA_PROJECT_KEY}"
                    },
                    "summary": "${summary}",
                    "description": "${description}",
                    "issuetype": {
                      "name": "Bug"
                    }
                  }
                }"""

                httpRequest authentication: env.JIRA_CREDENTIALS_ID,
                            httpMode: 'POST',
                            contentType: 'APPLICATION_JSON',
                            requestBody: payload,
                            url: "${env.JIRA_BASE_URL}/rest/api/3/issue",
                            validResponseCodes: '201'
            }
        }
    }
}
