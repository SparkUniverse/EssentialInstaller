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

import gg.essential.elementa.unstable.state.v2.memo

/**
 * Allows an easy way to chain installation steps one after another, used by the `then` function on InstallationStep
 */
class ChainedInstallStep<I, T, O>(
    private val first: InstallStep<I, T>,
    private val second: InstallStep<T, O>,
) : InstallStep<I, O>("chained ${first.id}; ${second.id}") {

    override val numberOfSteps = memo { first.numberOfSteps() + second.numberOfSteps() }

    override val stepsCompleted = memo { first.stepsCompleted() + second.stepsCompleted() }

    override val currentStep = memo { if (first.isCompleted()) second.currentStep() else first.currentStep() }

    override suspend fun execute(input: I): Result<O> {
        return first.execute(input).fold({ second.execute(it) }, { return Result.failure(it) })
    }

}
