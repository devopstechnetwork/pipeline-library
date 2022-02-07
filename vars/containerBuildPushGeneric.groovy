// vars/containerBuildPushGeneric.groovy
def call(String imageName, String imageTag, String containerRegistry = "us-east1-docker.pkg.dev/core-workshop/workshop-registry", Closure body) {
  def label = "kaniko-${UUID.randomUUID().toString()}"
  def podYaml = libraryResource 'podtemplates/kaniko.yml'
  def customBuildArg = ""
  podTemplate(name: 'kaniko', inheritFrom: 'default-jnlp', label: label, yaml: podYaml, podRetention: never(), activeDeadlineSeconds:1) {
    node(label) {
      body()
      def tag = imageTag ?: env.SHORT_COMMIT
      if(env.EVENT_BASE_IMAGE_TAG) {
        customBuildArg = "--build-arg BASE_IMAGE_TAG=${env.EVENT_BASE_IMAGE_TAG}"
      }
      imageName = imageName.toLowerCase()
      container(name: 'kaniko', shell: '/busybox/sh') {
        withEnv(['PATH+EXTRA=/busybox:/kaniko']) {
          sh label: "kaniko build and push container image", script: """#!/busybox/sh
            /kaniko/docker-credential-gcr configure-docker --registries=${containerRegistry}
            /kaniko/executor -f ${pwd()}/Dockerfile -c ${pwd()} ${customBuildArg} --build-arg commitAuthor='${COMMIT_AUTHOR}' --build-arg shortCommit=${env.SHORT_COMMIT} --cache=true -d ${containerRegistry}/${imageName}:${tag}
          """
        }
      }
    }
  }
}
