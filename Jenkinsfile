pipeline {
    agent {
        docker {
            image 'iubar-maven-ubuntu:jdk11'
            label 'docker'
            args '-v ${HOME}/.m2:/home/jenkins/.m2:rw,z -v ${HOME}/.sonar:/home/jenkins/.sonar:rw,z'
        }
    }
    options {
        ansiColor('xterm')
    }
    environment {
        MAVEN_ARGS = '--batch-mode --errors --fail-fast --show-version --quiet'
        MAVEN_OPTS = '-Dstyle.color=always -Djava.awt.headless=true'
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn $MAVEN_ARGS $MAVEN_OPTS clean compile'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn $MAVEN_ARGS $MAVEN_OPTS test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('Quality') {
            when {
                not {
                    environment name: 'SKIP_SONARQUBE', value: 'true'
                }
            }
            steps {
                sh '''
                    sonar-scanner
                    wget --user=${ARTIFACTORY_USER} --password=${ARTIFACTORY_PASS} http://192.168.0.119:8082/artifactory/iubar-repo-local/jenkins/jenkins-sonar-quality-gate-check.sh --no-check-certificate
                    chmod +x ./jenkins-sonar-quality-gate-check.sh
                    ./jenkins-sonar-quality-gate-check.sh false # true / false = Ignore or not the quality gate score
                '''
            }
        }
        stage('Deploy') {
            steps {
                sh 'mvn $MAVEN_ARGS $MAVEN_OPTS -DskipTests deploy'
            }
        }
    }
    post {
        always { // oppure utilizzare changed
            sh "curl -H 'JENKINS: Pipeline Hook Iubar' -i -X GET -G ${env.IUBAR_WEBHOOK_URL} -d status=${currentBuild.currentResult} -d job_name='${JOB_NAME}' -d build_number='${BUILD_NUMBER}'"
        }
        cleanup {
            cleanWs()
            dir("${WORKSPACE}@tmp") {
                deleteDir()
            }
        }
    }
}
