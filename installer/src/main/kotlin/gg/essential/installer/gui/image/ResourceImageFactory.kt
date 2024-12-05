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

package gg.essential.installer.gui.image

import gg.essential.elementa.components.UIImage
import gg.essential.elementa.utils.ResourceCache

// Copied from Essential

/**
 * An ImageFactory that returns images resulting from the specified [resource].
 * Utilizes a [ResourceCache] to decrease loading time
 */
class ResourceImageFactory(
    private val resource: String,
    preload: Boolean = true
) : ImageFactory() {

    init {
        if (preload) {
            generate()
        }
    }

    override fun generate(): UIImage {
        return UIImage.ofResourceCached(resource, cache)
    }

    private companion object {

        val cache: ResourceCache = ResourceCache(Int.MAX_VALUE)

    }
}
