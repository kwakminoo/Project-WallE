
include <woli_lib.scad>;

// 04 PCB TRAY V0.2
// No unverified PCB hole pattern is used.

tray_t=3;
guide_h=5;
guide_t=2;

difference(){
    union(){
        rplate(pcb_tray_w,pcb_tray_l,tray_t,4);

        // edge guides
        for(x=[-1,1])
            for(y=[-1,1])
                translate([x*(pcb_w/2+1.2), y*(pcb_l/2-5), tray_t+guide_h/2])
                    cube([guide_t,10,guide_h],center=true);
    }

    // mounting holes to chassis bosses
    for(x=[-pcb_mount_cc_x/2,pcb_mount_cc_x/2])
        for(y=[-pcb_mount_cc_y/2,pcb_mount_cc_y/2])
            translate([x,y,-0.1])
                cylinder(h=tray_t+0.3,d=m3_clear);

    // cooling / cable slots
    for(y=[-22,0,22])
        translate([0,y,-0.1])
            rplate(32,6,tray_t+0.3,2);

    // zip tie slots
    for(x=[-20,20])
        translate([x,0,-0.1])
            rplate(tie_slot_w,24,tray_t+0.3,1.2);
}
