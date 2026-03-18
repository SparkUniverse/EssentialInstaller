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

package gg.essential.installer.gui

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.inspector.Inspector
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.unstable.layoutdsl.*
import gg.essential.elementa.unstable.state.v2.combinators.map
import gg.essential.elementa.unstable.state.v2.memo
import gg.essential.elementa.unstable.state.v2.mutableStateOf
import gg.essential.elementa.unstable.util.isInComponentTree
import gg.essential.installer.gui.component.*
import gg.essential.installer.gui.page.*
import gg.essential.installer.isDebug
import gg.essential.installer.logging.Logging.logger

/**
 * Responsible for keeping track of the page stack to provide the currently open page and back button functionality
 */
object PageHandler {

    // Internal tracking
    private val pageStack = mutableListOf<InstallerPage>(StartPage)

    // External
    val currentAndPreviousPageWithNavigationType = mutableStateOf(PageInfo(StartPage, StartPage, NavigationType.JUMP))
    val current = currentAndPreviousPageWithNavigationType.map { it.current }
    val previous = currentAndPreviousPageWithNavigationType.map { it.previous }
    val navigationType = currentAndPreviousPageWithNavigationType.map { it.navigationType }
    val canGoBack = currentAndPreviousPageWithNavigationType.map { (curr, prev, _) -> curr != prev && curr != StartPage }

    fun navigateTo(page: InstallerPage) {
        logger.info("Navigating to $page")
        pageStack.add(page)
        currentAndPreviousPageWithNavigationType.set { (curr, _, _) -> PageInfo(page, curr, NavigationType.FORWARDS) }
    }

    fun navigateBack() {
        if (pageStack.size <= 1) return
        logger.info("Navigating back")
        val prev = pageStack.removeLast()
        val curr = pageStack.last()
        currentAndPreviousPageWithNavigationType.set(PageInfo(curr, prev, NavigationType.BACKWARDS))
    }

    fun navigateToStart(additionalPage: InstallerPage) = navigateToStart(listOf(additionalPage))

    fun navigateToStart(additionalPages: List<InstallerPage> = listOf()) {
        pageStack.clear()
        logger.info("Navigating to start")
        pageStack.add(StartPage)
        if (additionalPages.isNotEmpty()) {
            logger.info("And jumping forwards with extra pages: ${additionalPages.joinToString(", ")}")
            pageStack.addAll(additionalPages)
        }
        val curr = pageStack.last()
        val prev = if (pageStack.size > 1) pageStack[pageStack.size - 2] else curr
        currentAndPreviousPageWithNavigationType.set(PageInfo(curr, prev, NavigationType.JUMP))
    }

    fun createUninstallScreen() = LayoutDslScreen {
        UninstallOldInstallerPage()
    }

    fun createUpdateScreen() = LayoutDslScreen {
        UpdateAvailablePage()
    }

    fun createMainScreen() = LayoutDslScreen {
        installerBackground {
            currentPage()
        }
    }

