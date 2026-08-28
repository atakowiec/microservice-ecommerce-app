def services = [
    'web-app',
    'api-gateway',
    'user-service',
    'catalog-service',
    'notification-service',
    'order-service'
]

pipeline {
    agent {
        label 'docker-cloud'
    }

    options {
        disableConcurrentBuilds()
        timestamps()
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
                        if (service == 'web-app') {
                            sh "docker build -t ${IMAGE_REGISTRY}/${service}:${BUILD_NUMBER} ${service}"
                        } else {
                            sh "docker build -f ${service}/Dockerfile -t ${IMAGE_REGISTRY}/${service}:${BUILD_NUMBER} ."
                        }
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
                            ssh -o StrictHostKeyChecking=no ubuntu@${EC2_HOST} 'mkdir -p /home/ubuntu/ecommerce'
                            scp -o StrictHostKeyChecking=no -r k8s ubuntu@${EC2_HOST}:/home/ubuntu/ecommerce/

                            ssh -o StrictHostKeyChecking=no ubuntu@${EC2_HOST} '
                                set -eu
                                cd /home/ubuntu/ecommerce &&
                                export KUBECONFIG=/home/ubuntu/.kube/config &&

                                kubectl apply -f k8s/namespace.yaml &&
                                kubectl -n ecommerce get secret ecommerce-credentials >/dev/null &&
                                kubectl get ingressclass traefik >/dev/null &&
                                sed -i "s/latest/${BUILD_NUMBER}/g" k8s/kustomization.yaml &&
                                kubectl apply -k k8s &&

                                kubectl -n ecommerce rollout status deployment/api-gateway --timeout=180s &&
                                kubectl -n ecommerce rollout status deployment/user-service --timeout=180s &&
                                kubectl -n ecommerce rollout status deployment/catalog-service --timeout=180s &&
                                kubectl -n ecommerce rollout status deployment/notification-service --timeout=180s &&
                                kubectl -n ecommerce rollout status deployment/order-service --timeout=180s &&
                                kubectl -n ecommerce rollout status deployment/web-app --timeout=180s
                            '
                        """
                    }
                }
            }
        }
    }
}
