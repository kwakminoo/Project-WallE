
include <woli_lib.scad>;

base_w=46; base_l=42; base_h=4;
pocket_w=servo_a+servo_fit;
pocket_d=servo_c+servo_fit;

difference(){
    union(){
        rplate(base_w,base_l,base_h,4);

        translate([-pocket_w/2-2,-pocket_d/2-2,base_h])
            cube([2,pocket_d+4,18]);

        translate([ pocket_w/2,-pocket_d/2-2,base_h])
            cube([2,pocket_d+4,18]);
    }

    for(x=[-14,14])
        translate([x,0,-0.1])
            slot3d(8,m3_clear,base_h+0.3);

    for(x=[-15,15])
        translate([x,0,-0.1])
            rplate(tie_slot_w,22,base_h+0.3,1.2);
}
