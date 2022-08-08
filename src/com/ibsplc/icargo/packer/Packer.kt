package com.ibsplc.icargo.packer

import com.beust.klaxon.Klaxon
import java.io.File
import java.nio.file.Paths

val fuseFirst = true

/**
 * Check if an [item] can be packed into a list of [bin]
 **/
fun packItem(bins: MutableList<Bin>, bin: Bin, item: Item): Boolean {
    val coord = getCornerCoordOfItemInBin(bin, item)

    // Proceed only if item is in the polyhedron
    if (isItemInPolyhedron(Item(
        l = item.l,
        b = item.b,
        h = item.h,
        x = coord.x,
        y = coord.y,
        z = coord.z
    ), bin.contour)) {
        // Check if item type is compatible for this bin
        if ((item.incompatibleTypes.filter { type -> bin.itemTypesInBin.contains(type) }.isNotEmpty() ||
                bin.incompatibleItemTypesInBin.contains(item.type))) {
            return false
        }

        if (item.type != null) {
            if (!bin.itemTypesInBin.contains(item.type)) {
                bin.itemTypesInBin.add(item.type)
            }
            if (bin.incompatibleItemTypesInBin.filter { type ->
                item.incompatibleTypes.contains(type) }.isEmpty()) {
                bin.incompatibleItemTypesInBin.addAll(item.incompatibleTypes)
            }
        }

        val binIndex = bins.indexOf(bin)

        bins.removeAt(binIndex)
        bins.addAll(createSubBins(bin, item))

        item.x = coord.x
        item.y = coord.y
        item.z = coord.z
        item.packed = true # Set this property to denote that this item can be packed

        return true
    }
    return false
}

fun packBestPossibleItemIntoBin(bins: MutableList<Bin>, candidateItems: List<ItemSpinConf>, bin: Bin): Item? {
    for ((item, spin) in candidateItems) {
        spinItem(item, spin)

        if (packItem(bins, bin, item)) {
            return item
        }
    }
    return null
}

fun packItemIntoBestPossibleCandidateBin(bins: MutableList<Bin>, candidateBins: List<BinSpinConf>, item: Item): Boolean {
    for ((bin, spin) in candidateBins) {
        spinItem(item, spin)

        if (packItem(bins, bin, item)) {
            return true
        }
    }
    return false
}

class DoPackConf(val weightOfItems: Double = 0.0, val weightLimit: Double = Double.POSITIVE_INFINITY)
data class PackResult(val items: List<Item>, val bins: MutableList<Bin>, val weightOfItems: Double = 0.0)
data class BinPackResult(val packedItems: MutableMap<String, List<Item>>, val unpackedItems: List<Item>?)

