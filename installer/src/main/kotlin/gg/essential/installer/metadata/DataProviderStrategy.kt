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

import kotlinx.serialization.Serializable

/**
 * An enum containing all the possible strategies to use when getting mod data.
 */
@Serializable
enum class DataProviderStrategy {

    /**
     * Only use the fallback providers if the currently selected provider throws an error.
     * This can either be the primary provider, a provider with a higher priority than others.
     */
    ONLY_IF_ERROR,
    /**
     * Always use fallback data if other higher priority providers do not have the data requested.
     */
    COMBINE_WITH_PRIORITY

}
