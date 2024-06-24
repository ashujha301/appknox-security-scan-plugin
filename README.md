# Appknox Jenkins Plugin

The Appknox Jenkins Plugin allows you to perform Appknox security scan on your mobile application binary. The APK/IPA built from your CI pipeline will be uploaded to Appknox platform which performs static scan and the build will be errored according to the chosen risk threshold.

## How to use it?

### Step 1: Get your Appknox access token

Sign up on [Appknox](https://appknox.com).

Generate a personal access token from <a href="https://secure.appknox.com/settings/developersettings" target="_blank">Developer Settings</a>

### Step 2: Configure the Jenkins Plugin

In your Jenkinsfile Add this after building your Application stage.

```
stages {
        stage('Appknox Scan') {
            steps {
                script {
                        // Perform Appknox scan using AppknoxPlugin
                        step([
                            $class: 'AppknoxPlugin',
                            accessTokenID: 'your-accessToken-ID', //Specify the Appknox Access Token ID. Ensure the ID matches with the ID given while configuring Appknox Access Token in the credentials.
                            filePath: FILE_PATH,
                            riskThreshold: params.RISK_THRESHOLD.toUpperCase()
                        ])
                    
                }
            }
        }
    }
    
```

## Inputs

| Key                     | Value                        |
|-------------------------|------------------------------|
| `accessTokenID`         | Personal access token secret id |
| `file_path`             | File path to the mobile application binary to be uploaded |
| `risk_threshold`        | Risk threshold value for which the CI should fail. <br><br>Accepted values: `CRITICAL, HIGH, MEDIUM & LOW` <br><br>Default: `LOW` |

---

Example:
```
pipeline {
    agent any
    parameters {
        choice(name: 'RISK_THRESHOLD', choices: ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'], description: 'Risk Threshold')
    }
    stages {
        stage('Checkout') {
            steps {
                git 'https://github.com/yourgithub/yourreponame'
            }
        }
        stage('Build App') {
            steps {
                // Build the app using own builder, Example given using gradle
                script {
                    if (isUnix()) {
                        sh './gradlew build'
                        FILE_PATH = "${WORKSPACE}/app/build/outputs/apk/debug/app-debug.apk"
                    } else {
                        bat './gradlew build'
                        FILE_PATH = "${WORKSPACE}\\app\\build\\outputs\\apk\\debug\\app-debug.apk"
                    }
                    echo "Found APK: ${FILE_PATH}"
                }
            }
        }
        stage('Appknox Scan') {
            steps {
                script {
                        // Perform Appknox scan using AppknoxPlugin
                        step([
                            $class: 'AppknoxPlugin',
                            accessTokenID: 'appknox-access-token', //Specify the Appknox Access Token ID. Ensure the ID matches with the ID given while configuring Appknox Access Token in the credentials.
                            filePath: FILE_PATH,
                            riskThreshold: params.RISK_THRESHOLD.toUpperCase()
                        ])
                    
                }
            }
        }
    }
}


```