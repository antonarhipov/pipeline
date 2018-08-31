import jetbrains.buildServer.configs.kotlin.v2018_1.*
import jetbrains.buildServer.configs.kotlin.v2018_1.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2018_1.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2018.1"

project {

    vcsRoot(ApplicationVcs)
    vcsRoot(LibraryVcs)
    vcsRoot(Pipeline)
    subProjectsOrder = arrayListOf(RelativeId("Development"), RelativeId("Staging"), RelativeId("Live"))

    subProject(Live)
    subProject(Development)
    subProject(Staging)
}

object ApplicationVcs : GitVcsRoot({
    name = "ApplicationVcs"
    url = "https://github.com/antonarhipov/nanoservice"
    authMethod = password {
        userName = "antonarhipov"
        password = "credentialsJSON:372bd256-7c6f-4a5b-ac95-a9b4d3505dd2"
    }
})

object LibraryVcs : GitVcsRoot({
    name = "LibraryVcs"
    url = "https://github.com/antonarhipov/common"
    authMethod = password {
        userName = "antonarhipov"
        password = "credentialsJSON:22dd1f8f-17b2-46af-bc46-38ef2f6955d3"
    }
})

object Pipeline : GitVcsRoot({
    name = "Pipeline"
    url = "https://github.com/antonarhipov/tt-pipeline"
    authMethod = password {
        userName = "antonarhipov"
        password = "credentialsJSON:22dd1f8f-17b2-46af-bc46-38ef2f6955d3"
    }
})


object Development : Project({
    name = "Development"

    buildType(TestUI)
    buildType(TestReport)
    buildType(Application)
    buildType(TestExt)
    buildType(TestOps)
    buildType(Library)
    buildTypesOrder = arrayListOf(Library, Application, TestUI, TestExt, TestOps, TestReport)
})

object Application : BuildType({
    name = "Application"

    vcs {
        root(ApplicationVcs)
    }

    dependencies {
        snapshot(Library) {
        }
    }
})

object Library : BuildType({
    name = "Library"

    vcs {
        root(LibraryVcs)
    }
})

object TestExt : BuildType({
    name = "TestExt"

    dependencies {
        snapshot(Application) {
        }
    }
})

object TestOps : BuildType({
    name = "TestOps"

    dependencies {
        snapshot(Application) {
        }
    }
})

object TestReport : BuildType({
    name = "TestReport"

    type = BuildTypeSettings.Type.COMPOSITE

    vcs {
        showDependenciesChanges = true
    }

    triggers {
        vcs {
            branchFilter = ""
            watchChangesInDependencies = true
        }
    }

    dependencies {
        snapshot(TestExt) {
        }
        snapshot(TestOps) {
        }
        snapshot(TestUI) {
        }
    }
})

object TestUI : BuildType({
    name = "TestUI"

    dependencies {
        snapshot(Application) {
        }
    }
})


object Live : Project({
    name = "Live"

    buildType(MakePublic)
    buildTypesOrder = arrayListOf(MakePublic)
})

object MakePublic : BuildType({
    name = "MakePublic"

    enablePersonalBuilds = false
    type = BuildTypeSettings.Type.DEPLOYMENT
    maxRunningBuilds = 1

    dependencies {
        snapshot(TestApplication) {
        }
    }
})


object Staging : Project({
    name = "Staging"

    buildType(Docker)
    buildType(TestApplication)
    buildTypesOrder = arrayListOf(Docker, TestApplication)
})

object Docker : BuildType({
    name = "Docker"

    vcs {
        root(ApplicationVcs)
    }

    dependencies {
        snapshot(TestReport) {
            reuseBuilds = ReuseBuilds.ANY
        }
    }
})

object TestApplication : BuildType({
    name = "TestApplication"

    triggers {
        vcs {
            triggerRules = "+:root=${ApplicationVcs.id}:Dockerfile"

            branchFilter = ""
            watchChangesInDependencies = true
        }
    }

    dependencies {
        snapshot(Docker) {
        }
    }
})
