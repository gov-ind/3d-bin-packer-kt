@file:JvmName("Geometry")
package com.ibsplc.icargo.packer

import java.io.Serializable

var _id = 0
var numberOfHeaviestItemsToPackFromCenter = 15

enum class Type { l, b, h }
data class Point(val x: Double = 0.0, val y: Double = 0.0, val z: Double = 0.0): Serializable
data class Face(val points: List<Point> = arrayListOf()): Serializable
data class Polyhedron(val faces: List<Face> = arrayListOf()): Serializable

data class Bin @JvmOverloads constructor(
    val id: String = "",
    val l: Double = 0.0,
    val b: Double = 0.0,
    val h: Double = 0.0,
    val reservedForHeavyItems: Boolean = false,
    val weightLimit: Double = Double.POSITIVE_INFINITY,
    val contour: Polyhedron = Polyhedron(),
    val offset: Point = Point(),
    val _id: Int = com.ibsplc.icargo.packer._id,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val type: Type = Type.l,
    var checked: Boolean = false,
    val reservedForUnstackable: Boolean = false,
    var itemTypesInBin: ArrayList<String> = arrayListOf(),
    var incompatibleItemTypesInBin: ArrayList<String> = arrayListOf()
): Serializable

data class Item @JvmOverloads constructor(
    val id: String = "",
    var l: Double = 0.0,
    var b: Double = 0.0,
    var h: Double = 0.0,
    val spinnable: Boolean = true,
    val unstackable: Boolean = false,
    val weight: Double = 0.0,
    val type: String? = null,
    val incompatibleTypes: List<String> = listOf(),
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0,
    var packed: Boolean = false,
    var axis: Int = 0,
    var heavy: Boolean = false,
    val fused: Boolean = false,
    val factors: Array<Int> = arrayOf(),
    val fusedItems: List<Item> = listOf()
): Serializable

/**
 * Subtract two points
 **/
val minus = { p1: Point, p2: Point -> Point(x = p1.x - p2.x, y = p1.y - p2.y, z = p1.z - p2.z) }

/**
 * Cross product of two points
 **/
val cross = { p1: Point, p2: Point -> Point(
    x = p1.y * p2.z - p2.y * p1.z,
    y = p1.z * p2.x - p2.z * p1.x,
    z = p1.x * p2.y - p2.x * p1.y
) }

/**
 * Dot product of two points
 **/
val dot = { p1: Point, p2: Point -> p1.x * p2.x + p1.y * p2.y + p1.z * p2.z }

/**
 * Magnitude of a vector
 **/
val magnitude = { p: Point -> Math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z) }

/**
 * Return a [Point] that is orthogonal to a [face]
 **/
fun normal(face: Face): Point {
    val dir1 = minus(face.points[1], face.points[0])
    val dir2 = minus(face.points[2], face.points[0])

    val normal = cross(dir1, dir2)

    val d = magnitude(normal)

    return Point(x = normal.x / d, y = normal.y / d, z = normal.z / d)
}

/**
 * Check if a [Point] [p] is inside a [polyhedron]
 **/
fun isPointInMe(p: Point, polyhedron: Polyhedron): Boolean {
    for (face in polyhedron.faces) {
        val vectorFromPointToFace = minus(face.points[0], p)
        var dotProduct = dot(vectorFromPointToFace, normal(face))

        dotProduct /= magnitude(vectorFromPointToFace)

        if (dotProduct < -1e-15) {
            return false
        }
    }
    return true
}

/**
 * Check if an [item] is in a [polyhedron]
 **/
fun isItemInPolyhedron(item: Item, polyhedron: Polyhedron?): Boolean {
    if (polyhedron != null) {
        val corners = getCornersOfItem(item)

        for (corner in corners) {
            if (!isPointInMe(corner, polyhedron)) return false
        }
    }
    return true
}

//val lam = { strategy: Int, (l, b, h): Bin -> }

