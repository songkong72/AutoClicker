package com.sejun.autoclicker

/** 게임 위에 뜨는 창을 끌어 옮길 때 화면 밖으로 나가지 않게 위치를 맞춘다. 안드로이드 클래스를 쓰지 않아 단위 테스트가 된다. */
internal object OverlayDragBounds {
    /**
     * 화면 가운데를 기준(0, 0)으로 놓인 창의 위치([x], [y])를 화면 안으로 맞춘다.
     * 창이 화면보다 크면 그 방향으로는 가운데(0)에 둔다.
     */
    fun centered(x: Int, y: Int, screenW: Int, screenH: Int, viewW: Int, viewH: Int): Pair<Int, Int> {
        val maxX = Math.max(0, (screenW - viewW) / 2)
        val maxY = Math.max(0, (screenH - viewH) / 2)
        return x.coerceIn(-maxX, maxX) to y.coerceIn(-maxY, maxY)
    }
}
