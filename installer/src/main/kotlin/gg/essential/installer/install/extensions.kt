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

package gg.essential.installer.install

import gg.essential.installer.download.DownloadStep
import gg.essential.installer.download.util.DownloadInfo
import io.ktor.client.request.*
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

typealias OutputInstallStep<O> = InstallStep<Unit, O>
typealias InputInstallStep<I> = InstallStep<I, Unit>
typealias StandaloneInstallStep = InstallStep<Unit, Unit>

suspend fun <T> InstallStep<Unit, T>.execute() = execute(Unit)

suspend fun <T> InstallStep<Unit, T>.start() = start(Unit)

fun <I, O> installationStep(id: String, function: suspend InstallStep<*, *>.(I) -> O) = SingleInstallStep(id, function)

fun <T> InstallStep<T, DownloadInfo>.download(
    name: String,
    path: Path,
    openOptions: List<OpenOption> = listOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING),
    block: HttpRequestBuilder.() -> Unit = {},
) = ChainedInstallStep(this, DownloadStep(name, path, openOptions, block))

fun downloadRequest(
    downloadInfo: DownloadInfo,
    path: Path,
    openOptions: List<OpenOption> = listOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING),
    block: HttpRequestBuilder.() -> Unit = {},
) = installationStep<Unit, DownloadInfo>("Downloading ${downloadInfo.name}") { downloadInfo }.download(downloadInfo.name, path, openOptions, block)

fun downloadRequest(
    name: String,
    downloadInfoSupplier: suspend () -> DownloadInfo,
    path: Path,
    openOptions: List<OpenOption> = listOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING),
    block: HttpRequestBuilder.() -> Unit = {},
) = installationStep<Unit, DownloadInfo>("Downloading $name") { downloadInfoSupplier() }.download(name, path, openOptions, block)
