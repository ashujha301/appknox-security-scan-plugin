# Appknox Jenkins Plugiin

The Appknox Jenkins Plugin allows you to perform Appknox security scan on your mobile application binary. The APK/IPA built from your CI pipeline will be uploaded to Appknox platform which performs static scan and the build will be errored according to the chosen risk threshold.

## How to use it?

### Step 1: Get your Appknox access token

Sign up on [Appknox](https://appknox.com).

Generate a personal access token from <a href="https://secure.appknox.com/settings/developersettings" target="_blank">Developer Settings</a>

### Step 2: Configure the Jenkins Plugin

In your Jenkinsfile Add this after building your Application stage.

```
stage('Appknox Scan') {
            steps {
                script {
                    def apkFilePath = 'app/build/outputs/apk/debug/app-debug.apk'  // Adjust this path as necessary
                    def accessToken = params.APPKNOX_ACCESS_TOKEN
                    def riskThreshold = params.RISK_THRESHOLD

                    // Trigger Appknox Jenkins Plugin
                    build job: 'appknox-jenkins-plugin', 
                    parameters: [
                        string(name: 'APPKNOX_ACCESS_TOKEN', value: accessToken),
                        string(name: 'FILE_PATH', value: apkFilePath),
                        string(name: 'RISK_THRESHOLD', value: riskThreshold)
                    ]
                }
            }
        }
```

## Inputs

| Key                     | Value                        |
|-------------------------|------------------------------|
| `appknox_access_token`  | Personal access token secret |
| `file_path`             | File path to the mobile application binary to be uploaded |
| `risk_threshold`        | Risk threshold value for which the CI should fail. <br><br>Accepted values: `CRITICAL, HIGH, MEDIUM & LOW` <br><br>Default: `LOW` |

---

Example:
```
pipeline {
    agent any
    
    parameters {
        string(name: 'APPKNOX_ACCESS_TOKEN', defaultValue: '', description: 'Appknox Access Token')
        string(name: 'RISK_THRESHOLD', defaultValue: 'LOW', description: 'Risk Threshold')
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                sh './gradlew build'  // Assuming the customer uses Gradle to build their project
            }
        }
        
        stage('Appknox Scan') {
            steps {
                script {
                    def apkFilePath = 'app/build/outputs/apk/debug/app-debug.apk'  // Adjust this path as necessary
                    def accessToken = params.APPKNOX_ACCESS_TOKEN
                    def riskThreshold = params.RISK_THRESHOLD

                    // Trigger Appknox Jenkins Plugin
                    build job: 'appknox-jenkins-plugin', 
                    parameters: [
                        string(name: 'APPKNOX_ACCESS_TOKEN', value: accessToken),
                        string(name: 'FILE_PATH', value: apkFilePath),
                        string(name: 'RISK_THRESHOLD', value: riskThreshold)
                    ]
                }
            }
        }
    }
}

```