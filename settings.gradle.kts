rootProject.name = "rtl433-data-pipeline"

val localCommonsDir = file("modules/ksb-commons")
if (localCommonsDir.exists()) {
    includeBuild(localCommonsDir)
}