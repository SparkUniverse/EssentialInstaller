package gg.essential.installer.gui.component

import gg.essential.elementa.UIComponent
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.resolution.*

// Copied from Essential

fun lazyX(initializer: () -> XConstraint) = LazyConstraint(lazy(initializer)) as XConstraint

fun lazyY(initializer: () -> YConstraint) = LazyConstraint(lazy(initializer)) as YConstraint

fun lazyPosition(initializer: () -> PositionConstraint) = LazyConstraint(lazy(initializer)) as PositionConstraint

fun lazyHeight(initializer: () -> HeightConstraint) = LazyConstraint(lazy(initializer)) as HeightConstraint

fun lazyRadius(initializer: () -> RadiusConstraint) = LazyConstraint(lazy(initializer)) as RadiusConstraint

fun lazyWidth(initializer: () -> WidthConstraint) = LazyConstraint(lazy(initializer)) as WidthConstraint

fun lazySize(initializer: () -> SizeConstraint) = LazyConstraint(lazy(initializer)) as SizeConstraint

fun lazyMaster(initializer: () -> MasterConstraint) = LazyConstraint(lazy(initializer)) as MasterConstraint

private class LazyConstraint(val constraint: Lazy<SuperConstraint<Float>>) : MasterConstraint {

    override var cachedValue = 0f
    override var recalculate = true
    override var constrainTo: UIComponent? = null

    override fun animationFrame() {
        super.animationFrame()
        constraint.value.animationFrame()
    }

    override fun getHeightImpl(component: UIComponent): Float {
        return (constraint.value as HeightConstraint).getHeightImpl(component)
    }

    override fun getRadiusImpl(component: UIComponent): Float {
        return (constraint.value as RadiusConstraint).getRadiusImpl(component)
    }

    override fun getWidthImpl(component: UIComponent): Float {
        return (constraint.value as WidthConstraint).getWidthImpl(component)
    }

    override fun getXPositionImpl(component: UIComponent): Float {
        return (constraint.value as XConstraint).getXPositionImpl(component)
    }

    override fun getYPositionImpl(component: UIComponent): Float {
        return (constraint.value as YConstraint).getYPositionImpl(component)
    }

    override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {}

}
