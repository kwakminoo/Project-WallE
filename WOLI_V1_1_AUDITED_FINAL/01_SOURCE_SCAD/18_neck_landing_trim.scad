
include <woli_lib.scad>;

// ============================================================
// 18 NECK LANDING TRIM V0.4
// Optional thin trim around neck base.
// It hides the seam between Neck and Top Cover.
// ============================================================

trim_t=1.4;
outer_w=60;
outer_l=48;
inner_w=neck_base_w+0.8;
inner_l=neck_base_l+0.8;

difference(){
    rplate(outer_w,outer_l,trim_t,7);
    translate([0,0,-0.1])
        rplate(inner_w,inner_l,trim_t+0.3,5.5);

    for(x=[-neck_hole_x/2,neck_hole_x/2])
        for(y=[-neck_hole_y/2,neck_hole_y/2])
            translate([x,y,-0.1])
                cylinder(h=trim_t+0.3,d=m3_clear);
}
