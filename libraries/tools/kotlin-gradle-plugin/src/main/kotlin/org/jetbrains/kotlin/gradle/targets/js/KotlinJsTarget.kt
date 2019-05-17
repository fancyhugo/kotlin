/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator.Companion.runTaskNameSuffix
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator.Companion.testTaskNameSuffix
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsNodeDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinBrowserJs
import org.jetbrains.kotlin.gradle.targets.js.subtargets.KotlinNodeJs
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

class KotlinJsTarget(project: Project, platformType: KotlinPlatformType) :
    KotlinOnlyTarget<KotlinJsCompilation>(project, platformType), KotlinJsTargetDsl {

    val testTaskName = lowerCamelCaseName(disambiguationClassifier, testTaskNameSuffix)
    val testTask get() = project.tasks.maybeCreate(testTaskName)

    val runTaskName = lowerCamelCaseName(disambiguationClassifier, runTaskNameSuffix)
    val runTask get() = project.tasks.maybeCreate(runTaskName)

    private var hasSubtargets = false

    val browser by lazy {
        hasSubtargets = true
        KotlinBrowserJs(this).also {
            it.configure()
        }
    }
    val nodejs by lazy {
        hasSubtargets = true
        KotlinNodeJs(this).also {
            it.configure()
        }
    }

    override fun browser(body: KotlinJsBrowserDsl.() -> Unit) {
        browser.body()
    }

    override fun nodejs(body: KotlinJsNodeDsl.() -> Unit) {
        nodejs.body()
    }

    fun configureDefaults() {
        if (!hasSubtargets) {
            browser()
        }
    }
}