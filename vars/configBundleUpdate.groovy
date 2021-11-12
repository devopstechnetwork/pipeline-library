// vars/configBundleUpdate.groovy
def call(String nameSpace = "cbci") {
  def bundleName = env.BUNDLE_ID
  def label = "kubectl"
  def podYaml = libraryResource 'podtemplates/kubectl.yml'
  
  podTemplate(name: 'kubectl', label: label, yaml: podYaml) {
    node(label) {
      checkout scm
      container("kubectl") {
        sh "mkdir -p ${bundleName}"
        sh "cp *.yaml ${bundleName}"
        sh "kubectl cp --namespace ${nameSpace} ${bundleName} cjoc-0:/var/jenkins_home/jcasc-bundles-store/ -c jenkins"
      }
      echo "begin config bundle reload"
      withCredentials([usernamePassword(credentialsId: 'admin-cli-token', usernameVariable: 'JENKINS_CLI_USR', passwordVariable: 'JENKINS_CLI_PSW')]) {
        sh '''
          curl --user $JENKINS_CLI_USR:$JENKINS_CLI_PSW -XGET http://${bundleName}/${bundleName}/casc-bundle-mgnt/check-bundle-update 
          curl --user $JENKINS_CLI_USR:$JENKINS_CLI_PSW -XPOST http://${bundleName}/${bundleName}/casc-bundle-mgnt/reload-bundle/
        '''
      }
    }
  }
}
