pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    parameters {
        booleanParam(
            name: 'PUBLISH_IMAGES',
            defaultValue: false,
            description: 'GHCR image publish stage is only enabled when this flag is true.'
        )
        string(
            name: 'GHCR_NAMESPACE',
            defaultValue: 'ghcr.io/your-org-or-user',
            description: 'Example: ghcr.io/onurcansevinc'
        )
    }

    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'chmod +x mvnw'
            }
        }

        stage('Backend Tests') {
            steps {
                sh './mvnw test'
            }
        }

        stage('Frontend Build') {
            steps {
                dir('storefront') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('Docker Compose Config') {
            steps {
                sh 'docker compose --env-file .env.production.example -f docker-compose.yml -f docker-compose.apps.yml config > /tmp/java-ecommerce.compose.rendered.yml'
            }
        }

        stage('Jib Smoke Build') {
            steps {
                sh './mvnw -pl product-service -am -Dmaven.test.skip=true package com.google.cloud.tools:jib-maven-plugin:3.4.6:buildTar'
                sh 'test -f product-service/target/jib-image.tar'
            }
        }

        stage('Publish GHCR Images') {
            when {
                expression {
                    def branchName = env.BRANCH_NAME ?: env.GIT_BRANCH ?: ''
                    return params.PUBLISH_IMAGES && (branchName == 'main' || branchName.endsWith('/main'))
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'ghcr-credentials', usernameVariable: 'REGISTRY_USERNAME', passwordVariable: 'REGISTRY_PASSWORD')]) {
                    sh '''
                        set -eu
                        test -n "$GHCR_NAMESPACE"
                        echo "$REGISTRY_PASSWORD" | docker login ghcr.io -u "$REGISTRY_USERNAME" --password-stdin

                        IMAGE_TAG="$(git rev-parse --short=12 HEAD)"

                        for module in \
                          product-service \
                          inventory-service \
                          cart-service \
                          order-service \
                          payment-service \
                          notification-service \
                          api-gateway \
                          config-server \
                          discovery-server
                        do
                          ./mvnw -pl "$module" -am -Dmaven.test.skip=true package \
                            com.google.cloud.tools:jib-maven-plugin:3.4.6:build \
                            -Djib.to.image="$GHCR_NAMESPACE/java-ecommerce-$module:$IMAGE_TAG" \
                            -Djib.to.tags=latest
                        done
                    '''
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'product-service/target/jib-image.tar', allowEmptyArchive: true
        }
    }
}
