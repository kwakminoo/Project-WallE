
include <woli_lib.scad>;

// ============================================================
// 19B KLIFE PD-Q2 BATTERY TRAY V1.1
// 68 x 143 x 11 mm / approx 205 g
// ============================================================

pocket_w = battery_w + battery_fit_clear;
rail_x = pocket_w/2 + battery_rail_t/2;
rail_len = battery_l - 5;

difference(){
    union(){
        rplate(battery_tray_w,battery_tray_l,battery_tray_t,6);

        for(x=[-rail_x,rail_x])
            translate([x,0,battery_tray_t+battery_rail_h/2-0.5])
                cube([battery_rail_t,rail_len,battery_rail_h+1],center=true);
    }

    // M3 clearance to chassis blind pilots.
    for(x=[-battery_tray_mount_x,battery_tray_mount_x])
        for(y=[-battery_tray_mount_y,battery_tray_mount_y])
            translate([x,y,-0.1])
                cylinder(h=battery_tray_t+0.3,d=m3_clear);

    // Strap slots.
    for(y=[-42,42])
        for(x=[-30,30])
            translate([x,y,-0.1])
                rplate(4.0,20,battery_tray_t+0.3,1.2);

    // Weight reduction / air gap.
    for(y=[-48,-16,16,48])
        translate([0,y,-0.1])
            rplate(40,8,battery_tray_t+0.3,2.5);
}
