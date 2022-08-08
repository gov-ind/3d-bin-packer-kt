package com.ibsplc.icargo.packer

val excludeGroupName = "exclude"
// var fId = -1

/**
 * Group an [item] by it's dimension, unstackable, spinnable, or exclude property
 **/
fun group(item: Item,
          groupBy: (Item) -> String = { a -> a.l.toString() + "x" + a.b.toString() + "x" + a.h.toString() +
              (if (a.type != null) "_" + a.type else "") +
              (if (a.unstackable) "_unstackable" else "_stackable") +
              (if (a.spinnable) "_spinnable" else "_unspinnable") },
          exclude: (Item) -> Boolean = { _ -> false }): String {
    return if (exclude(item)) excludeGroupName else groupBy(item)
}

val groupItems = { items: List<Item> -> items.groupBy { item -> group(item) } }

/**
 * Fuse each set of items within a group
 **/
val tryFuseItems = { groups: Map<String, List<Item>> ->
    groups.values.fold(listOf<Item>()) { acc, items ->
        acc + fuseItems(items)
    } }

/**
 * Fuse a list of items
 **/
fun fuseItems(items: List<Item>): List<Item> {
    if (items.size == 1) return items

    // Greedily express the number of items as a sum of perfect cubes
    val factors = findFactors(items.size)
    var i = 0

    return factors.fold(listOf<Item>()) { acc, factor ->
        val numberOfItems = factor[0] * factor[1] * factor[2]
        val fusedItems = items.subList(i, i + numberOfItems)
        val item = Item(
            l = items[0].l * factor[0],
            b = items[0].b * factor[1],
            h = items[0].h * factor[2],
            fused = !(factor[0] == 1 && factor[1] == 1 && factor[2] == 1),
            factors = factor, // We need the factors to unfuse the items
            fusedItems =  fusedItems,
            weight = fusedItems.fold(fusedItems[0].weight) { acc, item -> acc + item.weight },
            spinnable = fusedItems.all { it.spinnable },
            unstackable = fusedItems.all { it.unstackable },
            type = items[0].type,
            incompatibleTypes = items[0].incompatibleTypes
            // fID for debugging
            // id = "f" + ++fId
        )
        i += numberOfItems

        return@fold acc + item
    }
}

/**
 * Unfuse an item into its constituent items
 **/
val unfuseItems = { items: List<Item> -> items.fold(listOf<Item>()) { acc, item -> acc + unfuseItem(item) } }

fun unfuseItem(item: Item): List<Item> {
    if (!item.fused) return listOf(item)

    val out = arrayListOf<Item>()
    val l = item.l / item.factors[0]
    val b = item.b / item.factors[1]
    val h = item.h / item.factors[2]
    var c = 0

    for (i in 0 until item.factors[0]) {
        for (j in 0 until item.factors[1]) {
            for (k in 0 until item.factors[2]) {
                val fusedItem = item.fusedItems[c++]
                out.add(
                    fusedItem.copy(
                        fused = false,
                        l = l,
                        b = b,
                        h = h,
                        x = if (item.x == 0.0) 0.0 else item.x - item.l / 2 + (i * l) + l / 2,
                        y = if (item.y == 0.0) 0.0 else item.y - item.h / 2 + (k * h) + h / 2,
                        z = if (item.z == 0.0) 0.0 else item.z - item.b / 2 + (j * b) + b / 2,
                        packed = item.packed
                    )
                )
            }
        }
    }

    return out
}

/**
 * Greedily factor a number as a sum of perfect cubes
 **/
fun findFactors(n: Int, factors: Array<Array<Int>> = arrayOf()): Array<Array<Int>> {
    if (n == 1) return factors + arrayOf(1, 1, 1)

    val cubeRoot = findNearestPerfectCubeLesserThan(n)

    if (cubeRoot == 1) {
        if (n % 2 == 0) return factors + arrayOf(2, n / 2, 1)
        return factors + arrayOf(2, (n - 1) / 2, 1) + arrayOf(1, 1, 1)
    }
    else {
        val diff = n - (cubeRoot * cubeRoot * cubeRoot)

        if (diff == 0) return factors + arrayOf(cubeRoot, cubeRoot, cubeRoot)

        return factors + findFactors(diff, arrayOf(arrayOf(cubeRoot, cubeRoot, cubeRoot)))
    }
}

val findNearestPerfectCubeLesserThan = { n: Int -> Math.floor(Math.cbrt(n.toDouble())).toInt() }
