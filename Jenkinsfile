
pipeline {
    agent none
    environment {
        version = 'v1.0.0'
        devSvrPasswd = '64391099@inhand'
        devSvrHost = '10.5.16.213'
        devSvrUser = 'inhand'
        project = 'elements'
    }

    options {
        disableConcurrentBuilds()
        skipDefaultCheckout()
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    //parameters {
        //string(name: 'branch', defaultValue: 'develop', description: 'Please input the branc name?')
        //string(name: 'commitId', defaultValue: '', description: 'Please input the commit id or tag?')
        //string(name: 'project', defaultValue: '', description: 'Please input the destination project?')
    //}

    stages {
        stage('Checkout') {
            agent any
            steps {
                script {
                    def gitPreCommit = checkoutProject()
                    // custom build display name, add git commit reversion
                    currentBuild.displayName = displayName()
                    echo "Start to build ${currentBuild.displayName}"

                    javaModules = ['config-server']

                }
                //checkout2Dst(params.branch,params.commitId)
                //notice build starting to slack 'ci' channel
                //slackSendStart()
            }
        }
        stage('Build') {
            agent any
            steps {
                script {
                    echo "Start to excute gradlew.sh "
                        if (javaModules.size() > 0) {
                            sh "gradle"
                        }
                }
            }
        }

        stage('Docker') {
            agent any
            when {
                anyOf {
                    branch 'master';
                    branch 'release'
                }
            }
            steps {
                script {
                   // stash includes: 'swagger.yaml', name: 'swagger'
                   // stash includes: 'config/**,docker-*.yml', name: 'config'

                    buildDockerImg(javaModules, env.project)
                }
            }
        }

        stage('Artifact') {
            agent any
            when {
                branch 'master'
            }
            steps {
                archiveArtifacts '*.yml'
                artifactBuildResults(javaModules, [])
            }
        }

        stage('Deploy') {
            agent {
                label 'office.j3r0lin.com'
            }
            when{
                branch 'release'
            }
            steps {
                script {
                    def host = env.devSvrHost
                    def pass = env.devSvrPasswd
                    def user = env.devSvrUser
                    def names = javaModules.join(' ')
                    if (!names.isEmpty()) {
                        echo "Deploy services ${names}"
                        sh "sshpass -p '${pass}' ssh ${user}@${host} 'cd /mnt/elements&&sudo docker-compose pull --parallel ${names}'"
                        sh "sshpass -p '${pass}' ssh ${user}@${host} 'cd /mnt/elements&&sudo docker-compose up --no-deps -d ${names}'"
                        slackSend message: "$JOB_NAME: Restarting ${names} on test.smartfleet.cloud."
                    }
                }
            }
        }

        stage('Jira') {
            agent {
                node {
                    label 'office.j3r0lin.com'
                    customWorkspace '/usr/local/elements'
                }
            }
            when {
                branch 'develop'
            }
            steps {
                // comments to jira issues search by git commit messages
                step([$class       : 'hudson.plugins.jira.JiraIssueUpdater',
                      issueSelector: [$class: 'hudson.plugins.jira.selector.DefaultIssueSelector'],
                      scm          : scm])
            }
        }
    }


    post {
        always {
            echo 'build done'
            // send build result status to slack channel
            slackMessage()
        }
    }
}


def buildDockerImg(javaModules=[], project='elements'){
    for (int i = 0; i < javaModules.size(); i++) {
        def m = javaModules[i]
        sh "cp Dockerfile ${m}/build/libs/"
        def app = docker.build("${project}/${m}", "--build-arg app=${m} ${m}/build/libs")
        docker.withRegistry('https://registry.cn-hangzhou.aliyuncs.com', 'han-aliyun-registry') {
            app.push('latest')
            if (env.BRANCH_NAME == 'master') {
                app.push(env.version)
            }
        }
    }
}

def checkout2Dst(br_name,cid){
    if (""!=branch){
        def cmd = "git checkout "+br_name
        def proc = cmd.execute()
        proc.waitFor()
    }
    if (""!=cid){

        def cmd = "git reset --hard "+cid
        def proc = cmd.execute()
        proc.waitFor()
    }
}
