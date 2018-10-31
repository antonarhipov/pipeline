import jetbrains.buildServer.configs.kotlin.v2018_1.*
import jetbrains.buildServer.configs.kotlin.v2018_1.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2018_1.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2018_1.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.v2018_1.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2018_1.vcs.GitVcsRoot

version = "2018.1"

project {
    //region roots
    vcsRoot(ApplicationVcs)
    vcsRoot(ExtTestsVcs)
    vcsRoot(LibraryVcs)
    vcsRoot(IntegrationTestsVcs)
    vcsRoot(UiTestsVcs)
    //endregion

    subProject(Development)
    subProject(Staging)
    subProject(Live)

    //region ordering
    subProjectsOrder = arrayListOf(RelativeId("Development"), RelativeId("Staging"), RelativeId("Live"))
    //endregion
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

//endregion

//region staging
object Staging : Project ({
    id("Staging")
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
            reuseBuilds = ReuseBuilds.SUCCESSFUL
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
object Live : Project ({
    id("Live")
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

//endregion