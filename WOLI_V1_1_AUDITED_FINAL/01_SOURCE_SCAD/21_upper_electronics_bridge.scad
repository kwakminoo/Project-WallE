
include <woli_lib.scad>;

// ============================================================
// 21 UPPER ELECTRONICS DECK V1.1
//
// FDM: print flat plate on bed, bosses upward.
// Use 4x 21S standoffs below this plate.
// ============================================================

module module_boss(x,y){
    boss_overlap=0.6;
    translate([x,y,bridge_t-boss_overlap])
        difference(){
            cylinder(h=module_boss_h+boss_overlap,d=module_boss_d);
            translate([0,0,boss_overlap+0.5])
                cylinder(h=module_boss_h-0.45,d=module_pilot_d);
        }
}

difference(){
    union(){
        rplate(bridge_w,bridge_l,bridge_t,6);

        // Main PCB tray bosses.
        for(x=[pcb_deck_cx-pcb_mount_cc_x/2,pcb_deck_cx+pcb_mount_cc_x/2])
            for(y=[pcb_deck_cy-pcb_mount_cc_y/2,pcb_deck_cy+pcb_mount_cc_y/2])
                module_boss(x,y);

        // TB6612 tray bosses (tray holes are y ±17).
        for(y=[tb_deck_cy-17,tb_deck_cy+17])
            module_boss(tb_deck_cx,y);

        // USB-C holder bosses/slot targets.
        for(x=[usbc_deck_cx-7,usbc_deck_cx+7])
            module_boss(x,usbc_deck_cy-7);
    }

    // Standoff clearance holes.
    for(x=[-bridge_leg_x,bridge_leg_x])
        for(y=[-bridge_leg_y,bridge_leg_y])
            translate([x,y,-0.1])
                cylinder(h=bridge_t+0.3,d=m3_clear);

    // Central cable pass-through.
    // Kept away from every module mounting boss.
    translate([6,0,-0.1])
        rplate(16,10,bridge_t+0.3,3);
}
