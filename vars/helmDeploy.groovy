def call(String name, 
         String imageTag, 
         String namespace = "staging",
         Closure body) {
  def label = "helm-${UUID.randomUUID().toString()}"
  def podYaml = libraryResource 'podtemplates/helm.yml'
  podTemplate(name: 'helm', inheritFrom: 'default-jnlp', label: label, yaml: podYaml, podRetention: never(), activeDeadlineSeconds:1) {
    node(label) {
      body()
      stagingUrl = "https://${name}.staging.workshop.cb-sa.io"
      gitHubDeploy(repoOwner, repo, "", "staging", githubCredentialId, "true", "false")
      container('helm') {
        sh """
          helm upgrade --install -f ./chart/values.yaml --set image.tag=$imageTag --namespace=$namespace  $name ./chart
        """
      }
      gitHubDeployStatus(repoOwner, repo, stagingUrl, 'success', githubCredentialId)
      //only add comment for PRs - CHANGE_ID isn't populated for commits to regular branches
      if (env.CHANGE_ID) {
        def config = [:]
        config.message = "Staging Environment URL: ${stagingUrl}"
        config.credId = githubCredentialId
        config.issueId = env.CHANGE_ID
        config.repoOwner = repoOwner
        config.repo = repo
        gitHubComment(config)
      }
    }
  }
}
