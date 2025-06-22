import java.util.Properties

val localProps = Properties().apply {
    val file = rootProject.file("private.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun getProp(key: String): String? = localProps.getProperty(key)


plugins.withId("com.android.library") {
    afterEvaluate {
        extensions.findByType(PublishingExtension::class.java)?.apply {
            // 避免重复创建
            if (publications.findByName("release") == null) {
                publications.create("release", MavenPublication::class.java) {
                    groupId = "com.kernelflux.ktoolbox"
                    artifactId = project.name.substringAfterLast("-")

                    version = if (project.hasProperty("ktoolbox.version")) {
                        project.property("ktoolbox.version").toString()
                    } else {
                        rootProject.findProperty("ktoolbox.version")?.toString() ?: "1.0.0"
                    }

                    val releaseComponent = components.findByName("release")
                    if (releaseComponent != null) {
                        from(releaseComponent)
                        println("✅ release component found in ${project.name}")
                    } else {
                        logger.warn("⚠️ release component not found in ${project.name}")
                    }
                }
            }

            repositories {
                val gprUser = getProp("gpr.user") ?: ""
                val gprPass = getProp("gpr.key") ?: ""
                println("gprUser:$gprUser,\ngprPass:$gprPass")
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/kernelflux/KToolBox")
                    credentials {
                        username = gprUser
                        password = gprPass
                    }
                }
            }
        }
    }
}