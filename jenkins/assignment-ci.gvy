pipeline {
    agent any
    
    tools {
        // Configure Maven in Jenkins Global Tool Configuration
        maven 'M3'  // Name must match your Jenkins Maven installation
    }
    
    triggers {
        pollSCM('H/5 * * * *') // Fallback polling every 5 minutes
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code..'
                git branch: 'main', 
                    url: 'https://github.com/ashwinshaharkar/samplejavaapp'
            }
        }
        
        stage('Maven Compile') {
            steps {
                echo 'Compiling..'
                sh 'mvn compile'  // Using Jenkins-configured Maven
            }
        }
        
        stage('PMD Analysis') {
            steps {
                echo 'Running PMD Analysis..'
                sh 'mvn -P metrics pmd:pmd'
            }
            post {
                success {
                    recordIssues enabledForFailure: true, 
                                tools: [pmd(pattern: '**/target/pmd.xml')]
                }
            }
        }
        
        stage('Unit Tests') {
            steps {
                echo 'Running Unit Tests..'
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Verification') {
            steps {
                echo 'Verifying..'
                sh 'mvn verify'
            }
            post {
                success {
                    jacoco buildOverBuild: true, 
                          deltaBranchCoverage: '20', 
                          deltaClassCoverage: '20', 
                          deltaComplexityCoverage: '20', 
                          deltaInstructionCoverage: '20', 
                          deltaLineCoverage: '20', 
                          deltaMethodCoverage: '20'
                }
            }
        }
        
        stage('Package') {
            steps {
                echo 'Packaging..'
                sh 'mvn package'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
        
        stage('Deploy') {
            steps {
                echo 'Deploying artifacts..'
                sh 'mkdir -p /var/www/develop-branch'
                sh 'cp target/*.jar /var/www/develop-branch/'
                sh 'cp -r target/site /var/www/develop-branch/'
            }
        }
    }
    
    post {
        always {
            cleanWs()
            // Delete target directory to save space
            sh 'mvn clean'
        }
        success {
            slackSend color: 'good', 
                     message: "Build Successful: ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
        }
        failure {
            slackSend color: 'danger', 
                     message: "Build Failed: ${env.JOB_NAME} ${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)"
        }
    }
}