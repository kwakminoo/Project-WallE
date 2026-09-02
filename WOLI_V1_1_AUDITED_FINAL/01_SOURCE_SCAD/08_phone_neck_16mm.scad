
include <woli_lib.scad>;

// ============================================================
// 08 PHONE NECK Ø16 V0.2.1 - V1.1 RELEASE
// FIX: sphere overlaps stem by 1.5 mm so STL is one connected solid.
// Ball diameter remains DESIGN V1 and requires physical fit test.
// ============================================================

stem_d=9;
stem_h=11.5;          // +1.5 mm overlap into ball
ball_overlap=1.5;

module taper(){
    hull(){
        translate([0,0,neck_base_h])
            rplate(46,34,1,4);
        translate([0,0,neck_base_h+neck_h])
            rplate(24,24,1,4);
    }
}

difference(){
    union(){
        rplate(neck_base_w,neck_base_l,neck_base_h,5);
        taper();

        translate([0,0,neck_base_h+neck_h])
            cylinder(h=stem_h,d=stem_d);

        // sphere bottom overlaps stem by ball_overlap
        translate([0,0,neck_base_h+neck_h+stem_h-ball_overlap+phone_ball_d/2])
            sphere(d=phone_ball_d);
    }

    for(x=[-neck_hole_x/2,neck_hole_x/2])
        for(y=[-neck_hole_y/2,neck_hole_y/2])
            translate([x,y,-0.1])
                cylinder(h=neck_base_h+0.3,d=m3_clear);
}
