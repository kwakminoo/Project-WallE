
include <woli_lib.scad>;

// ============================================================
// 01 MAIN CHASSIS V1.1 AUDITED
//
// FDM orientation: floor on build plate, open side upward.
//
// Attachment philosophy:
// - Upper deck standoffs: M3 clearance through standoff, self-thread into blind floor pilots.
// - Battery tray: M3 clearance through tray, self-thread into blind floor pilots.
// - Caster cartridge: M3 clearance through cartridge, self-thread into blind floor pilots.
// - Top cover: four short SIDE screws through chassis wall into cover-skirt pilot bosses.
//
// No nut is trapped between chassis and a redundant bottom cover.
// ============================================================

module blind_floor_pilot(x,y){
    translate([x,y,floor_t-m3_pilot_depth])
        cylinder(h=m3_pilot_depth+0.05,d=m3_pilot_d);
}

difference(){
    union(){
        rshell(body_w,body_l,chassis_h,shell_r,wall,floor_t);
    }

    // N20 metal bracket floor slots removed per chassis revision.

    // Wheel/body side clearance.
    for(sx=[-1,1])
        translate([sx*(body_w/2),motor_y,16])
            cube([22,wheel_opening_l,27],center=true);

    // Caster central pass-through only.
    translate([0,caster_dock_y,-0.1])
        rplate(caster_floor_open,caster_floor_open,floor_t+0.3,3);

    // Upper deck standoff blind pilots.
    for(x=[-bridge_leg_x,bridge_leg_x])
        for(y=[-bridge_leg_y,bridge_leg_y])
            blind_floor_pilot(x,y);

    // Battery tray blind pilots.
    for(x=[-battery_tray_mount_x,battery_tray_mount_x])
        for(y=[-battery_tray_mount_y,battery_tray_mount_y])
            blind_floor_pilot(x,y+battery_center_y);

    // Caster cartridge blind pilots.
    for(x=[-caster_mount_x,caster_mount_x])
        for(y=[-caster_mount_y,caster_mount_y])
            blind_floor_pilot(x,y+caster_dock_y);

    // Short side clearance holes for top-cover fastening.
    // Screws enter from outside of chassis side walls.
    for(sx=[-1,1])
        for(y=[-cover_side_screw_y,cover_side_screw_y])
            translate([sx*(body_w/2-1.5),y,chassis_h-2.5])
                rotate([0,90,0])
                    cylinder(h=wall+2.0,d=m3_clear,center=true);
}
