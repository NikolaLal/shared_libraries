def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

	node {
	    def server = Artifactory.server ('LinuxartsVM')
    	    def rtMaven = Artifactory.newMavenBuild()
            def buildInfo
	    def mvnHome
	    // Clean workspace before doing anything
	    deleteDir()

	    try {
	        stage ('Clone') {
	        	checkout scm
			mvnHome = tool 'M3'
		}
		    
		stage ('Artifactory configuration') {
       			 rtMaven.tool = 'M3' // Tool name from Jenkins configuration
        		 rtMaven.deployer releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local', server: server
        		 rtMaven.resolver releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot', server: server
			 buildInfo = Artifactory.newBuildInfo()
			 buildInfo.env.capture = true
	        }
			
	        stage ('Build') {
	              // Run the maven build
     			 if (isUnix()) {
         			sh "'${mvnHome}/bin/mvn' -Dmaven.test.failure.ignore clean package"
      					} else {
         			bat(/"${mvnHome}\bin\mvn" -Dmaven.test.failure.ignore clean package)
      				}
	        }
		    
	        stage ('Tests') {
		        parallel 'static': {
		            sh "echo 'shell scripts to run static tests...'"
		        },
		        'unit': {
		            sh "echo 'shell scripts to run unit tests...'"
		        },
		        'integration': {
		            sh "echo 'shell scripts to run integration tests...'"
		        }
	        }
		    
		stage ('Publish build info') {
        	    server.publishBuildInfo buildInfo
		}
		    
	      	stage ('Deploy') {
	            sh "echo 'deploying to server ${config.serverDomain}...'"
	      	}
	    } catch (err) {
	        currentBuild.result = 'FAILED'
	        throw err
	    }
    }
}
