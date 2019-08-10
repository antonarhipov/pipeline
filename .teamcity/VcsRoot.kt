import jetbrains.buildServer.configs.kotlin.v2018_2.vcs.GitVcsRoot

//region VCS roots
object ApplicationVcs : GitVcsRoot({
    name = "ApplicationVcs"
    url = "https://github.com/antonarhipov/pipeline-application.git"
    branchSpec = """
        +:refs/heads/(master)
        +:refs/heads/(feature*)
    """.trimIndent()
})

object IntegrationTestsVcs : GitVcsRoot({
    name = "IntegrationTestsVcs"
    url = "https://github.com/antonarhipov/pipeline-integration-tests.git"
    branchSpec = """
        +:refs/heads/(master)
        +:refs/heads/(feature*)
    """.trimIndent()
})

object LibraryVcs : GitVcsRoot({
    name = "LibraryVcs"
    url = "https://github.com/antonarhipov/pipeline-library.git"
    branchSpec = """
        +:refs/heads/(master)
        +:refs/heads/(feature*)
    """.trimIndent()
})

object UiTestsVcs : GitVcsRoot({
    name = "UiTestsVcs"
    url = "https://github.com/antonarhipov/pipeline-ui-tests.git"
    branchSpec = """
        +:refs/heads/(master)
        +:refs/heads/(feature*)
    """.trimIndent()
})
//endregion
