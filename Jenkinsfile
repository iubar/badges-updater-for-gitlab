pipeline {
    agent {    
    	docker {   	
    		image 'iubar-maven-alpine'
    		label 'docker'
    		args '-v ${HOME}/.m2:/home/jenkins/.m2:rw,z -v ${HOME}/.sonar:/home/jenkins/.sonar:rw,z'
    	} 
    }
    stages {
        stage ('Build') {
            steps {
                sh 'mvn --batch-mode clean compile'
            }
        }
		stage('Test') {
            steps {
                sh 'mvn --batch-mode -Djava.io.tmpdir=${WORKSPACE}@tmp -Djava.awt.headless=true test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml' // show junit log in Jenkins 
                }
            }
        }
        stage('Quality') {
			agent { 
				docker {    		
					image 'iubar-sonar'
					label 'docker'
				}
			}				
            steps {
				sh '''
					echo "SKIP_SONARQUBE: ${SKIP_SONARQUBE}"
					if [ $SKIP_SONARQUBE = true ]; then												
						echo "Skipping sonar-scanner analysis"
            		else
               			sonar-scanner
						wget --user=${ARTIFACTORY_USER} --password=${ARTIFACTORY_PASS} http://192.168.0.119:8082/artifactory/iubar-repo-local/jenkins/jenkins-sonar-quality-gate-check.sh --no-check-certificate
						chmod +x ./jenkins-sonar-quality-gate-check.sh
						./jenkins-sonar-quality-gate-check.sh false # true / false = Ignore or not the quality gate score
                	fi
				'''
            }
        }
		stage ('Deploy') {
            steps {
                sh 'mvn --batch-mode -DskipTests=true deploy'
            }
        }		
    }
	post {
        changed {
        	echo "CURRENT STATUS: ${currentBuild.currentResult}"
            sh "curl -H 'JENKINS: Pipeline Hook Iubar' -i -X GET -G ${env.IUBAR_WEBHOOK_URL} -d status=${currentBuild.currentResult} -d project_name='${JOB_NAME}'"
        }
		cleanup {
			cleanWs()
			dir("${env.WORKSPACE}@tmp") {				
				deleteDir()
			}
        }
    }    
}
