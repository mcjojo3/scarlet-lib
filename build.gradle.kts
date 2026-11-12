// Scarlet Lib: modern vanilla mechanics for b1.7.3 (StationAPI) that any mod can build on.
// Builds on its own, or as a subproject of a mod that includes it from source (CreRaces, Twilight Lib).
plugins {
	id("maven-publish")
	id("fabric-loom")
	id("babric-loom-extension")
}

fun prop(name: String): String = providers.gradleProperty(name).get()

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

base.archivesName = "scarlet-lib"
// The version lives in fabric.mod.json, so IDE runs (which copy resources as-is) show it too.
version = (groovy.json.JsonSlurper().parse(file("src/main/resources/fabric.mod.json")) as Map<*, *>)["version"] as String
group = "mc.sayda.scarlet_lib"

repositories {
	maven("https://maven.glass-launcher.net/snapshots/")
	maven("https://maven.glass-launcher.net/releases/")
	maven("https://maven.glass-launcher.net/babric")
	maven("https://maven.minecraftforge.net/")
	maven("https://jitpack.io/")
	mavenCentral()
	exclusiveContent {
		forRepository {
			maven("https://api.modrinth.com/maven")
		}
		filter {
			includeGroup("maven.modrinth")
		}
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("minecraft_version")}")
	mappings("net.glasslauncher:biny:${prop("yarn_mappings")}:v2")
	modImplementation("net.fabricmc:fabric-loader:${prop("loader_version")}")

	implementation("org.apache.logging.log4j:log4j-core:2.17.2")
	implementation("org.slf4j:slf4j-api:1.8.0-beta4")
	implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.17.1")
	implementation("org.jetbrains:annotations:23.0.0")

	modImplementation("net.modificationstation:StationAPI:${prop("stationapi_version")}")

	// Optional: Retro Commands runs the commands when installed (compat/retrocommands). Its jar is
	// Java 21 bytecode, so it is only compiled against here, never shipped or required.
	modCompileOnly("maven.modrinth:retrocommands:0.7.5+babric") // https://modrinth.com/mod/retrocommands
}

configurations.all {
	exclude("babric")
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
}

java {
	withSourcesJar()
}

tasks.withType<GenerateModuleMetadata> {
	enabled = false
}

publishing {
	repositories {
		mavenLocal()
	}

	publications {
		register("mavenJava", MavenPublication::class) {
			artifactId = "scarlet-lib"
			from(components["java"])
		}
	}
}
