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
