import jetbrains.buildServer.configs.kotlin.v2018_1.*
import jetbrains.buildServer.configs.kotlin.v2018_1.buildFeatures.merge
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
    subProjectsOrder = arrayListOf(RelativeId("Development"), RelativeId("Staging"), RelativeId("Live"))

    subProject(Live)
    subProject(Development)
    subProject(Staging)
}

object ApplicationVcs : GitVcsRoot({
    name = "ApplicationVcs"
    url = "http://localhost:3000/anton/application.git"
    branchSpec = """
        +:refs/heads/(master)
        +:refs/heads/(feature*)
    """.trimIndent()
    authMethod = password {
        userName = "antonarhipov"
        password = "credentialsJSON:372bd256-7c6f-4a5b-ac95-a9b4d3505dd2"
    }
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
    authMethod = password {
        userName = "anton"
        password = "credentialsJSON:c77eb248-b6ac-41b0-8b4c-50e67d8681fc"
    }
})

object LibraryVcs : GitVcsRoot({
    name = "LibraryVcs"
    url = "http://localhost:3000/anton/library.git"
    branchSpec = """
        +:refs/heads/(master)
        +:refs/heads/(feature*)
    """.trimIndent()
    authMethod = password {
        userName = "anton"
        password = "credentialsJSON:c77eb248-b6ac-41b0-8b4c-50e67d8681fc"
    }
})

object UiTestsVcs : GitVcsRoot({
    name = "UiTestsVcs"
    url = "http://localhost:3000/anton/ui-tests.git"
    branchSpec = """
        +:refs/heads/(master)
        +:refs/heads/(feature*)
    """.trimIndent()
    authMethod = password {
        userName = "anton"
        password = "credentialsJSON:c77eb248-b6ac-41b0-8b4c-50e67d8681fc"
    }
})


object Development : Project({
    name = "Development"

    buildType(TestUI)
    buildType(TestReport)
    buildType(TestInt)
    buildType(Application)
    buildType(TestExt)
    buildType(Library)
    buildTypesOrder = arrayListOf(Library, Application, TestUI, TestExt, TestInt, TestReport)
})

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
            runnerArgs = "-Dmaven.test.failure.ignore=true"
            mavenVersion = defaultProvidedVersion()
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

object Library : BuildType({
    name = "Library"

    artifactRules = "target/library-*.jar"

    vcs {
        root(LibraryVcs)
    }

    steps {
        maven {
            goals = "clean package"
            runnerArgs = "-Dmaven.test.failure.ignore=true"
            mavenVersion = defaultProvidedVersion()
        }
    }

    features {
        merge {
            branchFilter = "+:feature*"
        }
    }
})

object TestExt : BuildType({
    name = "TestExt"

    vcs {
        root(ExtTestsVcs)
    }

    steps {
        maven {
            goals = "clean test"
            runnerArgs = "-Dmaven.test.failure.ignore=true"
            mavenVersion = defaultProvidedVersion()
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

object TestInt : BuildType({
    name = "TestInt"

    vcs {
        root(IntegrationTestsVcs)
    }

    steps {
        maven {
            goals = "clean test"
            runnerArgs = "-Dmaven.test.failure.ignore=true"
            mavenVersion = defaultProvidedVersion()
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
        snapshot(TestInt) {
        }
        snapshot(TestUI) {
        }
    }
})

object TestUI : BuildType({
    name = "TestUI"

    vcs {
        root(UiTestsVcs)
    }

    steps {
        maven {
            goals = "clean test"
            runnerArgs = "-Dmaven.test.failure.ignore=true"
            mavenVersion = defaultProvidedVersion()
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
