package com.ibsplc.icargo.packer

import com.ibsplc.icargo.packer.Bin
import com.ibsplc.icargo.packer.Item
import com.ibsplc.icargo.packer.Type
import com.ibsplc.icargo.packer.numberOfHeaviestItemsToPackFromCenter

val filterBins = { bins: List<Bin> -> ArrayList(bins.filter { bin -> bin.l != 0.0 && bin.b != 0.0 && bin.h != 0.0 }) }

/**
 * Sort [bins] so that breadth has the highest priority and length has the lowest
 **/
fun sortBins(bins: MutableList<Bin>) {
    return bins.sortWith(object: Comparator<Bin> {
        override fun compare(a: Bin, b: Bin): Int {
            if (a.type == Type.l) return -1
            if (a.type == Type.h) {
                if (b.type == Type.b) return -1
                return 1
            }
            if (a.type == Type.b) return 1
            return -1
        }
    })
}

fun sortItemsByVolume(items: MutableList<Item>) {
    return items.sortWith(object: Comparator<Item> {
        override fun compare(a: Item, b: Item): Int {
            val aVol = a.l * a.b * a.h
            val bVol = b.l * b.b * b.h

            if (aVol == bVol) {
                if (a.weight == b.weight) return 0
                if (a.weight > b.weight) return 1
                return -1
            }
            if (aVol > bVol)
                return -1
            return 1
        }
    })
}

data class BinSpinConf(val bin: Bin, val spin: Int)
data class ItemSpinConf(val item: Item, val spin: Int)

val sortLBins = { bins: MutableList<BinSpinConf>, item: Item ->
    bins.sortWith(object : Comparator<BinSpinConf> {
        override fun compare(a: BinSpinConf, b: BinSpinConf): Int {
            val item1 = item.copy()
            spinItem(item1, a.spin)
            val item2 = item.copy()
            spinItem(item2, b.spin)

            if (item1.h == item2.h) return 0
            if (item1.h < item2.h) return -1
            return 1
        }
    })
}

val sortHBins = { bins: MutableList<BinSpinConf>, item: Item ->
    bins.sortWith(object : Comparator<BinSpinConf> {
        override fun compare(a: BinSpinConf, b: BinSpinConf): Int {
            val item1 = item.copy()
            spinItem(item1, a.spin)
            val item2 = item.copy()
            spinItem(item2, b.spin)

            //if (a.bin.b == b.bin.b) return 0
            //if (a.bin.b > b.bin.b /*&& item1.l * item1.h < item2.l * item2.h*/) return -1
            if (item1.l == item2.l) return 0
            if (item1.l > item2.l) return -1
            return 1
        }
    })
}

val sortBBins = { bins: MutableList<BinSpinConf>, item: Item ->
    bins.sortWith(object : Comparator<BinSpinConf> {
        override fun compare(a: BinSpinConf, b: BinSpinConf): Int {
            val item1 = item.copy()
            spinItem(item1, a.spin)
            val item2 = item.copy()
            spinItem(item2, b.spin)
            val freeVol1 = (a.bin.l - item1.l) * item1.h * item1.b
            val freeVol2 = (b.bin.l - item2.l) * item2.h * item2.b

            if (freeVol1 == freeVol2) return 0
            if (freeVol1 < freeVol2) return -1
            return 1
        }
    })
}

/**
 * Sort items for a bin that was partitioned length-wise so that the first item wastes the least volume
 **/
val sortItemsForLBin = { items: MutableList<ItemSpinConf> ->
    items.sortWith(object : Comparator<ItemSpinConf> {
        override fun compare(a: ItemSpinConf, b: ItemSpinConf): Int {
            val item1 = a.item.copy()
            spinItem(item1, a.spin)
            val item2 = b.item.copy()
            spinItem(item2, b.spin)
            val vol1 = item1.l * item1.b * item1.h
            val vol2 = item2.l * item2.b * item2.h

            if (vol1 == vol2) return 0
            if (vol1 > vol2) return -1
            return 1
        }
    })
}

/*val sortItemsForHBin = { items: List<ItemSpinConf>, bin: Bin ->
    items.sortedWith(object : Comparator<ItemSpinConf> {
        override fun compare(a: ItemSpinConf, b: ItemSpinConf): Int {
            val item1 = a.item.copy()
            spinItem(item1, a.spin)
            val item2 = b.item.copy()
            spinItem(item2, b.spin);

            if (((bin.h - item1.h) * bin.l * item1.b) + (item1.h * (bin.l - item1.l) * item1.b)
                < (((bin.h - item2.h) * bin.l * item2.b))
                + (item2.h * (bin.l - item2.l) * item2.b)) {
                return -1
            }
            return 1
        }
    })
}*/

val sortItemsForBBin = sortItemsForLBin

/**
 * Sort items for a bin that was partitioned height-wise so that the first item wastes the least length
 **/
val sortItemsForHBin = { items: MutableList<ItemSpinConf> ->
    items.sortWith(object : Comparator<ItemSpinConf> {
        override fun compare(a: ItemSpinConf, b: ItemSpinConf): Int {
            val item1 = a.item.copy()
            spinItem(item1, a.spin)
            val item2 = b.item.copy()
            spinItem(item2, b.spin)

            if (item1.l == item2.l) return 0
            if (item1.l > item2.l) return -1
            return 1
        }
    })
}

data class ItemWithIndex(val item: Item?, val index: Int)

/**
 * Create a bin configuration so that items are packed from the center
 **/
fun putNHeaviestItemsFirst(items: ArrayList<Item?>, n: Int = numberOfHeaviestItemsToPackFromCenter): ArrayList<Item?> {
    if (numberOfHeaviestItemsToPackFromCenter == -1) return items

    val copy = items.mapIndexed({ index, item -> ItemWithIndex(item, index) })

    copy.sortedWith(object: Comparator<ItemWithIndex> {
        override fun compare(a: ItemWithIndex, b: ItemWithIndex): Int {
            if (a.item != null && b.item != null) {
                if (a.item.unstackable) {
                    return 1
                }
                if (a.item.weight > b.item.weight)
                    return -1
            }
            return 1
        }
    })

    for (i in 0..n) {
        items[copy[i].index] = null
    }

    val out = arrayListOf<Item?>()

    for (i in 0..n) {
        val item = copy[i]

        if (item.item != null) {
            item.item.heavy = true
            out.add(item.item)
        }
    }

    out.addAll(items.filter({ item -> item != null }))

    return out
}
