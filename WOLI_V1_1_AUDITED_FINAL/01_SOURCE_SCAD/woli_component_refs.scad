
include <woli_lib.scad>;

// ============================================================
// WOLI COMPONENT REFERENCE LIBRARY V0.6
// REFERENCE ONLY. Off-the-shelf parts are NOT printed.
// Geometry uses confirmed outer dimensions where available.
// ============================================================

module ref_n20_motor(){
    color("silver")
        cube([n20_x,n20_y,n20_z],center=true);
}

module ref_n20_bracket(){
    color([0.65,0.65,0.65])
        cube([n20_bracket_x,n20_bracket_y,n20_bracket_z],center=true);
}

module ref_wheel(){
    color("black")
        rotate([90,0,0])
            cylinder(h=wheel_w,d=wheel_d,center=true);
}

module ref_caster_body(){
    // confirmed envelope only; ball diameter intentionally not modeled
    color("white")
        rplate(caster_w,caster_l,caster_h,3);
}

module ref_main_pcb(){
    color([0.08,0.5,0.15])
        cube([pcb_w,pcb_l,pcb_stack_h],center=true);
}

module ref_servo(){
    color([0.25,0.25,0.25])
        cube([servo_a,servo_c,servo_b],center=true);
}

module ref_limit_switch(){
    color([0.15,0.15,0.15])
        cube([limit_w,limit_h,limit_l],center=true);
}

module ref_tof(){
    color([0.1,0.55,0.6])
        cube([tof_l,tof_w,tof_h],center=true);
}

module ref_tb6612(){
    // height is PENDING: use thin reference slab + keepout volume.
    color([0.8,0.1,0.1])
        cube([tb6612_w,tb6612_l,2],center=true);
}

module ref_tb6612_keepout(h=12){
    color([1,0,0,0.15])
        cube([tb6612_w+2,tb6612_l+2,h],center=true);
}

module ref_usbc(){
    color([0.1,0.55,0.45])
        cube([usbc_board_w,usbc_board_l,usbc_connector_h],center=true);
}

module ref_phone_clearance(){
    color([0.05,0.1,0.8,0.18])
        cube([phone_ref_w,phone_ref_t,phone_ref_h],center=true);
}


module ref_battery(){
    color([0.08,0.08,0.08])
        cube([battery_w,battery_l,battery_h],center=true);
}
