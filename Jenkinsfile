pipeline {
    agent none

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
                            git branch: 'main', url: 'https://github.com/kuldeepsingh99/openshift-jenkins-cicd.git'
                            def pom = readMavenPom file: 'pom.xml'
                            version = pom.version
                            sh "mvn install -Dmaven.repo.local=${localRepo}"
                        }
                    }
                }
            }
        }

        stage('Create Image Builder') {
            agent any
            when {
                expression {
                    openshift.withCluster() {
                        openshift.withProject() {
                            return !openshift.selector("bc", "sample-app-jenkins-new").exists();
                        }
                    }
                }
            }
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject() {
                            // Use existing tag instead of :latest
                            openshift.newBuild("--name=sample-app-jenkins-new", "--image-stream=openjdk-11-rhel7:1.14", "--binary=true")
                        }
                    }
                }
            }
        }

        stage('Build Image') {
            agent any
            steps {
                sh "rm -rf ocp && mkdir -p ocp/deployments"
                sh "pwd && ls -la target "
                sh "cp target/openshiftjenkins-0.0.1-SNAPSHOT.jar ocp/deployments"

                script {
                    openshift.withCluster() {
                        openshift.withProject() {
                            openshift.selector("bc", "sample-app-jenkins-new").startBuild("--from-dir=./ocp", "--follow", "--wait=true")
                        }
                    }
                }
            }
        }

        stage('deploy') {
            agent any
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
                            app.narrow("svc").expose();
                        }
                    }
                }
            }
        }
    }
}
