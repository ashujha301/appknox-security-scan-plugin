pipeline {
    agent any
    
    environment {
        JAVA_HOME = tool name: 'JDK 1.8', type: 'jdk'
        PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
    }
    
    parameters {
        string(name: 'APPKNOX_ACCESS_TOKEN', defaultValue: '', description: 'Appknox Access Token')
        string(name: 'FILE_PATH', defaultValue: '', description: 'Path to the Binary file')
        choice(name: 'RISK_THRESHOLD', choices: ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'], description: 'Risk Threshold')
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/ashujha301/appknox-jenkins-plugin'
            }
        }
        
        stage('Download Appknox CLI') {
            steps {
                script {
                    def appknoxCLI = new io.jenkins.plugins.appknox.commands.AppknoxCommands()
                    appknoxCLI.downloadAppknoxCLI()
                }
            }
        }
        
        stage('Appknox Jenkins Plugin Execution') {
            steps {
                script {
                    def appknoxTool = new io.jenkins.plugins.appknox.tools.AppknoxTool()
                    def accessToken = params.APPKNOX_ACCESS_TOKEN
                    def filePath = params.FILE_PATH
                    def riskThreshold = params.RISK_THRESHOLD
                    
                    appknoxTool.execute(accessToken, filePath, riskThreshold)
                }
            }
        }
    }
    
    post {
        always {
            echo 'Cleaning up...'
            // Remove the Appknox CLI binary
            sh 'rm -f ${env.HOME}/bin/appknox'
        }
        success {
            echo 'Appknox Jenkins Plugin tasks completed successfully.'
        }
        failure {
            echo 'Appknox Jenkins Plugin tasks failed.'
        }
        cleanup {
            cleanWs()
        }
    }
}
