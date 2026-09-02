
include <woli_lib.scad>;

// Print 4 copies.
difference(){
    cylinder(h=bridge_leg_h,d=10);
    translate([0,0,-0.1])
        cylinder(h=bridge_leg_h+0.3,d=m3_clear);
}
