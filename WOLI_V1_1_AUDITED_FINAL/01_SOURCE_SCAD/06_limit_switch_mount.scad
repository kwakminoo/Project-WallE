
include <woli_lib.scad>;

bw=34; bl=36; bh=4;

difference(){
    union(){
        rplate(bw,bl,bh,4);

        translate([-limit_w/2-1.5,-limit_h/2-1,bh])
            cube([2,limit_h+2,8]);

        translate([ limit_w/2-0.5,-limit_h/2-1,bh])
            cube([2,limit_h+2,8]);
    }

    for(x=[-10,10])
        translate([x,0,-0.1])
            rotate([0,0,90])
                slot3d(10,m3_clear,bh+0.3);

    translate([0,0,-0.1])
        rplate(tie_slot_w,limit_l+3,bh+0.3,1.2);
}
