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
                            git branch: 'main', url: 'https://gitlab.com/kylecanonigo-group/kylecanonigo-project.git'
                            def pom = readMavenPom file: 'pom.xml'
                            version = pom.version
                            sh "mvn install -Dmaven.repo.local=${localRepo}"
                        }
                    }
                }
            }
        }

        stage('Create Image Builder') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject() {
                            def bcSelector = openshift.selector("bc", "sample-app-jenkins-new")
                            def isSelector = openshift.selector("is", "sample-app-jenkins-new")

                            if (bcSelector.exists()) {
                                echo "BuildConfig 'sample-app-jenkins-new' exists. Deleting it first..."
                                bcSelector.delete()
                            }

                            if (isSelector.exists()) {
                                echo "ImageStream 'sample-app-jenkins-new' exists. Deleting it first..."
                                isSelector.delete()
                            }

                            // Wait for deletion to complete
                            timeout(time: 30, unit: 'SECONDS') {
                                waitUntil {
                                    def bcGone = !openshift.selector("bc", "sample-app-jenkins-new").exists()
                                    def isGone = !openshift.selector("is", "sample-app-jenkins-new").exists()
                                    return bcGone && isGone
                                }
                            }

                            echo "Creating new BuildConfig 'sample-app-jenkins-new'..."
                            openshift.newBuild("--name=sample-app-jenkins-new", "--image-stream=openjdk-11-rhel7:1.14", "--binary=true")
                        }
                    }
                }
            }
        }


        stage('Build Image') {
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
