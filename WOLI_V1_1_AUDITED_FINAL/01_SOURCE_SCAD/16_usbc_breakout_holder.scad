
include <woli_lib.scad>;

// ============================================================
// 16 USB-C FEMALE BREAKOUT HOLDER V0.3.1
// Confirmed board: approx 12 x 15 mm
// Connector thickness: approx 4.2 mm
// Front is left open so USB-C plug can be inserted.
// ============================================================

base_w = 26;
base_l = 24;
base_t = 3;
rail_h = 5.5;
board_w = usbc_board_w + usbc_clear;
board_l = usbc_board_l + usbc_clear;

difference(){
    union(){
        rplate(base_w,base_l,base_t,4);

        // left/right rails around board width
        for(x=[-1,1])
            translate([x*(board_w/2+1.0), 1,
                       base_t+rail_h/2])
                cube([2,board_l-2,rail_h],center=true);

        // rear stop only; front remains open for connector access
        translate([0, board_l/2+0.5,
                   base_t+rail_h/2])
            cube([board_w+4,2,rail_h],center=true);
    }

    // board bottom relief
    translate([0,1,-0.1])
        rplate(board_w-2,board_l-4,base_t+0.3,2);

    // two adjustable deck slots
    for(x=[-7,7])
        translate([x,-7,-0.1])
            slot3d(8,m3_clear,base_t+0.3);
}
