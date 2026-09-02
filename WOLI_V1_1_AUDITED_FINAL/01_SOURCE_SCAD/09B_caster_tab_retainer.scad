
include <woli_lib.scad>;

ret_w=38;
ret_l=24;
ret_t=3.0;

difference(){
    rplate(ret_w,ret_l,ret_t,4);

    // Keep central round housing completely free.
    translate([0,0,-0.1])
        cylinder(h=ret_t+0.3,d=17.0);

    // Clearance holes; threads are formed in 09 base.
    for(x=[-caster_retainer_screw_cc/2,caster_retainer_screw_cc/2])
        translate([x,0,-0.1])
            cylinder(h=ret_t+0.3,d=m3_clear);
}