fun createInitialBins (strategy: Int, bin: Bin): MutableList<Bin> {
    if (strategy == 1) { // heaviest from middle
        //return binsForHeaviestFromMiddle;
        return arrayListOf()
    }
    else {
        numberOfHeaviestItemsToPackFromCenter = -1

        return arrayListOf(
            Bin(
                _id = ++_id,
                l = bin.l,
                b = bin.b,
                h = bin.h,
                offset = Point(
                    x = -bin.l / 2,
                    y = -bin.h / 2,
                    z = -bin.b / 2
                ),
                contour = bin.contour
            )
        )
    }
}

val getCornersOfItem = { item: Item -> arrayListOf(
    Point(
        x = item.x - item.l / 2,
        y = item.y - item.h / 2,
        z = item.z - item.b / 2
    ),
    Point(
        x = item.x - item.l / 2,
        y = item.y - item.h / 2,
        z = item.z + item.b / 2
    ),
    Point(
        x = item.x - item.l / 2,
        y = item.y + item.h / 2,
        z = item.z - item.b / 2
    ),
    Point(
        x = item.x - item.l / 2,
        y = item.y + item.h / 2,
        z = item.z + item.b / 2
    ),
    Point(
        x = item.x + item.l / 2,
        y = item.y - item.h / 2,
        z = item.z - item.b / 2
    ),
    Point(
        x = item.x + item.l / 2,
        y = item.y - item.h / 2,
        z = item.z + item.b / 2
    ),
    Point(
        x = item.x + item.l / 2,
        y = item.y + item.h / 2,
        z = item.z - item.b / 2
    ),
    Point(
        x = item.x + item.l / 2,
        y = item.y + item.h / 2,
        z = item.z + item.b / 2
    )
) }

val getCornerCoordOfItemInBin = { bin: Bin, item: Item -> Point(
    x = bin.offset.x + item.l / 2,
    y = bin.offset.y + item.h / 2,
    z = bin.offset.z + item.b / 2
) }

val splitBinByLength = { bin : Bin, item: Item -> bin.copy(
    _id = ++_id,
    x = item.l / 2,
    y = 0.0,
    z = 0.0,
    l = bin.l - item.l,
    b = bin.b,
    h = bin.h,
    offset = Point(
        x = bin.offset.x + item.l,
        y = bin.offset.y,
        z = bin.offset.z
    ),
    type = Type.l,
    checked = false
) }

val splitBinByHeight = { bin : Bin, item: Item -> bin.copy(
    _id = ++_id,
    x = -bin.l / 2 + item.l / 2,
    y = item.h / 2,
    z = -bin.b / 2 + item.b / 2,
    l = item.l,
    b = item.b,
    h = bin.h - item.h,
    offset = Point(
        x = bin.offset.x,
        y = bin.offset.y + item.h,
        z = bin.offset.z
    ),
    type = Type.h,
    reservedForUnstackable = item.unstackable,
    checked = false
) }

val splitBinByBreadth = { bin : Bin, item: Item -> bin.copy(
    _id = ++_id,
    x = -bin.l / 2 + item.l / 2,
    y = 0.0,
    z = item.b / 2,
    l = item.l,
    b = bin.b - item.b,
    h = bin.h,
    offset = Point(
        x = bin.offset.x,
        y = bin.offset.y,
        z = bin.offset.z + item.b
    ),
    type = Type.b,
    checked = false
) }

val createSubBins = { bin: Bin, item: Item -> arrayOf(
    splitBinByLength(bin, item),
    splitBinByBreadth(bin, item),
    splitBinByHeight(bin, item)
) }

fun doesItemFitInBin(bin: Bin, item: Item, weightOfItems: Double?, weightLimit: Double?): Boolean {
    if ((bin.type == Type.l || bin.type == Type.b) && item.unstackable && !(isBinRoot(bin))) return false
    if (!item.unstackable && bin.reservedForUnstackable) return false
    return item.l <= bin.l &&
        item.b <= bin.b &&
        item.h <= bin.h &&
        ((weightOfItems ?: 0.0) + item.weight) <= (weightLimit ?: Double.POSITIVE_INFINITY)
}

val isBinRoot = { bin: Bin -> bin.y == 0.0 }

/**
 * Create a polyhedron
 **/
