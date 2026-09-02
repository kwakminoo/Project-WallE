
include <woli_lib.scad>;

// ============================================================
// 22 UNIVERSAL SERVO-HORN ADAPTER V0.7
//
// PURPOSE
// - sits on TOP of the stock MG90S servo horn
// - avoids assuming spline tooth count / spline diameter
// - uses radial slots to accommodate different stock-horn hole locations
//
// IMPORTANT
// - horn_slot_d = 2.4mm is DESIGN/PENDING.
// - hardware team must confirm actual horn screw diameter.
// ============================================================

adapter_t = 3.2;

difference(){
    cylinder(h=adapter_t,d=lock_plate_d);

    // do not interfere with stock horn center screw
    translate([0,0,-0.1])
        cylinder(h=adapter_t+0.3,d=horn_center_clear_d);

    // four radial mounting slots
    for(a=[0,90,180,270])
        rotate([0,0,a])
            translate([horn_slot_radius,0,-0.1])
                rotate([0,0,90])
                    slot3d(horn_slot_len,horn_slot_d,adapter_t+0.3);
}
