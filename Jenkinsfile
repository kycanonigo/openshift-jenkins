pipeline {
    agent {
        kubernetes {
            label 'maven'
            defaultContainer 'maven'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
    - name: maven
      image: maven:3.8.6-openjdk-17
      command:
        - cat
      tty: true
"""
        }
    }

    environment {
        VERSION = ""
    }

    stages {
        stage('Build App') {
            steps {
                container('maven') {
                    git branch: 'main', url: 'https://gitlab.com/kylecanonigo-group/kylecanonigo-project.git'

                    script {
                        def pom = readMavenPom file: 'pom.xml'
                        env.VERSION = pom.version
                    }

                    sh "mvn clean install"
                }
            }
        }

        stage('Create Image Builder') {
            when {
                expression {
                    openshift.withCluster() {
                        openshift.withProject() {
                            return !openshift.selector("bc", "sample-app-jenkins-new").exists()
                        }
                    }
                }
            }
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject() {
                            openshift.newBuild("--name=sample-app-jenkins-new", "--image-stream=openjdk18-openshift:1.14-3", "--binary=true")
                        }
                    }
                }
            }
        }

        stage('Build Image') {
            steps {
                container('maven') {
                    sh "rm -rf ocp && mkdir -p ocp/deployments"
                    sh "cp target/openshiftjenkins-${VERSION}.jar ocp/deployments"
                }

                script {
                    openshift.withCluster() {
                        openshift.withProject() {
                            openshift.selector("bc", "sample-app-jenkins-new").startBuild("--from-dir=./ocp", "--follow", "--wait=true")
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

    post {
        failure {
            echo "Pipeline failed. Please check logs."
        }
        success {
            echo "Pipeline completed successfully!"
        }
    }
}