fun createContour(l: Double, b: Double , h: Double): Polyhedron {
    return Polyhedron(
        faces = listOf(
            Face(
                points = listOf(
                    Point(-l / 2, -h / 2, -b / 2),
                    Point(-l / 2, h / 2, -b / 2),
                    Point(l / 2, h / 2, -b / 2),
                    Point(l / 2, -h / 2, -b / 2),
                    Point(-l / 2, -h / 2, -b / 2)
                )
            ),
            Face(
                points = listOf(
                    Point(l / 2, -h / 2, -b / 2),
                    Point(l / 2, h / 2, -b / 2),
                    Point(l / 2, h / 2, b / 2),
                    Point(l / 2, -h / 2, b / 2),
                    Point(l / 2, -h / 2, -b / 2)
                )
            ),
            Face(
                points = listOf(
                    Point(l / 2, -h / 2, b / 2),
                    Point(l / 2, h / 2, b / 2),
                    Point(-l / 2, h / 2, b / 2),
                    Point(-l / 2, -h / 2, b / 2),
                    Point(l / 2, -h / 2, b / 2)
                )
            ),
            Face(
                points = listOf(
                    Point(-l / 2, -h / 2, b / 2),
                    Point(-l / 2, h / 2, b / 2),
                    Point(-l / 2, h / 2, -b / 2),
                    Point(-l / 2, -h / 2, -b / 2),
                    Point(-l / 2, -h / 2, b / 2)
                )
            ),
            Face(
                points = listOf(
                    Point(-l / 2, h / 2, b / 2),
                    Point(l / 2, h / 2, b / 2),
                    Point(l / 2, h / 2, -b / 2),
                    Point(-l / 2, h / 2, -b / 2),
                    Point(-l / 2, h / 2, b / 2)
                )
            ),
            Face(
                points = listOf(
                    Point(-l / 2, -h / 2, b / 2),
                    Point(-l / 2, -h / 2, -b / 2),
                    Point(l / 2, -h / 2, -b / 2),
                    Point(l / 2, -h / 2, b / 2),
                    Point(-l / 2, -h / 2, b / 2)
                )
            )
        )
    )
}

/*fun createContour(l: Double, b: Double , h: Double): Polyhedron {
    return Polyhedron(
        faces = listOf(
            Face(
                points = listOf(
                    Point(-l / 2, -b / 2, -h / 2),
                    Point(-l / 2, b / 2, -h / 2),
                    Point(l / 2, b / 2, -h / 2),
                    Point(l / 2, -b / 2, -h / 2),
                    Point(-l / 2, -b / 2, -h / 2)
                )
            ),
            Face(
                points = listOf(
                    Point(l / 2, -b / 2, -h / 2),
                    Point(l / 2, b / 2, -h / 2),
                    Point(l / 2, b / 2, h / 2),
                    Point(l / 2, -b / 2, h / 2),
                    Point(l / 2, -b / 2, -h / 2)
                )
            ),
            Face(
                points = listOf(
                    Point(l / 2, -b / 2, h / 2),
                    Point(l / 2, b / 2, h / 2),
                    Point(-l / 2, b / 2, h / 2),
                    Point(-l / 2, -b / 2, h / 2),
                    Point(l / 2, -b / 2, h / 2)
                )
            ),
            Face(
                points = listOf(
                    Point(-l / 2, -b / 2, h / 2),
                    Point(-l / 2, b / 2, h / 2),
                    Point(-l / 2, b / 2, -h / 2),
                    Point(-l / 2, -b / 2, -h / 2),
                    Point(-l / 2, -b / 2, h / 2)
                )
            ),
            Face(
                points = listOf(
                    Point(-l / 2, b / 2, h / 2),
                    Point(l / 2, b / 2, h / 2),
                    Point(l / 2, b / 2, -h / 2),
                    Point(-l / 2, b / 2, -h / 2),
                    Point(-l / 2, b / 2, h / 2)
                )
            ),
            Face(
                points = listOf(
                    Point(-l / 2, -b / 2, h / 2),
                    Point(-l / 2, -b / 2, -h / 2),
                    Point(l / 2, -b / 2, -h / 2),
                    Point(l / 2, -b / 2, h / 2),
                    Point(-l / 2, -b / 2, h / 2)
                )
            )
        )
    )
}*/
