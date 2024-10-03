package gg.essential.installer.install

import gg.essential.elementa.state.v2.State
import gg.essential.elementa.state.v2.combinators.map
import gg.essential.elementa.state.v2.mutableStateOf
import gg.essential.elementa.state.v2.stateOf
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
            delay(250)
            withContext(Dispatchers.Main) {
                completed.set(true)
            }
            return Result.success(result)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

}
