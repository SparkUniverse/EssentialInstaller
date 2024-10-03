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