fun doPack(items: MutableList<Item>, bins: MutableList<Bin>, conf: DoPackConf): PackResult {
    var weightOfItems = conf.weightOfItems
    val weightLimit = conf.weightLimit

    sortItemsByVolume(items)
    //items = putNHeaviestItemsFirst(items);

    for (item in items) {
        val candidateLBins = arrayListOf<BinSpinConf>()
        val candidateHBins = arrayListOf<BinSpinConf>()
        val candidateBBins = arrayListOf<BinSpinConf>()

        //sortBins(filterBins(bins))

        for (bin in bins) {
            if (item.packed) break

            if (doesItemFitInBin(bin, item, weightOfItems, weightLimit)) {
                if (bin.type == Type.l) {
                    candidateLBins.add(BinSpinConf(bin, 0))
                }
                else if (bin.type == Type.h) {
                    candidateHBins.add(BinSpinConf(bin, 0))
                }
                else if (bin.type == Type.b) {
                    candidateBBins.add(BinSpinConf(bin, 0))
                }
            }
            // Attempt to rotate item across all dimensions
            if (item.spinnable) {
                spinItem(item, 1)
                if (doesItemFitInBin(bin, item, weightOfItems, weightLimit)) {
                    if (bin.type == Type.l) {
                        candidateLBins.add(BinSpinConf(bin, 1))
                    }
                    else if (bin.type == Type.h) {
                        candidateHBins.add(BinSpinConf(bin, 1))
                    }
                    else if (bin.type == Type.b) {
                        candidateBBins.add(BinSpinConf(bin, 1))
                    }
                }

                spinItem(item, 2)
                if (doesItemFitInBin(bin, item, weightOfItems, weightLimit)) {
                    if (bin.type == Type.l) {
                        candidateLBins.add(BinSpinConf(bin, 2))
                    }
                    else if (bin.type == Type.h) {
                        candidateHBins.add(BinSpinConf(bin, 2))
                    }
                    else if (bin.type == Type.b) {
                        candidateBBins.add(BinSpinConf(bin, 2))
                    }
                }

                spinItem(item, 3)
                if (doesItemFitInBin(bin, item, weightOfItems, weightLimit)) {
                    if (bin.type == Type.l) {
                        candidateLBins.add(BinSpinConf(bin, 3))
                    }
                    else if (bin.type == Type.h) {
                        candidateHBins.add(BinSpinConf(bin, 3))
                    }
                    else if (bin.type == Type.b) {
                        candidateBBins.add(BinSpinConf(bin, 3))
                    }
                }

                spinItem(item, 4)
                if (doesItemFitInBin(bin, item, weightOfItems, weightLimit)) {
                    if (bin.type == Type.l) {
                        candidateLBins.add(BinSpinConf(bin, 4))
                    }
                    else if (bin.type == Type.h) {
                        candidateHBins.add(BinSpinConf(bin, 4))
                    }
                    else if (bin.type == Type.b) {
                        candidateBBins.add(BinSpinConf(bin, 4))
                    }
                }

                spinItem(item, 5)
                if (doesItemFitInBin(bin, item, weightOfItems, weightLimit)) {
                    if (bin.type == Type.l) {
                        candidateLBins.add(BinSpinConf(bin, 5))
                    }
                    else if (bin.type == Type.h) {
                        candidateHBins.add(BinSpinConf(bin, 5))
                    }
                    else if (bin.type == Type.b) {
                        candidateBBins.add(BinSpinConf(bin, 5))
                    }
                }
                unspinItem(item)
            }
        }

        if (candidateHBins.isNotEmpty()) {
            sortHBins(candidateHBins, item)
            if (packItemIntoBestPossibleCandidateBin(bins, candidateHBins, item)) {
                weightOfItems += item.weight
            }
        }
        else if (candidateBBins.isNotEmpty()) {
            sortBBins(candidateBBins, item)
            if (packItemIntoBestPossibleCandidateBin(bins, candidateBBins, item)) {
                weightOfItems += item.weight
            }
        }
        else if (candidateLBins.isNotEmpty()) {
            sortLBins(candidateLBins, item)
            if (packItemIntoBestPossibleCandidateBin(bins, candidateLBins, item)) {
                weightOfItems += item.weight
            }
        }
    }

    // Find bins that were not checked (for packing)
    bins.forEach { bin -> bin.checked = false }
    var uncheckedBins = bins.filter{ !it.checked }

    while (uncheckedBins.isNotEmpty()) {
        for (bin in uncheckedBins) {
            val target = bins.find{ a -> bin._id == a._id }
            target?.checked = true
            val candidateItems = arrayListOf<ItemSpinConf>()

            for (item in items) {
                if (item.packed) continue

                if (doesItemFitInBin(bin, item, weightOfItems, weightLimit)) {
                    candidateItems.add(ItemSpinConf(item, 0))
                }
                if (item.spinnable) {
                    spinItem(item, 1)

                    if (doesItemFitInBin(bin, item, weightOfItems, weightLimit)) {
                        candidateItems.add(ItemSpinConf(item, 1))
                    }
                    spinItem(item, 2)

                    if (doesItemFitInBin(bin, item, weightOfItems, weightLimit)) {
                        candidateItems.add(ItemSpinConf(item, 2))
                    }
                    spinItem(item, 3)

                    if (doesItemFitInBin(bin, item, weightOfItems, weightLimit)) {
                        candidateItems.add(ItemSpinConf(item, 3))
                    }
                    spinItem(item, 4)

                    if (doesItemFitInBin(bin, item, weightOfItems, weightLimit)) {
                        candidateItems.add(ItemSpinConf(item, 4))
                    }
                    spinItem(item, 5)

                    if (doesItemFitInBin(bin, item, weightOfItems, weightLimit)) {
                        candidateItems.add(ItemSpinConf(item, 5))
                    }
                    unspinItem(item)
                }
            }

            if (!candidateItems.isEmpty()) {
                if (bin.type == Type.l) {
                    sortItemsForLBin(candidateItems)
                }
                else if (bin.type == Type.h) {
                    sortItemsForHBin(candidateItems)
                }
                else if (bin.type == Type.b) {
                    sortItemsForBBin(candidateItems)
                }

                val item = packBestPossibleItemIntoBin(bins, candidateItems, bin)

                if (item != null) {
                    weightOfItems += item.weight
                }
            }
        }

        //sortBins(bins)
        uncheckedBins = bins.filter{ bin -> !bin.checked }
    }

    return PackResult(items, bins, weightOfItems)
}

