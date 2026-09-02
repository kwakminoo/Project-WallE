use <09C_caster_shim_parametric.scad>;
// wrapper
SHIM_H=2;
difference(){  rplate(40,34,2,5);  translate([0,0,-0.1]) rplate(caster_floor_open,caster_floor_open,2+0.3,3);  for(x=[-caster_mount_x,caster_mount_x]) for(y=[-caster_mount_y,caster_mount_y]) translate([x,y,-0.1]) cylinder(h=2+0.3,d=m3_clear);}
