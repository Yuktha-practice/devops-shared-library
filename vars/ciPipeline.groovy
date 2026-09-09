def call(Map config = [:]) {

    pipeline {

        agent any

        stages {

            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('Build') {
                steps {
                    echo "Building ${config.application ?: 'application'}"
                }
            }

            stage('Test') {
                steps {
                    echo "Running tests for ${config.application ?: 'application'}"
                }
            }

            stage('Docker Build') {
                steps {
                    echo "Building Docker image for ${config.application ?: 'application'}"
                }
            }
        }
    }
}
