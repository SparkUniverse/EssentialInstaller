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

import gg.essential.elementa.unstable.state.v2.State
import gg.essential.elementa.unstable.state.v2.combinators.map
import gg.essential.elementa.unstable.state.v2.mutableStateOf
import gg.essential.elementa.unstable.state.v2.stateOf
import gg.essential.installer.isDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * A singular installation step
 */
class SingleInstallStep<I, O>(
    id: String,
    private val function: suspend SingleInstallStep<I, O>.(I) -> O,
) : InstallStep<I, O>(id) {

    private val completed = mutableStateOf(false)

    override val numberOfSteps: State<Int> = stateOf(1)
    override val stepsCompleted = completed.map { if (it) 1 else 0 }
    override val currentStep = stateOf(this)

    override suspend fun execute(input: I): Result<O> {
        try {
            val result = function(input)
            // This was left at 1000 accidentally from testing,
            // but I think some delay makes the process look nicer if there are no downloads to do.
            // Instead of an instant flash to install finished
            // Disable it in debug mode, otherwise modes like install everything take way too long
            if (!isDebug()) {
                delay(250)
            }
            withContext(Dispatchers.Main) {
                completed.set(true)
            }
            return Result.success(result)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

}
