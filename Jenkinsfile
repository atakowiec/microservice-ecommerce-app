def services = [
    'user-service',
    'catalog-service',
    'notification-service',
    'order-service'
]

pipeline {
    agent {
        label 'docker-cloud'
    }

    environment {
        IMAGE_REGISTRY = 'atakowiec'
    }

    stages {
        stage('Build') {
            steps {
                script {
                    services.each { service ->
                        echo "Building ${service}"
                        sh "docker build -t ${IMAGE_REGISTRY}/${service}:${BUILD_NUMBER} ${service}"
                    }
                }
            }
        }

        stage('Push') {
            steps {
                script {
                    withCredentials([
                            usernamePassword(
                                credentialsId: 'docker-hub',
                                usernameVariable: 'USERNAME',
                                passwordVariable: 'TOKEN'
                            )
                        ]) {
                        sh 'echo $TOKEN | docker login -u $USERNAME --password-stdin'

                        services.each { service ->
                            sh "docker push ${IMAGE_REGISTRY}/${service}:${BUILD_NUMBER}"
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([string(credentialsId: 'ec2-host', variable: 'EC2_HOST')]) {
                    sshagent(credentials: ['ec2-ssh-key']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ubuntu@${EC2_HOST} '
                                cd /opt/ecommerce &&
                                export IMAGE_TAG=${BUILD_NUMBER} &&
                                docker compose pull &&
                                docker compose up -d --remove-orphans
                            '
                        """
                    }
                }
            }
        }
    }
}