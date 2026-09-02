
include <woli_lib.scad>;

// ============================================================
// 03 TOP COVER V1.1 AUDITED
//
// Print orientation:
// - rotate 180° so exterior roof faces build plate.
// - open cavity faces upward.
//
// Chassis interface:
// - inner locating skirt extends 4 mm downward into chassis.
// - 4 side screws enter from chassis exterior into cover pilot bosses.
//
// Neck:
// - neck base sits in shallow recess.
// - M3 screws self-thread into reinforced blind pilots.
// ============================================================

skirt_outer_w = body_w - 2*wall - cover_skirt_clear;
skirt_outer_l = body_l - 2*wall - cover_skirt_clear;
skirt_inner_w = skirt_outer_w - 2*cover_skirt_t;
skirt_inner_l = skirt_outer_l - 2*cover_skirt_t;

neck_seat_z = top_h - neck_recess_d;

module side_pilot_boss(sx,y){
    // Local boss attached to inner locating skirt.
    translate([sx*(skirt_outer_w/2-cover_skirt_t-2.0),y,-2.0])
        cube([7.0,14.0,8.0],center=true);
}

module rear_hatch_boss(x){
    translate([x,body_l/2-cover_t-2.0,rear_hatch_center_z])
        cube([8,7,10],center=true);
}

difference(){
    union(){
        tapered_top_shell(
            body_w,body_l,
            top_upper_w,top_upper_l,
            top_h,
            shell_r,top_upper_r,
            cover_t
        );

        // Internal neck reinforcement: does not protrude above exterior roof.
        translate([0,0,top_h-8])
            rplate(66,54,8,7);

        // Downward inner locating skirt.
        translate([0,0,-cover_skirt_depth])
            difference(){
                rplate(skirt_outer_w,skirt_outer_l,cover_skirt_depth+1.0,shell_r-wall-0.5);
                translate([0,0,-0.1])
                    rplate(skirt_inner_w,skirt_inner_l,cover_skirt_depth+1.2,
                           max(shell_r-wall-cover_skirt_t-0.5,1));
            }

        // Transition ring: physically connects the smaller locating skirt
        // to the cover's inner wall without increasing the inserted skirt size.
        translate([0,0,0])
            difference(){
                rplate(body_w-2*cover_t+1.2,
                       body_l-2*cover_t+1.2,
                       1.2,
                       shell_r-cover_t+0.3);
                translate([0,0,-0.1])
                    rplate(skirt_inner_w,
                           skirt_inner_l,
                           1.4,
                           max(shell_r-wall-cover_skirt_t-0.5,1));
            }

        // Four cover side-screw pilot bosses.
        for(sx=[-1,1])
            for(y=[-cover_side_screw_y,cover_side_screw_y])
                side_pilot_boss(sx,y);

        // Rear hatch pilot bosses.
        for(x=[-rear_hatch_screw_x,rear_hatch_screw_x])
            rear_hatch_boss(x);

        // Integrated front ToF guide rails.
        // Board plane is parallel to front wall and can be inserted before cover assembly.
        for(x=[-(tof_l+tof_rail_clear)/2-1.2,(tof_l+tof_rail_clear)/2+1.2])
            translate([x,-body_l/2+cover_t+2.4,top_h/2+1])
                cube([2.4,5.5,tof_w+4.0],center=true);

        // ToF lower ledge.
        translate([0,-body_l/2+cover_t+2.4,
                   top_h/2+1-(tof_w+tof_rail_clear)/2-1.2])
            cube([tof_l+5,5.5,2.4],center=true);
    }

    // Neck landing recess.
    translate([0,0,top_h-neck_recess_d])
        rplate(neck_recess_w,neck_recess_l,neck_recess_d+0.2,7);

    // Neck blind pilots. Do NOT cut through whole cover.
    for(x=[-neck_hole_x/2,neck_hole_x/2])
        for(y=[-neck_hole_y/2,neck_hole_y/2])
            translate([x,y,neck_seat_z-neck_pilot_depth])
                cylinder(h=neck_pilot_depth+0.05,d=neck_pilot_d);

    // Central neck cable pass-through.
    translate([0,0,top_h-10])
        cylinder(h=12,d=14);

    // Single rectangular ToF optical opening.
    translate([0,-body_l/2+cover_t/2,top_h/2+1])
        cube([tof_l+5,cover_t+3,tof_w+5],center=true);

    // Rear service opening.
    translate([0,body_l/2-cover_t/2,rear_hatch_center_z])
        cube([rear_service_w,cover_t+3,rear_service_h],center=true);

    // Rear service hatch screw pilot holes.
    for(x=[-rear_hatch_screw_x,rear_hatch_screw_x])
        translate([x,body_l/2-3.2,rear_hatch_center_z])
            rotate([90,0,0])
                cylinder(h=6.5,d=m3_pilot_d,center=true);

    // Side-screw blind pilots in cover skirt bosses.
    for(sx=[-1,1])
        for(y=[-cover_side_screw_y,cover_side_screw_y])
            translate([sx*(body_w/2-5.0),y,cover_side_screw_z])
                rotate([0,90,0])
                    cylinder(h=7.0,d=m3_pilot_d,center=true);

    // Hidden rear ventilation.
    for(x=[-36,-24,24,36])
        translate([x,body_l/2-cover_t/2,23])
            cube([7,cover_t+3,9],center=true);
}
