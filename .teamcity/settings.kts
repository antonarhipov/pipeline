import jetbrains.buildServer.configs.kotlin.v2018_1.*
import jetbrains.buildServer.configs.kotlin.v2018_1.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2018_1.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2018_1.triggers.finishBuildTrigger
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
    vcsRoot(ExtTestsVcs)
    vcsRoot(LibraryVcs)
    vcsRoot(IntegrationTestsVcs)
    vcsRoot(UiTestsVcs)

    subProject(Development)
//    subProject(staging)
//    subProject(live)

    subProjectsOrder = arrayListOf(RelativeId("Development"), RelativeId("Staging"), RelativeId("Live"))
}

//region VCS roots
object ApplicationVcs : GitVcsRoot({
    name = "ApplicationVcs"
    url = "http://localhost:3000/anton/application.git"
    branchSpec = """
        +:refs/heads/(master)
        +:refs/heads/(feature*)
    """.trimIndent()
})

object ExtTestsVcs : GitVcsRoot({
    name = "ExtTestsVcs"
    url = "http://localhost:3000/anton/extra-tests.git"
    branchSpec = """
        +:refs/heads/(master)
        +:refs/heads/(feature*)
    """.trimIndent()
})

object IntegrationTestsVcs : GitVcsRoot({
    name = "IntegrationTestsVcs"
    url = "http://localhost:3000/anton/integration-tests.git"
    branchSpec = """
        +:refs/heads/(master)
        +:refs/heads/(feature*)
    """.trimIndent()
})

object LibraryVcs : GitVcsRoot({
    name = "LibraryVcs"
    url = "http://localhost:3000/anton/library.git"
    branchSpec = """
        +:refs/heads/(master)
        +:refs/heads/(feature*)
    """.trimIndent()
})

object UiTestsVcs : GitVcsRoot({
    name = "UiTestsVcs"
    url = "http://localhost:3000/anton/ui-tests.git"
    branchSpec = """
        +:refs/heads/(master)
        +:refs/heads/(feature*)
    """.trimIndent()
})
//endregion

//region development
object Development : Project({
    name = "Development"

    buildType(Library)
    buildType(Application)
    buildType(TestUI)
    buildType(TestExt)
    buildType(TestInt)
    buildType(TestReport)

    buildTypesOrder = arrayListOf(Library, Application, TestUI, TestExt, TestInt, TestReport)
})

//region Application
object Application : BuildType({
    name = "Application"

    artifactRules = """
        application-*.jar
        target/application-1.0-SNAPSHOT.jar
    """.trimIndent()

    vcs {
        root(ApplicationVcs)
    }

    steps {
        maven {
            goals = "clean package"
        }
    }

    dependencies {
        dependency(Library) {
            snapshot {
            }

            artifacts {
                artifactRules = "library-*.jar"
            }
        }
    }
})
//endregion

//region Library
object Library : BuildType({
    name = "Library"

    artifactRules = "target/library-*.jar"

    vcs {
        root(LibraryVcs)
    }

    steps {
        maven {
            goals = "clean package"
        }
    }
})
//endregion

//region TestExt
object TestExt : BuildType({
    name = "TestExt"

    vcs {
        root(ExtTestsVcs)
    }

    steps {
        maven {
            goals = "clean test"
        }
    }

    dependencies {
        dependency(Application) {
            snapshot {
            }

            artifacts {
                artifactRules = "application-*.jar"
            }
        }
    }
})
//endregion

//region TestInt
object TestInt : BuildType({
    name = "TestInt"

    vcs {
        root(IntegrationTestsVcs)
    }

    steps {
        maven {
            goals = "clean test"
        }
    }

    dependencies {
        dependency(Application) {
            snapshot {
            }

            artifacts {
                artifactRules = "application-*.jar"
            }
        }
    }
})
//endregion

//region TestReport
object TestReport : BuildType({
    name = "TestReport"

    type = BuildTypeSettings.Type.COMPOSITE

    vcs {
        showDependenciesChanges = true
    }

    triggers {
        vcs {
            watchChangesInDependencies = true
        }
    }

    dependencies {
        snapshot(TestExt) {}
        snapshot(TestInt) {}
        snapshot(TestUI) {}
    }
})
//endregion

//region TestUI
object TestUI : BuildType({
    name = "TestUI"

    vcs {
        root(UiTestsVcs)
    }

    steps {
        maven {
            goals = "clean test"
        }
    }

    dependencies {
        dependency(Application) {
            snapshot {
            }

            artifacts {
                artifactRules = "application-*.jar"
            }
        }
    }
})
//endregion
//endregion

//region staging
val staging = Project {
    id("Staging")
    name = "Staging"

//    buildType(Docker)
//    buildType(TestApplication)
    buildTypesOrder = arrayListOf(Docker, TestApplication)
}

object Docker : BuildType({
    name = "Docker"

    vcs {
        root(ApplicationVcs)
    }

    steps {
        script {
            scriptContent = """echo "building Docker image" %build.number%"""
        }
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
        finishBuildTrigger {
            buildTypeExtId = "${Docker.id}"
            branchFilter = "+:*"
        }
    }

    dependencies {
        snapshot(Docker) {
        }
    }
})
//endregion

//region live
val live = Project {
    id("Live")
    name = "Live"

    buildType(MakePublic)
    buildTypesOrder = arrayListOf(MakePublic)
}

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

//endregion