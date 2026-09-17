def call(Map config = [:]) {

    pipeline {

        agent any
		tools {
	 		maven 'mymaven'
	 	}
        stages {

            stage('Clean Workspace') {
                steps {
                    cleanWs()
                }
            }

            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('Build') {
                steps {
                    script {

                        if (config.type == 'node') {

                            sh 'npm ci'
                            sh 'npm run build'

                        }
                        else if (config.type == 'java') {

                            sh './mvnw clean package'

                        }
                        else if (config.type == 'database') {

                            sh 'docker compose config'

                        }
                    }
                }
            }

            stage('Test') {
                steps {
                    script {

                        if (config.type == 'node') {

                            sh 'npm test'

                        }
                        else if (config.type == 'java') {

                            echo "Java tests already executed during Maven package"

                        }
                        else if (config.type == 'database') {

                            echo "No application tests for database"

                        }
                    }
                }
            }
			
			stage('CQA - SonarQube') {
                steps {
                    script {

                        if (config.type == 'node') {

                            def scannerHome = tool 'mysonar'

                            withSonarQubeEnv('mysonar') {
                                sh """
                                    ${scannerHome}/bin/sonar-scanner \
                                    -Dsonar.projectKey=${config.sonarProjectKey}
                                """
                            }

                        } else if (config.type == 'java') {


				                withSonarQubeEnv('mysonar') {
				                    sh """
				                        mvn clean verify sonar:sonar \
				                        -Dsonar.projectKey=${config.sonarProjectKey}
				                    """
								}
                        } else if (config.type == 'database') {

                            echo 'SonarQube analysis skipped for database repository'
                        }
                    }
                }
            }
			
			stage('Quality Gate') {
			    steps {
			        timeout(time: 5, unit: 'MINUTES') {
			            waitForQualityGate abortPipeline: true
			        }
			    }
			}

            stage('Docker Build') {
                steps {
                    script {
					
                        sh "docker build -t ${config.application}:latest ."

                    }
                }
            }

            stage('Trivy Scan') {
                steps {
                    script {
					
                       sh "TMPDIR=/var/lib/trivy-tmp trivy --cache-dir /var/lib/trivy image ${config.application}:latest"
                    }
                }
            }

            stage('Docker Push') {
                steps {
                    script {

						def ecrRegistry = "080665850643.dkr.ecr.${config.awsRegion}.amazonaws.com"

						sh """
							aws ecr get-login-password --region ${config.awsRegion} | \
							docker login --username AWS --password-stdin ${ecrRegistry}

							docker tag ${config.application}:latest \
							${ecrRegistry}/${config.ecrRepository}:latest

							docker push \
							${ecrRegistry}/${config.ecrRepository}:latest
						"""
    
                    }
                }
            }
        }
    }
}
