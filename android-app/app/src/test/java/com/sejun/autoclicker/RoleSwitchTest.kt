package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class RoleSwitchTest {
    @Test fun plainLeaderOrUserSeesNoBar() =
        assertEquals(false, RoleSwitch.model(owner = false, roster = false, userView = false, adminMode = false).visible)

    @Test fun rosterAdminSeesLeaderAndAdminOnly() {
        val m = RoleSwitch.model(owner = false, roster = true, userView = false, adminMode = true)
        assertEquals(true, m.visible)
        assertEquals(false, m.showUser)
        assertEquals(RoleSwitch.Seg.ADMIN, m.selected)
    }

    @Test fun rosterAdminInLeaderScreenSelectsLeader() =
        assertEquals(RoleSwitch.Seg.LEADER, RoleSwitch.model(owner = false, roster = true, userView = false, adminMode = false).selected)

    @Test fun developerAlsoSeesUserScreen() =
        assertEquals(true, RoleSwitch.model(owner = true, roster = true, userView = false, adminMode = true).showUser)

    @Test fun previewKeepsBarSoDeveloperCanReturn() {
        val m = RoleSwitch.model(owner = false, roster = false, userView = true, adminMode = true)
        assertEquals(true, m.visible)
        assertEquals(RoleSwitch.Seg.USER, m.selected)
    }
}
