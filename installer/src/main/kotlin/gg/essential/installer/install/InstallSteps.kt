/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.’s Essential Installer repository
 * and is protected under copyright registration #TX0009446119. For the
 * full license, see:
 * https://github.com/EssentialGG/EssentialInstaller/blob/main/LICENSE.
 *
 * You may modify, create, fork, and use new versions of our Essential
 * Installer mod in accordance with the GPL-3 License and the additional
 * provisions outlined in the LICENSE file. You may not sell, license,
 * commercialize, or otherwise exploit the works in this file or any
 * other in this repository, all of which is reserved by Essential.
 */

package gg.essential.installer.install

/**
 * A collections of InstallStep-s, one per stage.
 */
data class InstallSteps(
    val prepareStep: StandaloneInstallStep? = null,
    val downloadStep: StandaloneInstallStep? = null,
    val installStep: StandaloneInstallStep? = null,
) {

    /**
     * Converts this InstallSteps into a single InstallStep, to be able to be run directly.
     *
     * It executes each of the three steps in order
     */
    fun convertToSingleInstallStep(): StandaloneInstallStep {
        return Installation(
            listOfNotNull(
                prepareStep,
                downloadStep,
                installStep
            )
        )
    }

    companion object {
        /**
         * Merges multiple InstallSteps into one.
         * This means merging all the steps in each of the stages separately in their respective order.
         */
        fun merge(vararg steps: InstallSteps): InstallSteps {
            val prepareSteps = mutableListOf<StandaloneInstallStep>()
            val downloadSteps = mutableListOf<StandaloneInstallStep>()
            val installSteps = mutableListOf<StandaloneInstallStep>()
            for (step in steps) {
                if (step.prepareStep != null) {
                    prepareSteps.add(step.prepareStep)
                }
                if (step.downloadStep != null) {
                    downloadSteps.add(step.downloadStep)
                }
                if (step.installStep != null) {
                    installSteps.add(step.installStep)
                }
            }
            val prepareInstallation = Installation(prepareSteps)
            val downloadInstallation = Installation(downloadSteps)
            val installInstallation = Installation(installSteps)

            return InstallSteps(
                prepareInstallation,
                downloadInstallation,
                installInstallation,
            )
        }
    }
}

