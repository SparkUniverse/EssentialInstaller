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
