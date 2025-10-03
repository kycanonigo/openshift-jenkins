pipeline {
    agent any

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
                            git branch: 'main', url: 'https://gitlab.com/kylecanonigo-group/kylecanonigo-project.git'
                            def pom = readMavenPom file: 'pom.xml'
                            version = pom.version
                            sh "mvn clean install -Dmaven.repo.local=${localRepo}"
                        }

                        // Stash the target folder for later stages
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
                    // Unstash the built JAR
                    unstash 'built-jar'

                    // Ensure directory structure
                    sh "rm -rf ocp && mkdir -p ocp/deployments"

                    // Copy the jar to deployments folder
                    sh "cp target/*.jar ocp/deployments"

                    // Start OpenShift build
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
}
