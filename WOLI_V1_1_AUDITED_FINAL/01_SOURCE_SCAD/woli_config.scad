// ============================================================
// WOLI V1.1 AUDITED FINAL — MASTER PARAMETERS
// Units: mm
// ============================================================

$fn = 72;

// ---------- GLOBAL ----------
fit_clearance = 0.40;
wall = 3.0;
floor_t = 5.0;
cover_t = 2.8;
shell_r = 10.0;

m3_clear = 3.4;
m3_pilot_d = 2.8;
m3_pilot_depth = 4.2;

tie_slot_w = 3.8;
tie_slot_l = 18;

// ---------- BODY ----------
body_w = 170;
body_l = 90;
chassis_h = 31;
top_h = 34;

top_upper_w = 162;
top_upper_l = 80;
top_upper_r = 9;

// ---------- N20 DRIVE ----------
wheel_d = 43;
wheel_w = 17.5;

n20_x = 12;
n20_y = 33;
n20_z = 10;

n20_bracket_x = 25;
n20_bracket_y = 8.5;
n20_bracket_z = 11.5;
n20_bracket_hole_cc = 17;      // approx; slots absorb error

wheel_axis_ground_z = wheel_d/2; // reference only; actual chassis ground plane requires hardware check
wheel_opening_l = 25;            // side wheel cutout depth (Y), mm
motor_y = -body_l/2 + body_l/3;  // wheel center: 1/3 body depth in from front (-Y)
motor_slot_len = 8;
motor_bracket_zone_x = body_w/2 - 18;

// ---------- BALL CASTER ----------
caster_w = 22;
caster_l = 17;
caster_h = 15;
caster_ball_d_visual_ref = 11.5; // photo-only reference; not used for fit
caster_dock_y = body_l/2 - 21;

caster_body_clear = 0.7;
caster_retainer_t = 3.0;
caster_retainer_screw_cc = 27;

caster_mount_x = 15;
caster_mount_y = 12;
caster_floor_open = 18.5;

// ---------- MAIN PCB ----------
pcb_w = 50;
pcb_l = 70;
pcb_stack_h = 15;

pcb_tray_w = 56;
pcb_tray_l = 76;
pcb_mount_cc_x = 44;
pcb_mount_cc_y = 64;

// ---------- MG90S ----------
servo_a = 32.4;
servo_b = 32.5;
servo_c = 12;
servo_fit = 0.7;

// ---------- LIMIT SWITCH ----------
limit_w = 21;
limit_l = 23;
limit_h = 6;

// ---------- VL53L0X ----------
tof_w = 10.7;
tof_l = 25;
tof_h = 11.5;
tof_rail_clear = 0.7;

tof_bezel_w = 34;
tof_bezel_h = 19;

// ---------- PHONE HOLDER / NECK ----------
phone_ball_d = 18.0;            // physical socket fit still required
neck_h = 30;
neck_base_w = 54;
neck_base_l = 42;
neck_base_h = 5;
neck_hole_x = 38;
neck_hole_y = 26;

neck_recess_w = 62;
neck_recess_l = 50;
neck_recess_d = 1.2;

neck_pilot_d = 2.8;
neck_pilot_depth = 7.0;

// reference phone clearance envelope only
phone_ref_w = 165;
phone_ref_h = 82;
phone_ref_t = 12;

// ---------- KLIFE PD-Q2 BATTERY ----------
battery_w = 68;
battery_l = 143;
battery_h = 11;
battery_mass_g = 205;

battery_fit_clear = 0.8;
battery_tray_w = 84;
battery_tray_l = 146;
battery_tray_t = 2.4;
battery_rail_t = 2.5;
battery_rail_h = 5.5;
battery_center_y = -18;

battery_tray_mount_x = 39;
battery_tray_mount_y = 55;

// ---------- UPPER ELECTRONICS DECK ----------
bridge_w = 122;
bridge_l = 94;
bridge_t = 3.0;
bridge_leg_h = 20;
bridge_leg_x = 56;
bridge_leg_y = 36;

module_boss_h = 4.5;
module_boss_d = 8.0;
module_pilot_d = 2.8;

pcb_deck_cx = -25;
pcb_deck_cy = 0;

tb_deck_cx = 34;
tb_deck_cy = 17;

usbc_deck_cx = 36;
usbc_deck_cy = -24;

// ---------- TB6612FNG HW-166 ----------
tb6612_w = 26;
tb6612_l = 26;
tb6612_clear = 0.6;

// total installed height is intentionally not hard-coded

// ---------- USB-C BREAKOUT ----------
usbc_board_w = 12;
usbc_board_l = 15;
usbc_connector_h = 4.2;
usbc_clear = 0.6;

// ---------- TOP COVER INTERFACE ----------
cover_skirt_depth = 4.0;
cover_skirt_clear = 0.8;
cover_skirt_t = 2.5;

cover_side_screw_y = 55 * body_l / 190;
cover_side_screw_z = -2.5;

rear_service_w = 42;
rear_service_h = 14;
rear_hatch_screw_x = 19;
rear_hatch_center_z = 10;

// ---------- CABLE MANAGEMENT ----------
cable_bridge_w = 24;
cable_bridge_l = 14;
cable_bridge_h = 8;
cable_bridge_channel = 8;

// ---------- SERVO LOCK FRAMEWORK ----------
lock_angle_deg = 90;
unlock_angle_deg = 10;

lock_plate_d = 28;
lock_arm_w = 10;
lock_arm_t = 4.0;
lock_reach = 22;
lock_tip_d = 10;

horn_center_clear_d = 7.0;
horn_slot_d = 2.4;              // physical stock-horn check required
horn_slot_len = 8.0;
horn_slot_radius = 8.0;

lock_pad_t = 2.0;
lock_pad_d = 11.0;
