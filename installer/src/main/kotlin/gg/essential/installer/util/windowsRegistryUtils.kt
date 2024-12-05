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

package gg.essential.installer.util

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import java.util.*

fun isRegistryDisplayNameInWindowsRegistry(displayName: String, vararg registryPaths: RegistryPath): Boolean {
    return getRegistryKeyViaDisplayNameInWindowsRegistry(displayName, *registryPaths) != null
}

fun isRegistryDisplayNameInWindowsRegistry(registryPaths: Set<RegistryPath>, check: (String) -> Boolean): Boolean {
    return getRegistryKeyViaDisplayNameInWindowsRegistry(registryPaths, check) != null
}

fun getRegistryKeyViaDisplayNameInWindowsRegistry(displayName: String, vararg registryPaths: RegistryPath): Pair<RegistryPath, TreeMap<String, Any>>? {
    return getRegistryKeyViaDisplayNameInWindowsRegistry(setOf(*registryPaths)) {
        it.equals(displayName, true)
    }
}

fun getRegistryKeyViaDisplayNameInWindowsRegistry(registryPaths: Set<RegistryPath>, check: (String) -> Boolean): Pair<RegistryPath, TreeMap<String, Any>>? {
    return queryWindowsRegistry(registryPaths) { _, registryValues ->
        if (registryValues.containsKey("DisplayName")) {
            val registryDisplayName = registryValues["DisplayName"]
            if (registryDisplayName is String) {
                if (check(registryDisplayName)) {
                    return@queryWindowsRegistry true
                }
            }
        }
        false
    }
}

fun queryWindowsRegistry(registryPaths: Set<RegistryPath>, predicate: (RegistryPath, TreeMap<String, Any>) -> Boolean): Pair<RegistryPath, TreeMap<String, Any>>? {
    for (registryPath in registryPaths) {
        val registryKeys = Advapi32Util.registryGetKeys(registryPath.root, registryPath.keyPath)
        for (registryKey in registryKeys) {
            val registryKeyPath = RegistryPath(registryPath.root, registryPath.keyPath + "\\" + registryKey)
            val registryValues = Advapi32Util.registryGetValues(registryKeyPath.root, registryKeyPath.keyPath)
            if (predicate(registryKeyPath, registryValues)) {
                return registryKeyPath to registryValues
            }
        }
    }
    return null
}

data class RegistryPath(
    val root: WinReg.HKEY,
    val keyPath: String
)
