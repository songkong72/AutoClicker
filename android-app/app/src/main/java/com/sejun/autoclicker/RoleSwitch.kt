package com.sejun.autoclicker

/** 앱 첫 화면 위쪽의 화면 전환 막대(일반 화면 · 집결장 · 지휘관)에 무엇을 보일지 정한다. 안드로이드 클래스를 쓰지 않아 단위 테스트가 된다. */
internal object RoleSwitch {
    enum class Seg { USER, LEADER, ADMIN }

    /** [visible]이면 막대를 보인다. [showUser]면 "일반 화면" 칸도 보인다(개발자만). [selected]는 지금 보고 있는 화면. */
    data class Model(val visible: Boolean, val showUser: Boolean, val selected: Seg)

    /**
     * [owner] 개발자로 확인된 기기, [roster] 서버 지휘관 명단에 있는 기기, [userView] 개발자가 일반 화면을 미리 보는 중, [adminMode] 지휘관 화면.
     * 역할이 하나뿐인 기기(일반 사용자, 집결장)에는 막대를 보이지 않는다.
     */
    fun model(owner: Boolean, roster: Boolean, userView: Boolean, adminMode: Boolean): Model = Model(
        visible = owner || roster || userView,
        showUser = owner || userView,
        selected = when {
            userView -> Seg.USER
            adminMode -> Seg.ADMIN
            else -> Seg.LEADER
        }
    )
}
