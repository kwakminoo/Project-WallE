
include <woli_lib.scad>;

// ============================================================
// 15 TB6612FNG HW-166 TRAY V0.3.1 FINAL QA DESIGN
// Confirmed PCB plan size: 26 x 26 mm
// Total installed height: PENDING
//
// Conservative geometry:
// - continuous 40 x 40 mm base
// - low 2.2 mm perimeter guide around the PCB
// - open top
// - only two M3 mounting holes
// - no guessed PCB mounting-hole pattern
//
// Low guide height keeps the top-side solder pads accessible.
// ============================================================

base_w = 40;
base_l = 40;
base_t = 3;

pcb_clear = tb6612_clear;
pocket = tb6612_w + pcb_clear;   // 26.6 mm
guide_t = 1.8;
guide_h = 2.2;
guide_overlap = 0.7;
outer = pocket + 2*guide_t;

difference(){
    union(){
        rplate(base_w,base_l,base_t,5);

        // continuous low retaining frame, overlapped into base
        translate([0,0,base_t-guide_overlap])
            difference(){
                rplate(outer,outer,guide_h+guide_overlap,2.0);
                translate([0,0,-0.1])
                    rplate(pocket,pocket,guide_h+guide_overlap+0.2,1.2);
            }
    }

    // Two mounting holes, outside the PCB retaining frame
    for(y=[-17,17])
        translate([0,y,-0.1])
            cylinder(h=base_t+0.4,d=m3_clear);
}
