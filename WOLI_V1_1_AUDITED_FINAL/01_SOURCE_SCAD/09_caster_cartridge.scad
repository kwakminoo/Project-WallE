
include <woli_lib.scad>;

// ============================================================
// 09 BALL CASTER CARTRIDGE V1.1
//
// Actual caster: 22 x 17 x 15 mm, quantity 1.
// No holes are drilled in the purchased caster.
// ============================================================

plate_w=40;
plate_l=34;
plate_t=4.0;
central_open=18.0;
pocket_w=caster_w+caster_body_clear;
pocket_l=caster_l+caster_body_clear;
pocket_d=1.5;

difference(){
    rplate(plate_w,plate_l,plate_t,5);

    // Central body / ball opening.
    translate([0,0,-0.1])
        rplate(central_open,central_open,plate_t+0.3,3);

    // Shallow locating recess.
    translate([0,0,plate_t-pocket_d])
        rplate(pocket_w,pocket_l,pocket_d+0.2,3);

    // Blind pilot holes for 09B retainer screws.
    for(x=[-caster_retainer_screw_cc/2,caster_retainer_screw_cc/2])
        translate([x,0,plate_t-3.2])
            cylinder(h=3.25,d=m3_pilot_d);

    // Clearance holes to chassis floor pilots.
    for(x=[-caster_mount_x,caster_mount_x])
        for(y=[-caster_mount_y,caster_mount_y])
            translate([x,y,-0.1])
                cylinder(h=plate_t+0.3,d=m3_clear);
}
