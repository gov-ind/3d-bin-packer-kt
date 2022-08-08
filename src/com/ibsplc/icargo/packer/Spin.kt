package com.ibsplc.icargo.packer

fun spinItem(item: Item, axis: Int = 1) {
    val tmp: Double
    val tmp2: Double

    unspinItem(item)

    if (axis == 1) {
        tmp = item.l
        item.l = item.b
        item.b = tmp
    }
    else if (axis == 2) {
        tmp = item.b
        item.b = item.h
        item.h = tmp
    }
    else if (axis == 3) {
        tmp = item.l
        item.l = item.h
        item.h = tmp
    }
    else if (axis == 4) {
        tmp = item.l
        item.l = item.b
        item.b = tmp

        tmp2 = item.h
        item.h = item.l
        item.l = tmp2
    }
    else if (axis == 5) {
        tmp = item.l
        item.l = item.b
        item.b = tmp

        tmp2 = item.h
        item.h = item.b
        item.b = tmp2
    }

    item.axis = axis
};

fun unspinItem(item: Item) {
    val tmp: Double
    val tmp2: Double

    if (item.axis == 1) {
        tmp = item.l
        item.l = item.b
        item.b = tmp
    }
    else if (item.axis == 2) {
        tmp = item.b
        item.b = item.h
        item.h = tmp
    }
    else if (item.axis == 3) {
        tmp = item.l
        item.l = item.h
        item.h = tmp
    }
    else if (item.axis == 4) {
        tmp2 = item.h
        item.h = item.l
        item.l = tmp2

        tmp = item.l
        item.l = item.b
        item.b = tmp
    }
    else if (item.axis == 5) {
        tmp2 = item.h
        item.h = item.b
        item.b = tmp2


        tmp = item.l
        item.l = item.b
        item.b = tmp
    }

    item.axis = 0
}