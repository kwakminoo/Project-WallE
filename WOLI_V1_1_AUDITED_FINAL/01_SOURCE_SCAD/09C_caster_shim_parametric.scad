
include <woli_lib.scad>;
shim_h = is_undef(SHIM_H) ? 1 : SHIM_H;

difference(){
    rplate(40,34,shim_h,5);

    translate([0,0,-0.1])
        rplate(caster_floor_open,caster_floor_open,shim_h+0.3,3);

    for(x=[-caster_mount_x,caster_mount_x])
        for(y=[-caster_mount_y,caster_mount_y])
            translate([x,y,-0.1])
                cylinder(h=shim_h+0.3,d=m3_clear);
}
