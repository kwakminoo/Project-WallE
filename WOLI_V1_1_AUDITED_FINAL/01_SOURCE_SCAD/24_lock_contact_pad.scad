
include <woli_lib.scad>;

// ============================================================
// 24 LOCK CONTACT PAD V0.7
// Optional replaceable contact surface.
// Can be printed in PLA for prototype or softer material later.
// Attach with small adhesive/fastener after real contact point is verified.
// ============================================================

difference(){
    cylinder(h=lock_pad_t,d=lock_pad_d);

    // tiny center pilot hole; enlarge only if hardware requires screw
    translate([0,0,-0.1])
        cylinder(h=lock_pad_t+0.3,d=1.8);
}
