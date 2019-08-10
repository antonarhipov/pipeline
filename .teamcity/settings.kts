import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2018_2.vcs.GitVcsRoot

version = "2018.2"

project {
    //region roots
    vcsRoot(ApplicationVcs)
    vcsRoot(LibraryVcs)
    vcsRoot(IntegrationTestsVcs)
    vcsRoot(UiTestsVcs)
    //endregion

    subProject(Development)
    subProject(Staging)
    subProject(Live)

    subProjectsOrder = arrayListOf(Development.id!!, Staging.id!!, Live.id!!)

}
