package gg.essential.installer.install

import gg.essential.elementa.state.v2.memo

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
