
include <woli_config.scad>;

gap=30;

color([0.05,0.05,0.05])
    import("../02_STL_RELEASE/01_main_chassis.stl");

color([0.12,0.12,0.12])
    translate([0,battery_center_y,gap])
        import("../02_STL_RELEASE/19B_klife_pd_q2_battery_tray.stl");

color([0.16,0.16,0.16])
    translate([0,0,gap*2])
        import("../02_STL_RELEASE/21_upper_electronics_bridge.stl");

color([0.03,0.03,0.03])
    translate([0,0,gap*3.2])
        import("../02_STL_RELEASE/03_top_cover.stl");

color([0.03,0.03,0.03])
    translate([0,-8,gap*5])
        import("../02_STL_RELEASE/08_phone_neck_16mm.stl");
