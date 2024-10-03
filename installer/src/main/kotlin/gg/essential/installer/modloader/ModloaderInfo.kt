package gg.essential.installer.modloader

import gg.essential.installer.minecraft.MCVersion

data class ModloaderInfo(
    val type: ModloaderType,
    val version: ModloaderVersion,
) {

    constructor(
        type: ModloaderType,
        fullVersion: String,
        providedNumericVersion: String? = null,
    ) : this(type, ModloaderVersion.fromVersion(type, fullVersion, providedNumericVersion))

    companion object {

        fun fromVersionString(version: String, numericVersion: String? = null): ModloaderInfo {
            return when {
                // Neoforge before forge, because, well, forge would match both otherwise
                version.contains("neoforge", ignoreCase = true) -> {
                    ModloaderInfo(ModloaderType.NEOFORGE, version, numericVersion)
                }

                version.contains("forge", ignoreCase = true) -> {
                    ModloaderInfo(ModloaderType.FORGE, version, numericVersion)
                }

                version.contains("fabric", ignoreCase = true) -> {
                    ModloaderInfo(ModloaderType.FABRIC, version, numericVersion)
                }

                version.contains("quilt", ignoreCase = true) -> {
                    ModloaderInfo(ModloaderType.QUILT, version, numericVersion)
                }

                MCVersion.SNAPSHOT_PATTERN.matcher(version).find() -> ModloaderInfo(ModloaderType.NONE_SNAPSHOT, version)
                MCVersion.BETA_PATTERN.matcher(version).find() -> ModloaderInfo(ModloaderType.NONE_BETA, version)
                MCVersion.ALPHA_PATTERN.matcher(version).find() -> ModloaderInfo(ModloaderType.NONE_ALPHA, version)

                MCVersion.fromString(version) != null -> ModloaderInfo(ModloaderType.NONE_MODERN, version)

                else -> ModloaderInfo(ModloaderType.UNKNOWN, version)
            }
        }
    }

}
