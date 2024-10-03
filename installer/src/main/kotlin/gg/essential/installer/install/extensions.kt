package gg.essential.installer.install

typealias OutputInstallStep<O> = InstallStep<Unit, O>
typealias InputInstallStep<I> = InstallStep<I, Unit>
typealias StandaloneInstallStep = InstallStep<Unit, Unit>

suspend fun <T> InstallStep<Unit, T>.execute() = execute(Unit)

suspend fun <T> InstallStep<Unit, T>.start() = start(Unit)

fun <I, O> installationStep(id: String, function: suspend InstallStep<*, *>.(I) -> O) = SingleInstallStep(id, function)
