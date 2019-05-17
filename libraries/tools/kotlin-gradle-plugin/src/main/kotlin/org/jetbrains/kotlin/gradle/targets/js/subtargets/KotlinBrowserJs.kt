/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.subtargets

import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.nodeJs
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.tasks.createOrRegisterTask

class KotlinBrowserJs(target: KotlinJsTarget) :
    KotlinJsSubTarget(target, "browser"),
    KotlinJsBrowserDsl {

    private val versions = project.nodeJs.versions
    private val webpackTaskName = disambiguateCamelCased("webpack")

    override fun configureDefaultTestFramework(it: KotlinJsTest) {
        it.useKarma {
            useWebpack()
            useChromeHeadless()
        }
    }

    override fun runTask(body: KotlinWebpack.() -> Unit) {
        (project.tasks.getByName(runTaskName) as KotlinWebpack).body()
    }

    override fun webpackTask(body: KotlinWebpack.() -> Unit) {
        (project.tasks.getByName(webpackTaskName) as KotlinWebpack).body()
    }

    override fun configureRun(compilation: KotlinJsCompilation) {
        val project = compilation.target.project
        val npmProject = project.npmProject

        project.createOrRegisterTask<KotlinWebpack>(disambiguateCamelCased("webpack")) {
            val compileKotlinTask = compilation.compileKotlinTask
            it.dependsOn(target.npmResolveTask, compileKotlinTask)

            it.entry = npmProject.compileOutput(compileKotlinTask)

            project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(it)
        }

        project.createOrRegisterTask<KotlinWebpack>(disambiguateCamelCased("run")) {
            val compileKotlinTask = compilation.compileKotlinTask
            it.dependsOn(target.npmResolveTask, compileKotlinTask)

            it.bin = "webpack-dev-server"
            it.entry = npmProject.compileOutput(compileKotlinTask)

            val projectDir = target.project.projectDir.canonicalPath
            it.devServer = KotlinWebpackConfig.DevServer(
                contentBase = listOf("$projectDir/src/main/resources")
            )

            it.outputs.upToDateWhen { false }
            target.runTask.dependsOn(it)
        }
    }
}