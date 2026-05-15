package wile.redstonepen.util

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import kotlin.math.abs
import net.minecraft.world.phys.AABB

fun matchAABB(expected: AABB, eps: Double = 1e-9) =
    Matcher<AABB> { actual ->
        val failures = buildList {
            if (abs(actual.minX - expected.minX) > eps)
                add("minX: expected ${expected.minX} but was ${actual.minX}")
            if (abs(actual.minY - expected.minY) > eps)
                add("minY: expected ${expected.minY} but was ${actual.minY}")
            if (abs(actual.minZ - expected.minZ) > eps)
                add("minZ: expected ${expected.minZ} but was ${actual.minZ}")
            if (abs(actual.maxX - expected.maxX) > eps)
                add("maxX: expected ${expected.maxX} but was ${actual.maxX}")
            if (abs(actual.maxY - expected.maxY) > eps)
                add("maxY: expected ${expected.maxY} but was ${actual.maxY}")
            if (abs(actual.maxZ - expected.maxZ) > eps)
                add("maxZ: expected ${expected.maxZ} but was ${actual.maxZ}")
        }
        MatcherResult(
            failures.isEmpty(),
            { "AABB did not match:\n  ${failures.joinToString("\n  ")}" },
            { "AABB should not have matched but did" },
        )
    }

infix fun AABB.shouldMatchAABB(expected: AABB) = this should matchAABB(expected)