    private fun LayoutScope.currentPage() {
        val topPadding = 80f
        val sidePadding = 80f
        val bottomPadding = 64f
        val textGradientWidth = 80f

        fun LayoutScope.paddedPageBox(block: LayoutScope.() -> Unit) =
            box(
                Modifier
                    .fillWidth(padding = sidePadding)
                    .fillHeight(padding = (topPadding + bottomPadding) / 2f)
                    .alignVertical(Alignment.Start(topPadding))
                    .alignHorizontal(Alignment.Start(sidePadding))
            ) {
                block()
            }

        fun LayoutScope.paddedTextBox(block: LayoutScope.() -> Unit) =
            box(Modifier.fillWidth(padding = textGradientWidth).alignHorizontal(Alignment.Start(textGradientWidth)).alignVertical(Alignment.Start), block)

        fun LayoutScope.textBox(block: LayoutScope.() -> Unit) =
            box(Modifier.fillWidth(0.55f).alignTopLeft(), block)

        // This feels like a mess, but it's the cleanest solution for cross-page animations I could come up with...
        bind(currentAndPreviousPageWithNavigationType) { (curr, prev, navigation) ->
            val currTB = curr.titleAndBody.getUntracked()
            val prevTB = prev.titleAndBody.getUntracked()

            val animateStartPage = curr != prev && navigation != NavigationType.JUMP && (prev == StartPage || curr == StartPage)
            val animateText = !animateStartPage && prevTB != null && currTB != null
            val slidingLeft = navigation == NavigationType.FORWARDS

            if (animateText) {
                val currTBBox: UIComponent
                val prevTBBox: UIComponent
                // The gradient starts from the edges of the component, so we must expand the enclosing box accordingly and position it correctly so that text is where it should be
                box(Modifier.fillWidth(0.55f)
                    .alignHorizontal(Alignment.Start(sidePadding - textGradientWidth)).alignVertical(Alignment.Start(topPadding))
                    .effect { LeftAndRightGradientFadeEffect(textGradientWidth.toDouble()) }.effect { ScissorEffect() }
                ) {
                    // Compiler gets mad without ?.invoke(), even though `animateText` makes sure both are non-null
                    currTBBox = paddedTextBox { currTB?.invoke() }
                    prevTBBox = paddedTextBox { prevTB?.invoke() }
                }
                animateComponentHorizontalSlide(currTBBox, prevTBBox, slidingLeft, textGradientWidth)
            }

            // Layout the page after the text, so that the text doesn't block clicks on the right side
            val currPage = paddedPageBox {
                if (!animateText && currTB != null) {
                    textBox { currTB() }
                }
                curr()
            }

            if (animateStartPage) {
                val prevPage = paddedPageBox {
                    if (prevTB != null) {
                        textBox { prevTB() }
                    }
                    prev()
                }
                animateComponentHorizontalSlide(currPage, prevPage, slidingLeft, sidePadding)
            }

            if_(memo { curr.backButtonOverride() ?: PageHandler.canGoBack() }) {
                backButton(Modifier.alignBottomLeft(bottomPadding, sidePadding))
            }
        }
    }

    private fun animateComponentHorizontalSlide(
        curr: UIComponent,
        prev: UIComponent,
        slidingLeft: Boolean,
        offset: Float,
        strategy: AnimationStrategy = Animations.IN_CUBIC,
        time: Float = 0.5f,
    ) {
        val left = if (slidingLeft) prev else curr
        val right = if (slidingLeft) curr else prev
        // We "attach" the component on the right, to the right of the component on the left
        right.constrain {
            x = (SiblingConstraint() boundTo left) + (offset * 2).pixels
        }
        // We align the left component either on the screen, or to the left of it, based on sliding direction
        left.constrain {
            x = offset.pixels(alignOutside = !slidingLeft)
        }
        // Slide the components
        left.animate {
            setXAnimation(strategy, time, offset.pixels(alignOutside = slidingLeft))
        }
    }

    class LayoutDslScreen(
        block: LayoutScope.() -> Unit,
    ) : WindowScreen(ElementaVersion.V11) {
        init {
            window.layoutAsBox {
                block()
            }

            if (isDebug() || System.getProperty("elementa.dev", "false").toBoolean()) {
                val inspector by lazy { Inspector(window) hiddenChildOf window }
                window.onKeyType { typedChar, _ ->
                    if (typedChar == '=') {
                        if (inspector.isInComponentTree()) {
                            inspector.hide()
                        } else {
                            inspector.unhide()
                        }
                    }
                }
            }
        }
    }

    enum class NavigationType {
        FORWARDS,
        BACKWARDS,
        JUMP,
    }

    data class PageInfo(
        val current: InstallerPage,
        val previous: InstallerPage,
        val navigationType: NavigationType,
    )

}
