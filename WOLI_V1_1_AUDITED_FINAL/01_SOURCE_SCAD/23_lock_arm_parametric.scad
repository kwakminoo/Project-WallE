
include <woli_lib.scad>;

// ============================================================
// 23 PARAMETRIC LOCK ARM MODULE V0.7
// Call: lock_arm_custom(reach_mm)
// ============================================================

module lock_arm_custom(reach_mm=22){
    difference(){
        linear_extrude(height=lock_arm_t)
            hull(){
                circle(d=lock_plate_d);
                translate([reach_mm,0]) circle(d=lock_tip_d);
            }

        translate([0,0,-0.1])
            cylinder(h=lock_arm_t+0.3,d=horn_center_clear_d);

        for(a=[0,90,180,270])
            rotate([0,0,a])
                translate([horn_slot_radius,0,-0.1])
                    rotate([0,0,90])
                        slot3d(horn_slot_len,horn_slot_d,lock_arm_t+0.3);
    }
}

// Default preview when opening this file directly.
lock_arm_custom(lock_reach);
