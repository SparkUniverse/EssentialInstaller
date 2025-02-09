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

import gg.essential.elementa.state.v2.memo
import gg.essential.installer.logging.Logging

/**
 * An installation containing many steps
 */
open class Installation(
    private val steps: List<InstallStep<Unit, *>>
) : InstallStep<Unit, Unit>("installation") {

    override val numberOfSteps = memo { steps.sumOf { it.numberOfSteps() } }

    override val stepsCompleted = memo { steps.sumOf { it.stepsCompleted() } }

    override val currentStep = memo { steps.firstOrNull { it.isCompleted().not() }?.currentStep() ?: steps.last().currentStep() }

    override suspend fun execute(input: Unit): Result<Unit> {
        for (step in steps) {
            try {
                step.execute().onFailure {
                    Logging.logger.error("Error status when running {}:{}", this@Installation.id, step.id)
                    Logging.logger.error("Exception provided in error:", it)
                    return Result.failure(it)
                }
            } catch (e: Exception) {
                Logging.logger.error("Error when running {}:{}", this@Installation.id, step.id)
                Logging.logger.error("Exception caught:", e)
                return Result.failure(e)
            }
        }
        return Result.success(Unit)
    }

}
