
include <woli_component_refs.scad>;

// ============================================================
// 90 RELEASE ASSEMBLY V1.1
// Reference only. Simplified purchased parts are envelopes.
// ============================================================

deck_global_z = floor_t + bridge_leg_h;

// chassis
color([0.05,0.05,0.05])
    import("../02_STL_RELEASE/01_main_chassis.stl");

// battery tray + battery
color([0.12,0.12,0.12])
    translate([0,battery_center_y,floor_t])
        import("../02_STL_RELEASE/19B_klife_pd_q2_battery_tray.stl");

translate([0,battery_center_y,floor_t+battery_tray_t+battery_h/2])
    ref_battery();

// standoffs
for(x=[-bridge_leg_x,bridge_leg_x])
    for(y=[-bridge_leg_y,bridge_leg_y])
        color([0.18,0.18,0.18])
            translate([x,y,floor_t])
                import("../02_STL_RELEASE/21S_electronics_standoff_20mm.stl");

// upper electronics deck
color([0.16,0.16,0.16])
    translate([0,0,deck_global_z])
        import("../02_STL_RELEASE/21_upper_electronics_bridge.stl");

// PCB tray
color([0.22,0.22,0.22])
    translate([pcb_deck_cx,pcb_deck_cy,
               deck_global_z+bridge_t+module_boss_h])
        import("../02_STL_RELEASE/04_pcb_tray.stl");

// PCB envelope
translate([pcb_deck_cx,pcb_deck_cy,
           deck_global_z+bridge_t+module_boss_h+3+pcb_stack_h/2])
    ref_main_pcb();

// TB tray + board
color([0.18,0.18,0.18])
    translate([tb_deck_cx,tb_deck_cy,
               deck_global_z+bridge_t+module_boss_h])
        import("../02_STL_RELEASE/15_tb6612fng_tray.stl");
translate([tb_deck_cx,tb_deck_cy,
           deck_global_z+bridge_t+module_boss_h+4.0])
    ref_tb6612();

// USB holder + board
color([0.18,0.18,0.18])
    translate([usbc_deck_cx,usbc_deck_cy,
               deck_global_z+bridge_t+module_boss_h])
        import("../02_STL_RELEASE/16_usbc_breakout_holder.stl");
translate([usbc_deck_cx,usbc_deck_cy,
           deck_global_z+bridge_t+module_boss_h+4.0])
    ref_usbc();

// wheels
translate([ body_w/2+wheel_w/2-3,motor_y,wheel_axis_ground_z])
    rotate([0,0,90]) ref_wheel();
translate([-body_w/2-wheel_w/2+3,motor_y,wheel_axis_ground_z])
    rotate([0,0,90]) ref_wheel();

// motors
translate([ body_w/2-13,motor_y,13])
    rotate([0,0,90]) ref_n20_motor();
translate([-body_w/2+13,motor_y,13])
    rotate([0,0,90]) ref_n20_motor();

// caster cartridge + simplified actual caster
color([0.12,0.12,0.12])
    translate([0,caster_dock_y,floor_t])
        import("../02_STL_RELEASE/09_caster_cartridge.stl");

translate([0,caster_dock_y,floor_t+4])
    ref_caster_body();

// top cover
color([0.03,0.03,0.03])
    translate([0,0,chassis_h])
        import("../02_STL_RELEASE/03_top_cover.stl");

// neck seated in top recess
color([0.03,0.03,0.03])
    translate([0,-8,chassis_h+top_h-neck_recess_d])
        import("../02_STL_RELEASE/08_phone_neck_16mm.stl");

// horizontal phone clearance
translate([0,-8,chassis_h+top_h+60])
    ref_phone_clearance();
