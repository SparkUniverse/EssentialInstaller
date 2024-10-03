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