/**
 * Pack the [_items] into the [bin]
 */
fun pack(_items: List<Item>, bin: Bin): PackResult {
    var items = _items
    // Create bin config (for example, pack from corner / center)
    var bins = createInitialBins(0, bin)

    if (fuseFirst) {
        // Try to fuse items
        // First group items by dimension and other properties, such as whether the item is stackable, and so on.
        items = tryFuseItems(groupItems(items))

        // Separate the unstackable items
        val (fusedItems, unfusedItems) = items.partition { !it.unstackable && it.fused }
        val (fusedItemsBiggerThanBiggestUnfusedItem, rest) = fusedItems.partition {
            val volumeOfBiggestUnfusedItem = unfusedItems.map{ it.l * it. b * it.h }.max()
            if (volumeOfBiggestUnfusedItem != null) {
                return@partition it.l * it.b * it.h > volumeOfBiggestUnfusedItem
            }
            return@partition false
        }

        // First, pack the fused items
        val fusedItemsResults = doPack(
            fusedItemsBiggerThanBiggestUnfusedItem.toMutableList(),
            bins,
            DoPackConf(weightLimit = bin.weightLimit)
        )

        // Separate the fused and unpacked items
        val (fusedAndPacked, fusedButUnpacked) = fusedItemsResults.items.partition { item -> item.packed }

        bins = fusedItemsResults.bins

        // Unfuse the unpacked fused items and the other items
        val unpackedItems = unfuseItems(fusedButUnpacked + unfusedItems + rest).toMutableList()

        if (unpackedItems.isNotEmpty()) {
            // Attempt to pack the rest of the items
            val packResults = doPack(
                unpackedItems,
                bins,
                DoPackConf(fusedItemsResults.weightOfItems, bin.weightLimit)
            )
            items = fusedAndPacked + packResults.items
        }
    }
    else {
        // Pack without fusing
        items = doPack(
            items.toMutableList(),
            bins,
            DoPackConf(weightLimit = bin.weightLimit)
        ).items
    }

    return PackResult(items = unfuseItems(items), bins = bins)
}

/**
 * Pack the [_items] into the [bins]
 */
fun packIntoBins(_items: List<Item>, bins: List<Bin>): BinPackResult {
    var items = _items

    val result = bins.fold(mutableMapOf<String, List<Item>>()) { acc, bin ->
        if (items.isNotEmpty()) {
            val result = pack(items, bin)

            // Remove the unpacked items
            val (packedItems, unpackedItems) = result.items.partition { it.packed }

            acc[bin.id] = packedItems
            items = unpackedItems
        }

        return@fold acc
    }

    return BinPackResult(packedItems = result, unpackedItems = if (items.isEmpty()) null else items)
}

fun main(args: Array<String>) {
    try {
        val itemsInput = File(Paths.get("").toAbsolutePath().toString() + "/res/items_soumya.json").readText()
        val binsInput = File(Paths.get("").toAbsolutePath().toString() + "/res/bins_soumya.json").readText()

        val items = Klaxon().parseArray<Item>(itemsInput)
        val bins = Klaxon().parseArray<Bin>(binsInput)

        if (items != null && bins != null) {
            val result = packIntoBins(items, bins)
            print("yo")
        }
    }
    catch (e: Exception) {
        print(e)
    }
}
