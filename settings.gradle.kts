rootProject.name = "DevOps"

// single module that builds all Java sources under app/Backend
include(":app:Backend")
project(":app:Backend").projectDir = file("app/Backend")