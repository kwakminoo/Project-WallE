
include <woli_lib.scad>;

// ============================================================
// 20 CABLE BRIDGE V0.5
// Low-profile screw-down cable retainer.
// Keeps motor/sensor wires away from wheel/servo motion.
// ============================================================

difference(){
    rplate(cable_bridge_w,cable_bridge_l,cable_bridge_h,3);

    // horizontal cable channel
    translate([0,0,cable_bridge_h/2+1])
        rotate([90,0,0])
            cylinder(h=cable_bridge_l+2,d=cable_bridge_channel,center=true);

    // M3 fixing hole
    translate([0,0,-0.1])
        cylinder(h=3.3,d=m3_clear);
}
