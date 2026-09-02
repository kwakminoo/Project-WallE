
include <woli_lib.scad>;

// ============================================================
// 17 TOF FRONT BEZEL V0.4
// Cosmetic/service bezel around the single rectangular ToF window.
// Thin separate piece so appearance can be revised without reprinting cover.
// No lens/camera styling.
// ============================================================

bezel_t = 1.8;
opening_w = tof_l + 5.5;
opening_h = tof_w + 5.5;

difference(){
    rplate(tof_bezel_w,tof_bezel_h,bezel_t,3.2);
    translate([0,0,-0.1])
        rplate(opening_w,opening_h,bezel_t+0.3,2.0);
}
