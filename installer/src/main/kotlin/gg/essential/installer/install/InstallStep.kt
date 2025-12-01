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
import gg.essential.elementa.unstable.state.v2.mutableStateOf
import gg.essential.elementa.unstable.state.v2.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * An abstract installation step that can be executed, it takes an input and outputs a Result of some other type.
 *
 * The initial goal for this was to be able to pass data from one step to the other.
 * In practice, this is basically never done, primarily because I realized that I often needed to pass along many different variables, which got messy very quickly...
 *
 * There is no reason to remove the functionality now that it's been made though.
 * Maybe it'll be useful someday.
 */
abstract class InstallStep<I, O>(
    val id: String
) {

    val logger: Logger = LoggerFactory.getLogger("[$id]")

    abstract val numberOfSteps: State<Int>
    abstract val stepsCompleted: State<Int>
    abstract val currentStep: State<InstallStep<*, *>>

    val isCompleted = memo { stepsCompleted() >= numberOfSteps() }

    val result = mutableStateOf<Result<O>?>(null)

    suspend fun start(input: I) {
        val result = execute(input)
        withContext(Dispatchers.Main) {
            this@InstallStep.result.set(result)
        }
    }

    abstract suspend fun execute(input: I): Result<O>

    fun <NO> then(nextStep: InstallStep<O, NO>): InstallStep<I, NO> {
        return ChainedInstallStep(this, nextStep)
    }

    @JvmName("thenIgnoringOutput") // otherwise it has the same jvm signature than the above function
    fun <NO> then(nextStep: OutputInstallStep<NO>): InstallStep<I, NO> {
        return ChainedInstallStep(this.ignoreOutput(), nextStep)
    }

    fun <NI, NO> then(mapper: (O) -> NI, nextStep: InstallStep<NI, NO>): InstallStep<I, NO> {
        return ChainedInstallStep(ChainedInstallStep(this, SingleInstallStep(nextStep.id) { mapper(it) }), nextStep)
    }

    fun <NO> then(id: String, nextStep: suspend InstallStep<*, *>.(O) -> NO): InstallStep<I, NO> {
        return ChainedInstallStep(this, SingleInstallStep(id, nextStep))
    }

    fun ignoreOutput() = then(this.id) {}

}
