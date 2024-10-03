package gg.essential.installer.launcher.prism

import java.time.Instant

data class InstanceConfig(
    val name: String,
    val iconKey: String,
    val lastTimePlayed: Instant,
)
