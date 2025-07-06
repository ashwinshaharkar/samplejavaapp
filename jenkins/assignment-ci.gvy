pipeline {
    agent any
    stages {
        stage('Maven Compile') {
            steps {
                echo 'Compiling..'
                git url: 'https://github.com/ashwinshaharkar/samplejavaapp'
                sh script: '/opt/maven/bin/mvn compile'
           }
        }
        stage('PMD Analysis') {
            steps {
                echo 'Code Review..'
                sh script: '/opt/maven/bin/mvn -P metrics pmd:pmd'
            }
            post {
                success {
                    recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/target/pmd.xml')
                }
            }		
        }
        stage('Unit Tests') {
            steps {
                echo 'Unit Tests..'
                sh script: '/opt/maven/bin/mvn test'
            }
            post {
                success {
                    junit 'target/surefire-reports/*.xml'
                }
            }			
        }
        stage('Verification') {
            steps {
                echo 'Verification..'
                sh script: '/opt/maven/bin/mvn verify'
            }
            post {
                success {
                    jacoco buildOverBuild: true, deltaBranchCoverage: '20', deltaClassCoverage: '20', deltaComplexityCoverage: '20', deltaInstructionCoverage: '20', deltaLineCoverage: '20', deltaMethodCoverage: '20'
                }
            }			
        }
        stage('Package') {
            steps {
                echo 'Package..'
                sh script: '/opt/maven/bin/mvn package'	
           }		
        }
        stage('Deploy') {
            steps {
                // Copy built artifacts to deployment directory
                sh 'mkdir -p /var/www/develop-branch'
                sh 'cp target/*.jar /var/www/develop-branch/'
                sh 'cp -r target/site /var/www/develop-branch/'
            }
        }
    }
    post {
        always {
            // Clean up workspace after build
            cleanWs()
        }
        success {
            slackSend color: 'good', message: "Build Successful: ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
        failure {
            slackSend color: 'danger', message: "Build Failed: ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
}