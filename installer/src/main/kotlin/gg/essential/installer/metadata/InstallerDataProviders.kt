package gg.essential.installer.metadata

import gg.essential.installer.metadata.provider.InstallerMetadataProvider
import gg.essential.installer.metadata.provider.ModMetadataProvider
import gg.essential.installer.metadata.provider.ModVersionProvider
import gg.essential.installer.metadata.provider.ModloaderMetadataProvider
import kotlinx.serialization.Serializable

@Serializable
data class InstallerDataProviders(
    val modVersionProviderStrategy: DataProviderStrategy = DataProviderStrategy.COMBINE_WITH_PRIORITY,
    val modMetadataProviderStrategy: DataProviderStrategy = DataProviderStrategy.COMBINE_WITH_PRIORITY,
    val modloaderMetadataProviderStrategy: DataProviderStrategy = DataProviderStrategy.COMBINE_WITH_PRIORITY,
    val installerMetadataProviders: List<InstallerMetadataProvider> = listOf(),
    val modVersionProviders: List<ModVersionProvider> = listOf(),
    val modMetadataProviders: List<ModMetadataProvider> = listOf(),
    val modloaderMetadataProviders: List<ModloaderMetadataProvider> = listOf(),
)
