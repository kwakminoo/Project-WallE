
include <woli_lib.scad>;

// 10 REUSABLE CABLE CLIP V0.2
// Small printable clip for motor/sensor wire routing.

clip_w=16;
clip_l=12;
clip_h=7;
channel_w=7;

difference(){
    rplate(clip_w,clip_l,clip_h,3);

    translate([0,0,2.5])
        rotate([90,0,0])
            cylinder(h=clip_l+2,d=channel_w,center=true);

    translate([0,0,-0.1])
        cylinder(h=2.6,d=m3_clear);
}
