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

import gg.essential.elementa.state.v2.stateOf

/**
 * Simple installation step that always returns the provided error.
 *
 * Useful in cases where a function needs to return an installation step, but it errors/cannot
 */
class ErrorInstallStep<I, O>(
    private val exception: Exception,
) : InstallStep<I, O>("error") {

    override val numberOfSteps = stateOf(1)
    override val stepsCompleted = stateOf(1)
    override val currentStep = stateOf(this)

    override suspend fun execute(input: I): Result<O> = Result.failure(exception)

}
