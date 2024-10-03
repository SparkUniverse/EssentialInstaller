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
