/*
 * Copyright (c) 2023 Lunabee Studio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.configurationcache.extensions.capitalized
import java.net.URI

plugins {
    `maven-publish`
}

project.extensions.configure<PublishingExtension>("publishing") {
    setupMavenRepository()
    setupPublication()
}

/**
 * Set repository destination depending on [project] and version name.
 * Credentials should be stored in your root gradle.properties, in a non source controlled file.
 */
fun PublishingExtension.setupMavenRepository() {
    repositories {
        maven {
            authentication {
                credentials.username = System.getenv(EnvConfig.ENV_ARTIFACTORY_USER)
                    ?: project.properties["artifactory_deployer_release_username"] as? String
                credentials.password = System.getenv(EnvConfig.ENV_ARTIFACTORY_API_KEY)
                    ?: project.properties["artifactory_deployer_release_api_key"] as? String
            }
            url = URI.create("https://artifactory.lunabee.studio/artifactory/double-ratchet-kmm/")
        }
    }
}

/**
 * Entry point for setting publication detail.
 */
fun PublishingExtension.setupPublication() {
    publications {
        create<MavenPublication>(project.name) {
            setProjectDetails()
            setPom()
        }
    }
}

/**
 * Set project details:
 * - groupId will be [ProjectConfig.GROUP_ID]
 * - artifactId will take the name of the current [project]
 * - version will be set in each submodule gradle file
 */
fun MavenPublication.setProjectDetails() {
    groupId = ProjectConfig.GROUP_ID
    artifactId = project.name
    version = project.version.toString()
}

/**
 * Set POM file details.
 */
fun MavenPublication.setPom() {
    pom {
        name.set(project.name.capitalized())
        description.set(project.description)
        url.set(ProjectConfig.LIBRARY_URL)

        scm {
            connection.set("git@github.com:LunabeeStudio/Double_Ratchet_KMM.git")
            developerConnection.set("git@github.com:LunabeeStudio/Double_Ratchet_KMM.git")
            url.set("https://github.com/LunabeeStudio/Double_Ratchet_KMM")
        }

        developers {
            developer {
                id.set("Publisher")
                name.set("Publisher Lunabee")
                email.set("publisher@lunabee.com")
            }
        }

        withXml {
            asNode().appendNode("dependencies").apply {
                fun Dependency.write(scope: String) = appendNode("dependency").apply {
                    appendNode("groupId", group)
                    appendNode("artifactId", name)
                    version?.let { appendNode("version", version) }
                    appendNode("scope", scope)
                }

                configurations["api"].dependencies.forEach { dependency ->
                    dependency.write("implementation")
                }

                configurations["implementation"].dependencies.forEach { dependency ->
                    dependency.write("runtime")
                }
            }
        }
    }
}
