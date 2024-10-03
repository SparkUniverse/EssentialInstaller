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
