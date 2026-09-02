
include <woli_config.scad>;

module rr2d(w,l,r){
    offset(r=r) square([w-2*r,l-2*r], center=true);
}

module rplate(w,l,h,r){
    linear_extrude(height=h) rr2d(w,l,r);
}

module rshell(w,l,h,r,t,bottom){
    difference(){
        rplate(w,l,h,r);
        translate([0,0,bottom])
            rplate(w-2*t,l-2*t,h,r-t);
    }
}

module slot2d(len,d){
    hull(){
        translate([-len/2,0]) circle(d=d);
        translate([ len/2,0]) circle(d=d);
    }
}

module slot3d(len,d,h){
    linear_extrude(height=h) slot2d(len,d);
}

module tie_slot(h=5){
    rplate(tie_slot_w,tie_slot_l,h,1.2);
}

module body_screw_pattern(h=20){
    for(x=[-screw_x,screw_x])
        for(y=[-screw_y,screw_y])
            translate([x,y,-1])
                cylinder(h=h,d=m3_clear);
}

module body_bosses(h=20){
    for(x=[-screw_x,screw_x])
        for(y=[-screw_y,screw_y])
            translate([x,y,0])
                cylinder(h=h,d=m3_boss_d);
}


// Closed-top shell for removable upper cover.
// Leaves the underside open and preserves a roof thickness.
module top_shell(w,l,h,r,t){
    difference(){
        rplate(w,l,h,r);
        translate([0,0,-0.1])
            rplate(w-2*t,l-2*t,h-t+0.1,r-t);
    }
}


// Tapered rounded solid built from two rounded plates.
// Useful for product-like enclosure shoulders.
module tapered_rounded_solid(w0,l0,w1,l1,h,r0,r1){
    hull(){
        rplate(w0,l0,0.8,r0);
        translate([0,0,h-0.8])
            rplate(w1,l1,0.8,r1);
    }
}

// Open-bottom tapered cover with closed roof.
module tapered_top_shell(w0,l0,w1,l1,h,r0,r1,t){
    difference(){
        tapered_rounded_solid(w0,l0,w1,l1,h,r0,r1);
        translate([0,0,-0.1])
            tapered_rounded_solid(
                w0-2*t,l0-2*t,
                w1-2*t,l1-2*t,
                h-t+0.1,
                max(r0-t,1),max(r1-t,1)
            );
    }
}
