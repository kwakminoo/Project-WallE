
include <woli_lib.scad>;

// OPTIONAL TEST PART.
// USB-C cutout is deliberately generous because connector shell position
// on the breakout board still needs physical alignment verification.
hatch_w=50;
hatch_h=18;
hatch_t=2.4;

difference(){
    rplate(hatch_w,hatch_h,hatch_t,3);

    for(x=[-rear_hatch_screw_x,rear_hatch_screw_x])
        translate([x,0,-0.1])
            cylinder(h=hatch_t+0.3,d=m3_clear);

    translate([0,0,-0.1])
        rplate(12,6,hatch_t+0.3,2);
}
