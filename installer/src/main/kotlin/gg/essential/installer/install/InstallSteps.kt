package gg.essential.installer.install

/**
 * A collections of InstallStep-s, one per stage.
 */
data class InstallSteps(
    val prepareStep: StandaloneInstallStep? = null,
    val downloadStep: StandaloneInstallStep? = null,
    val installStep: StandaloneInstallStep? = null,
) {

    /**
     * Converts this InstallSteps into a single InstallStep, to be able to be run directly.
     *
     * It executes each of the three steps in order
     */
    fun convertToSingleInstallStep(): StandaloneInstallStep {
        return Installation(
            listOfNotNull(
                prepareStep,
                downloadStep,
                installStep
            )
        )
    }

    companion object {
        /**
         * Merges multiple InstallSteps into one.
         * This means merging all the steps in each of the stages separately in their respective order.
         */
        fun merge(vararg steps: InstallSteps): InstallSteps {
            val prepareSteps = mutableListOf<StandaloneInstallStep>()
            val downloadSteps = mutableListOf<StandaloneInstallStep>()
            val installSteps = mutableListOf<StandaloneInstallStep>()
            for (step in steps) {
                if (step.prepareStep != null) {
                    prepareSteps.add(step.prepareStep)
                }
                if (step.downloadStep != null) {
                    downloadSteps.add(step.downloadStep)
                }
                if (step.installStep != null) {
                    installSteps.add(step.installStep)
                }
            }
            val prepareInstallation = Installation(prepareSteps)
            val downloadInstallation = Installation(downloadSteps)
            val installInstallation = Installation(installSteps)

            return InstallSteps(
                prepareInstallation,
                downloadInstallation,
                installInstallation,
            )
        }
    }
}

