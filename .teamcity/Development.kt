import jetbrains.buildServer.configs.kotlin.v2018_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildTypeSettings
import jetbrains.buildServer.configs.kotlin.v2018_2.Project
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.vcs

object Development : Project({
    name = "Development"

    buildType(Library)
    buildType(Application)
    buildType(TestUI)
    buildType(TestInt)
    buildType(TestReport)

    buildTypesOrder = arrayListOf(Library, Application, TestUI, TestInt, TestReport)
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
        snapshot(TestInt) {}
        snapshot(TestUI) {}
    }
})
//endregion
