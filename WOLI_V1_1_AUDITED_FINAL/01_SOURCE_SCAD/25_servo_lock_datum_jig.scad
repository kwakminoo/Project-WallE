
include <woli_lib.scad>;

// ============================================================
// 25 SERVO LOCK DATUM JIG V0.7
//
// Measurement aid, NOT final mechanism.
// Place over/near servo shaft and use ruler/caliper to report:
// A) shaft-center -> blocking point distance
// B) blocking direction
//
// Rings: 18 / 22 / 26 / 30 mm radius.
// ============================================================

jig_t=1.6;
ring_w=1.2;

difference(){
    union(){
        for(d=[36,44,52,60])
            difference(){
                cylinder(h=jig_t,d=d);
                translate([0,0,-0.1])
                    cylinder(h=jig_t+0.3,d=d-2*ring_w);
            }

        // crosshair datum
        translate([-31,-0.5,0]) cube([62,1,jig_t]);
        translate([-0.5,-31,0]) cube([1,62,jig_t]);
    }

    translate([0,0,-0.1])
        cylinder(h=jig_t+0.3,d=horn_center_clear_d);
}
