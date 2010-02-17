/**
	Source Server Manager.
    Copyright (C) 2009  Matthew Livingstone

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
    I also ask that should you use this code, you give credit where credit is due.
 **/
package com.sourceservermanager;

import java.io.IOException;
import java.net.SocketTimeoutException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.method.NumberKeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sourceservermanager.rcon.SourceRcon;
import com.sourceservermanager.rcon.Rcon;
import com.sourceservermanager.rcon.exception.BadRcon;
import com.sourceservermanager.rcon.exception.ResponseEmpty;

public class sourceServerManager extends Activity {
	public static final String PREFS_NAME = "sourceServerManagerPrefs";
	public static final int MENU_ADD_SERVER = 0;
	public static final int MENU_EDIT_SERVER = 3;
	public static final int MENU_REMOVE_SERVER = 1;
	public static final int MENU_HELP = 4;
	public static final int MENU_QUIT = 2;
	public int serverCount;
	public String currentName;
	public String currentIP;
	public int currentPort;
	public String currentRconPass;
	public String serverResponse;
	public int editServerNumber;
	public boolean STATE_ADDING_SERVER = false;
	public boolean STATE_EDITING_SERVER = false;
	public boolean STATE_REMOVING_SERVER = false;
	public boolean STATE_VIEW_HELP = false;

	// Need handler for callbacks to the UI thread
	final Handler mHandler = new Handler();

	// Create runnable for posting server response from thread
	final Runnable mUpdateResults = new Runnable() {
		public void run() {
			final TextView rconRepsonseText = (TextView) findViewById(R.id.rconResponse);
			final ScrollView rconRepsonseScroll = (ScrollView) findViewById(R.id.rconResponseScroll);

			rconRepsonseText.append(serverResponse);
			// Force scroll to scroll to the bottom
			rconRepsonseScroll.scrollTo(0, rconRepsonseText.getHeight());
		}
	};

	/** Called when the activity is first created. **/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// All auto-complete-able RCON commands
		final String[] COMMANDS = new String[] { "_restart", "addip",
				"ai_auto_contact_solver", "ai_clear_bad_links",
				"ai_debug_assault", "ai_debug_directnavprobe",
				"ai_debug_doors", "ai_debug_efficiency", "ai_debug_enemies",
				"ai_debug_expressions", "ai_debug_follow", "ai_debug_loners",
				"ai_debug_looktargets", "ai_debug_los", "ai_debug_nav",
				"ai_debug_node_connect", "ai_debug_ragdoll_magnets",
				"ai_debug_shoot_positions", "ai_debug_speech",
				"ai_debug_squads", "ai_debug_think_ticks",
				"ai_debugscriptconditions", "ai_disable", "ai_drawbattlelines",
				"ai_dump_hints", "ai_efficiency_override",
				"ai_follow_use_points", "ai_follow_use_points_when_moving",
				"ai_lead_time", "ai_LOS_mode", "ai_moveprobe_debug",
				"ai_moveprobe_jump_debug", "ai_moveprobe_usetracelist",
				"ai_next_hull", "ai_no_local_paths", "ai_no_node_cache",
				"ai_no_select_box", "ai_no_steer", "ai_no_talk_delay",
				"ai_nodes", "ai_norebuildgraph",
				"ai_path_adjust_speed_on_immediate_turns",
				"ai_path_insert_pause_at_est_end",
				"ai_path_insert_pause_at_obstruction",
				"ai_reaction_delay_alert", "ai_reaction_delay_idle",
				"ai_rebalance_thinks", "ai_reloadresponsesystems",
				"ai_report_task_timings_on_limit", "ai_resume",
				"ai_sequence_debug", "ai_set_move_height_epsilon",
				"ai_shot_bias", "ai_shot_bias_max", "ai_shot_bias_min",
				"ai_shot_stats", "ai_shot_stats_term", "ai_show_connect",
				"ai_show_connect_fly", "ai_show_connect_jump",
				"ai_show_graph_connect", "ai_show_grid", "ai_show_hints",
				"ai_show_hull", "ai_show_hull_attacks", "ai_show_node",
				"ai_show_think_tolerance", "ai_show_visibility",
				"ai_simulate_task_overtime", "ai_spread_cone_focus_time",
				"ai_spread_defocused_cone_multiplier",
				"ai_spread_pattern_focus_time", "ai_step",
				"ai_think_limit_label", "ai_use_clipped_paths",
				"ai_use_efficiency", "ai_use_frame_think_limits",
				"ai_use_think_optimizations", "ainet_generate_report",
				"ainet_generate_report_only", "air_density", "alias",
				"ammo_338mag_max", "ammo_357sig_max", "ammo_45acp_max",
				"ammo_50AE_max", "ammo_556mm_box_max", "ammo_556mm_max",
				"ammo_57mm_max", "ammo_762mm_max", "ammo_9mm_max",
				"ammo_buckshot_max", "ammo_flashbang_max",
				"ammo_hegrenade_max", "ammo_smokegrenade_max", "banid",
				"bench_end", "bench_start", "bench_upload", "BindToggle",
				"bloodspray", "bot_add", "bot_add_ct", "bot_add_t",
				"bot_all_weapons", "bot_allow_grenades",
				"bot_allow_machine_guns", "bot_allow_pistols",
				"bot_allow_rifles", "bot_allow_rogues", "bot_allow_shield",
				"bot_allow_shotguns", "bot_allow_snipers",
				"bot_allow_sub_machine_guns", "bot_auto_vacate", "bot_chatter",
				"bot_crouch", "bot_debug", "bot_defer_to_human",
				"bot_difficulty", "bot_freeze", "bot_goto_mark",
				"bot_join_after_player", "bot_join_team", "bot_kick",
				"bot_kill", "bot_knives_only", "bot_memory_usage", "bot_mimic",
				"bot_mimic_yaw_offset", "bot_pistols_only", "bot_prefix",
				"bot_profile_db", "bot_quota", "bot_quota_match",
				"bot_show_nav", "bot_snipers_only", "bot_stop",
				"bot_traceview", "bot_walk", "bot_zombie",
				"breakable_disable_gib_limit", "breakable_multiplayer",
				"buddha", "bug_swap", "cache_print", "cast_hull", "cast_ray",
				"cc_lookup_crc", "ch_createairboat", "ch_createjeep",
				"changelevel", "changelevel2", "cl_clock_correction",
				"cl_clock_correction_force_server_tick",
				"cl_clock_showdebuginfo", "cl_clockdrift_max_ms", "cl_resend",
				"clear_debug_overlays", "clientport", "cmd",
				"collision_shake_amp", "collision_shake_freq",
				"collision_shake_time", "coop", "CreatePredictionError",
				"creditsdone", "cs_ShowStateTransitions",
				"cs_stacking_num_levels", "cvarlist", "deathmatch",
				"debug_physimpact", "decalfrequency", "developer",
				"differences", "disconnect", "disp_dynamic", "disp_modlimit",
				"disp_modlimit_down", "disp_modlimit_up", "disp_numiterations",
				"dispcoll_drawplane", "displaysoundlist", "drawcross",
				"drawline", "dti_flush", "dtwarning", "dtwatchent",
				"dtwatchvar", "dump_globals", "dumpstringtables", "echo",
				"endround", "ent_absbox", "ent_bbox", "ent_debugkeys",
				"ent_dump", "ent_fire", "ent_info", "ent_messages",
				"ent_messages_draw", "ent_name", "ent_pause", "ent_pivot",
				"ent_rbox", "ent_remove", "ent_remove_all", "ent_setname",
				"ent_show_response_criteria", "ent_step", "ent_text", "exec",
				"exit", "fadein", "fadeout", "find", "fire_absorbrate",
				"fire_dmgbase", "fire_dmginterval", "fire_dmgscale",
				"fire_extabsorb", "fire_extscale", "fire_growthrate",
				"fire_heatscale", "fire_incomingheatscale", "fire_maxabsorb",
				"firetarget", "flex_expression", "flex_looktime",
				"flex_maxawaytime", "flex_maxplayertime", "flex_minawaytime",
				"flex_minplayertime", "flex_talk", "flush", "flush_unlocked",
				"fog_enable_water_fog", "fov", "fps_max",
				"free_pass_peek_debug", "fs_printopenfiles",
				"fs_warning_level", "func_break_max_pieces",
				"func_breakdmg_bullet", "func_breakdmg_club",
				"func_breakdmg_explosive", "g_debug_doors",
				"g_debug_ragdoll_removal", "g_debug_trackpather",
				"g_debug_transitions", "g_Language", "g_ragdoll_maxcount",
				"give", "god", "groundlist", "heartbeat", "help",
				"hltv_autorecord", "hltv_connect", "hltv_debug", "hltv_delay",
				"hltv_maxclients", "hltv_maxrate", "hltv_port", "hltv_record",
				"hltv_retry", "hltv_snapshotinterval", "hltv_status",
				"hltv_stop", "hltv_stoprecord", "hltv_viewent",
				"host_framerate", "host_limitlocal", "host_map",
				"host_profile", "host_runofftime", "host_showcachemiss",
				"host_sleep", "host_speeds", "host_timescale", "hostage_debug",
				"hostname", "hostport", "hurtme", "incrementvar", "ip",
				"kdtree_test", "kick", "kill", "killserver", "listid",
				"listip", "listmodels", "log", "log_addaddress", "log_console",
				"log_events", "log_level", "log_udp", "lservercfgfile", "map",
				"map_background", "map_noareas", "map_showspawnpoints",
				"mapcyclefile", "maps", "mat_bumpbasis", "mat_configcurrent",
				"mat_envmapsize", "mat_envmaptgasize", "mat_fastspecular",
				"mat_forcedynamic", "mat_fullbright", "mat_leafvis",
				"mat_levelflush", "mat_loadtextures", "mat_luxels",
				"mat_maxframelatency", "mat_monitorgamma", "mat_norendering",
				"mat_normals", "mat_reloadallmaterials", "mat_reloadmaterial",
				"mat_reloadtextures", "mat_savechanges", "mat_setvideomode",
				"mat_shadowstate", "mat_showlightmappage", "mat_softwareskin",
				"mat_wireframe", "maxplayers", "mem_dump", "mem_dumpstats",
				"mem_force_flush", "mod_forcedata", "mp_allowNPCs",
				"mp_allowspectators", "mp_autocrosshair", "mp_autokick",
				"mp_autoteambalance", "mp_buytime", "mp_c4timer",
				"mp_chattime", "mp_decals", "mp_defaultteam",
				"mp_facefronttime", "mp_fadetoblack", "mp_falldamage",
				"mp_feetyawrate", "mp_flashlight", "mp_footsteps",
				"mp_forcecamera", "mp_forcerespawn", "mp_fraglimit",
				"mp_freezetime", "mp_friendlyfire", "mp_hostagepenalty",
				"mp_ik", "mp_limitteams", "mp_logecho", "mp_logfile",
				"mp_maxrounds", "mp_playerid", "mp_restartgame",
				"mp_roundtime", "mp_solidplayers", "mp_spawnprotectiontime",
				"mp_startmoney", "mp_teamlist", "mp_teamoverride",
				"mp_teamplay", "mp_timelimit", "mp_tkpunish", "mp_weaponstay",
				"mp_winlimit", "name", "nav_begin_area",
				"nav_clear_walkable_marks", "nav_connect", "nav_corner_lower",
				"nav_corner_raise", "nav_corner_select", "nav_crouch",
				"nav_delete", "nav_disconnect", "nav_edit", "nav_end_area",
				"nav_generate", "nav_jump", "nav_load", "nav_mark",
				"nav_mark_unnamed", "nav_mark_walkable", "nav_merge",
				"nav_no_jump", "nav_place_floodfill", "nav_place_pick",
				"nav_precise", "nav_quicksave", "nav_save",
				"nav_show_approach_points", "nav_show_danger", "nav_splice",
				"nav_split", "nav_strip", "nav_toggle_place_mode",
				"nav_toggle_place_painting", "nav_use_place", "net_blockmsg",
				"net_channels", "net_chokeloop", "net_drawslider",
				"net_droppackets", "net_fakelag", "net_fakeloss",
				"net_maxfragments", "net_showdrop", "net_showevents",
				"net_showfragments", "net_showmsg", "net_showpeaks",
				"net_showsplits", "net_showtcp", "net_showudp", "net_start",
				"net_synctags", "next", "noclip", "notarget",
				"npc_ammo_deplete", "npc_bipass", "npc_combat",
				"npc_conditions", "npc_create", "npc_create_aimed",
				"npc_create_equipment", "npc_destroy",
				"npc_destroy_unselected", "npc_enemies", "npc_focus",
				"npc_freeze", "npc_go", "npc_go_do_run", "npc_go_random",
				"npc_heal", "npc_height_adjust", "npc_kill", "npc_nearest",
				"npc_reset", "npc_route", "npc_select", "npc_sentences",
				"npc_speakall", "npc_squads", "npc_steering",
				"npc_steering_all", "npc_task_text", "npc_tasks",
				"npc_thinknow", "npc_viewcone", "npc_vphysics",
				"old_radiusdamage", "password", "path", "pause",
				"phys_impactforcescale", "phys_penetration_error_time",
				"phys_pushscale", "phys_speeds", "phys_stressbodyweights",
				"phys_swap", "phys_timescale", "phys_upimpactforcescale",
				"physics_budget", "physics_debug_entity",
				"physics_highlight_active", "physics_report_active",
				"physics_select", "physicsshadowupdate_render", "picker",
				"ping", "player_old_armor", "prop_crosshair", "prop_debug",
				"props_break_max_pieces", "quit", "quti", "r_3dnow",
				"r_AirboatViewDampenDamp", "r_AirboatViewDampenFreq",
				"r_AirboatViewZHeight", "r_colorstaticprops",
				"r_debugrandomstaticlighting", "r_decal_cullsize", "r_decals",
				"r_decalstaticprops", "r_DispBuildable", "r_DispDrawAxes",
				"r_DispEnableLOD", "r_DispFullRadius", "r_DispLockLOD",
				"r_DispRadius", "r_DispSetLOD", "r_DispTolerance",
				"r_DispUpdateAll", "r_DispUseStaticMeshes", "r_DispWalkable",
				"r_drawbatchdecals", "r_drawbrushmodels", "r_drawdecals",
				"r_DrawDisp", "r_drawentities", "r_drawleaf",
				"r_drawmodeldecals", "r_DrawModelLightOrigin",
				"r_drawmodelstatsoverlay", "r_drawmodelstatsoverlaydistance",
				"r_drawmodelstatsoverlaymax", "r_drawmodelstatsoverlaymin",
				"r_DrawSpecificStaticProp", "r_drawstaticprops",
				"r_drawtranslucentworld", "r_drawworld", "r_dynamic",
				"r_eyeglintlodpixels", "r_eyegloss", "r_eyemove", "r_eyes",
				"r_eyeshift_x", "r_eyeshift_y", "r_eyeshift_z", "r_eyesize",
				"r_fastzreject", "r_flex", "r_flushlod", "r_ForceRestore",
				"r_JeepViewDampenDamp", "r_JeepViewDampenFreq",
				"r_JeepViewZHeight", "r_lightaverage", "r_lightinterp",
				"r_lightmap", "r_lightstyle", "r_lockpvs", "r_lod",
				"r_lod_noupdate", "r_maxmodeldecal", "r_mmx",
				"r_modellodscale", "r_modelwireframedecal",
				"r_newproplighting", "r_nohw", "r_norefresh", "r_nosw",
				"r_novis", "r_occludeemaxarea", "r_occluderminarea",
				"r_occludermincount", "r_occlusion", "r_occlusionspew",
				"r_printdecalinfo", "r_rootlod", "r_shadowrendertotexture",
				"r_showenvcubemap", "r_skin", "r_sse", "r_sse2",
				"r_staticpropinfo", "r_teeth", "r_vehicleBrakeRate",
				"r_vehicleDrawDebug", "r_VehicleViewDampen", "r_visocclusion",
				"r_visualizetraces", "r_waterforceexpensive",
				"r_waterforcereflectentities", "rcon_password",
				"recompute_speed", "reload", "removeid", "removeip",
				"replaydelay", "report_entities", "report_simthinklist",
				"report_soundpatch", "report_touchlinks", "restart", "revert",
				"say", "say_team", "scene_allowoverrides", "scene_flatturn",
				"scene_flush", "scene_forcecombined", "scene_maxcaptionradius",
				"scene_print", "scene_showfaceto", "scene_showlook",
				"scene_showmoveto", "servercfgfile", "setang", "setmaster",
				"setmodel", "setpause", "setpos", "shake", "showhitlocation",
				"showtriggers", "showtriggers_toggle", "singlestep",
				"sk_ally_regen_time", "sk_npc_arm", "sk_npc_chest",
				"sk_npc_head", "sk_npc_leg", "sk_npc_stomach", "sk_player_arm",
				"sk_player_chest", "sk_player_head", "sk_player_leg",
				"sk_player_stomach", "skill", "smoothstairs",
				"snd_foliage_db_loss", "snd_gain", "snd_gain_max",
				"snd_gain_min", "snd_refdb", "snd_refdist", "snd_restart",
				"snd_vox_captiontrace", "snd_vox_globaltimeout",
				"snd_vox_sectimetout", "snd_vox_seqtimetout",
				"soundpatch_captionlength", "soundscape_flush", "spike",
				"status", "step_spline", "stuffcmds", "suitvolume",
				"surfaceprop", "sv_accelerate", "sv_airaccelerate",
				"sv_allowdownload", "sv_allowupload", "sv_alltalk",
				"sv_alternateticks", "sv_autosave", "sv_backspeed",
				"sv_bounce", "sv_cacheencodedents", "sv_cheats", "sv_contact",
				"sv_debug_player_use", "sv_debugmanualmode",
				"sv_debugresponses", "sv_deltaprint", "sv_deltatime",
				"sv_dumpresponses", "sv_filterban", "sv_findsoundname",
				"sv_footsteps", "sv_forcepreload", "sv_friction", "sv_gravity",
				"sv_hltv", "sv_instancebaselines", "sv_lan", "sv_logbans",
				"sv_massreport", "sv_max_usercmd_future_ticks", "sv_maxrate",
				"sv_maxspeed", "sv_maxunlag", "sv_maxupdaterate",
				"sv_maxvelocity", "sv_minrate", "sv_minupdaterate",
				"sv_netvisdist", "sv_noclipaccelerate", "sv_noclipduringpause",
				"sv_noclipspeed", "sv_npc_talker_maxdist", "sv_password",
				"sv_pausable", "sv_precachegeneric", "sv_precacheinfo",
				"sv_precachemodel", "sv_precachesound",
				"sv_pushaway_clientside", "sv_pushaway_clientside_size",
				"sv_pushaway_force", "sv_pushaway_max_force",
				"sv_pushaway_min_player_speed", "sv_rcon_banpenalty",
				"sv_rcon_maxfailures", "sv_rcon_minfailures",
				"sv_rcon_minfailuretime", "sv_region", "sv_rollangle",
				"sv_rollspeed", "sv_runcmds", "sv_sendtables",
				"sv_showhitboxes", "sv_showimpacts", "sv_showladders",
				"sv_skyname", "sv_soundemitter_filecheck",
				"sv_soundemitter_flush", "sv_soundemitter_trace",
				"sv_soundscape_printdebuginfo", "sv_specaccelerate",
				"sv_specnoclip", "sv_specspeed", "sv_stats", "sv_stepsize",
				"sv_stopspeed", "sv_stressbots", "sv_strict_notarget",
				"sv_suppress_viewpunch", "sv_teststepsimulation",
				"sv_thinktimecheck", "sv_timeout", "sv_unlag",
				"sv_unlockedchapters", "sv_voicecodec", "sv_voiceenable",
				"sv_wateraccelerate", "sv_waterdist", "sv_waterfriction",
				"template_debug", "Test_CreateEntity", "test_dispatcheffect",
				"Test_EHandle", "test_entity_blocker",
				"Test_InitRandomEntitySpawner", "Test_Loop", "Test_LoopCount",
				"Test_LoopForNumSeconds", "test_nav_opt",
				"Test_ProxyToggle_EnableProxy", "Test_ProxyToggle_SetValue",
				"Test_RandomChance", "Test_RandomizeInPVS",
				"Test_RandomPlayerPosition", "Test_RemoveAllRandomEntities",
				"Test_RunFrame", "Test_SendKey", "Test_SpawnRandomEntities",
				"Test_StartLoop", "Test_StartScript", "Test_Wait",
				"Test_WaitForCheckPoint", "testscript_debug", "think_limit",
				"trace_report", "unpause", "use", "user", "users",
				"vcollide_wireframe", "version", "violence_ablood",
				"violence_agibs", "violence_hblood", "violence_hgibs",
				"voice_inputfromfile", "voice_recordtofile",
				"voice_serverdebug", "vox_reload", "voxeltree_box",
				"voxeltree_playerview", "voxeltree_sphere", "voxeltree_view",
				"vprof", "vprof_cachemiss", "vprof_cachemiss_off",
				"vprof_cachemiss_on", "vprof_counters",
				"vprof_dump_groupnames", "vprof_dump_spikes",
				"vprof_generate_report", "vprof_generate_report_AI",
				"vprof_generate_report_AI_only",
				"vprof_generate_report_hierarchy",
				"vprof_generate_report_map_load", "vprof_off", "vprof_on",
				"vprof_playback_start", "vprof_playback_step",
				"vprof_playback_stop", "vprof_record_start",
				"vprof_record_stop", "vprof_reset", "vprof_reset_peaks",
				"vprof_vtune_group", "wait", "wc_air_edit_further",
				"wc_air_edit_nearer", "wc_air_node_edit", "wc_create",
				"wc_destroy", "wc_destroy_undo", "wc_link_edit",
				"wc_update_entity", "weapon_showproficiency", "writeid",
				"writeip" };

		// Grab serverCount preferences
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		serverCount = settings.getInt("serverCount", 0);

		// Grab all the UI elements we will need to read
		final AutoCompleteTextView rconCommandText = (AutoCompleteTextView) findViewById(R.id.rconCommand);
		final TextView rconRepsonseText = (TextView) findViewById(R.id.rconResponse);
		rconRepsonseText.setVerticalScrollBarEnabled(true);
		final ScrollView rconRepsonseScroll = (ScrollView) findViewById(R.id.rconResponseScroll);

		ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(this,
				android.R.layout.simple_dropdown_item_1line, COMMANDS);
		rconCommandText.setAdapter(adapter);

		rconRepsonseScroll.post(new Runnable() {
			public void run() {
				rconRepsonseScroll.fullScroll(ScrollView.FOCUS_DOWN);
			}
		});

		// Add OnClickListener for the send button
		final Button sendButton = (Button) findViewById(R.id.sendButton);
		sendButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				threadRconRequest();
			}
		});

		// onClickListener for the selectServerButton
		final Button serverSelectButton = (Button) findViewById(R.id.serverSelectButton);
		serverSelectButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				selectServer();
			}
		});

		// Listen for ENTER button when in command EditText
		rconCommandText.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					// return sendRconRequest();
					return threadRconRequest();
				}
				return false;
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Grab all data we want to carry over to next instance (after
		// orientation change)
		outState.putBoolean("STATE_ADDING_SERVER", STATE_ADDING_SERVER);
		outState.putBoolean("STATE_EDITING_SERVER", STATE_EDITING_SERVER);
		outState.putBoolean("STATE_REMOVING_SERVER", STATE_REMOVING_SERVER);
		outState.putBoolean("STATE_VIEW_HELP", STATE_VIEW_HELP);
		outState.putString("currentName", currentName);
		outState.putString("currentIP", currentIP);
		outState.putInt("currentPort", currentPort);
		outState.putString("currentRconPass", currentRconPass);

		final TextView rconRepsonseText = (TextView) findViewById(R.id.rconResponse);
		outState.putString("responseText", rconRepsonseText.getText()
				.toString());

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Restore all data stored
		STATE_ADDING_SERVER = savedInstanceState.getBoolean("STATE_ADDING_SERVER");
		STATE_EDITING_SERVER = savedInstanceState.getBoolean("STATE_EDITING_SERVER");
		STATE_REMOVING_SERVER = savedInstanceState.getBoolean("STATE_REMOVING_SERVER");
		STATE_VIEW_HELP = savedInstanceState.getBoolean("STATE_VIEW_HELP");
		currentName = savedInstanceState.getString("currentName");
		currentIP = savedInstanceState.getString("currentIP");
		currentPort = savedInstanceState.getInt("currentPort");
		currentRconPass = savedInstanceState.getString("currentRconPass");

		// Set the server name in the server selection button
		if (currentName != null) {
			final Button serverSelectButton = (Button) findViewById(R.id.serverSelectButton);
			serverSelectButton.setText(currentName + " "
					+ getString(R.string.serverSelectText));
		}

		// Restore all the response text from the server
		final TextView rconRepsonseText = (TextView) findViewById(R.id.rconResponse);
		rconRepsonseText.setText(savedInstanceState.getString("responseText"));

		// Restore different menus if user was in them when the rotate occurred
		if (STATE_ADDING_SERVER) {
			addNewServer();
		} else if (STATE_EDITING_SERVER) {
			editServerSelect();
		} else if (STATE_REMOVING_SERVER) {
			removeServer();
		} else if (STATE_VIEW_HELP) {
			showHelpDialog();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	protected void onDestory() {
		super.onDestroy();
	}

	/** Creates the menu items **/
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ADD_SERVER, 0, getString(R.string.addServerTitle)).setIcon(
				android.R.drawable.ic_menu_add);
		menu.add(0, MENU_EDIT_SERVER, 0, getString(R.string.editServerTitle)).setIcon(
				android.R.drawable.ic_menu_edit);
		menu.add(0, MENU_REMOVE_SERVER, 0, getString(R.string.removeServerText)).setIcon(
				android.R.drawable.ic_menu_delete);
		menu.add(0, MENU_HELP, 0, getString(R.string.helpText)).setIcon(
				android.R.drawable.ic_menu_help);
		menu.add(0, MENU_QUIT, 0, getString(R.string.quitText)).setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}

	/** Handles item selections **/
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ADD_SERVER:
			addNewServer();
			return true;
		case MENU_EDIT_SERVER:
			editServerSelect();
			return true;
		case MENU_REMOVE_SERVER:
			removeServer();
			return true;
		case MENU_HELP:
			showHelpDialog();
			return true;
		case MENU_QUIT:
			this.finish();
			return true;
		}
		return false;
	}

	/** Handles adding a new source server **/
	public boolean addNewServer() {
		// Used in restoring state after orientation flip
		STATE_ADDING_SERVER = true;

		// Create a dialog builder
		final Builder winAlert;
		Dialog winDialog;

		OnClickListener addListener = new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {

				// Save user preferences. We need an Editor object to make
				// changes.
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				SharedPreferences.Editor editor = settings.edit();

				// Make sure we look in the CURRENT dialog for the EditText
				// objects
				Dialog curDialog = (Dialog) dialog;
				EditText serverNameText = (EditText) curDialog
						.findViewById(R.id.serverName);
				EditText serverIPText = (EditText) curDialog
						.findViewById(R.id.serverIP);
				EditText serverPortText = (EditText) curDialog
						.findViewById(R.id.serverPort);
				EditText rconPassText = (EditText) curDialog
						.findViewById(R.id.rconPass);

				// Save these settings
				editor.putString("serverName" + serverCount, serverNameText
						.getText().toString());
				editor.putString("serverIP" + serverCount, serverIPText
						.getText().toString());
				editor.putInt("serverPort" + serverCount, Integer
						.parseInt(serverPortText.getText().toString()));
				editor.putString("rconPass" + serverCount, rconPassText
						.getText().toString());
				serverCount++;
				editor.putInt("serverCount", serverCount);

				// Commit changes
				editor.commit();

				// Close the dialog
				dialog.cancel();

				// Used in restoring state after orientation flip
				STATE_ADDING_SERVER = false;
				return;
			}
		};

		OnClickListener cancelListener = new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();

				// Used in restoring state after orientation flip
				STATE_ADDING_SERVER = false;
				return;
			}
		};

		LayoutInflater li = LayoutInflater.from(this);
		View view = li.inflate(R.layout.add_server, null);

		winAlert = new AlertDialog.Builder(this).setIcon(R.drawable.icon)
				.setTitle(getString(R.string.addServerTitle))
				.setPositiveButton(getString(R.string.addText), addListener).setNegativeButton(
						getString(R.string.cancelText), cancelListener).setView(view);

		// Ensure that we do not show the dialog if we rotate the screen
		// after pushing the back button
		winAlert.setOnKeyListener(new DialogInterface.OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					// Used in restoring state after orientation flip
					STATE_ADDING_SERVER = false;
				}
				return false;
			}
		});

		winDialog = winAlert.create();

		winDialog.show();

		// Only allow certain character in IP text
		EditText serverIPText = (EditText) winDialog
				.findViewById(R.id.serverIP);
		serverIPText.setKeyListener(new NumberKeyListener() {
			@Override
			protected char[] getAcceptedChars() {
				char[] numberChars = { '1', '2', '3', '4', '5', '6', '7', '8',
						'9', '0', '.' };
				return numberChars;
			}

			// Use numeric on-screen keyboard
			@Override
			public int getInputType() {
				return InputType.TYPE_CLASS_NUMBER;
			}
		});

		return true;
	}

	/** Handles selecting servers that have already been added to edit **/
	public boolean editServerSelect() {
		// Used in restoring state after orientation flip
		STATE_EDITING_SERVER = true;

		if (serverCount != 0) {
			final String[] serverNames = new String[serverCount];
			final String[] serverIPs = new String[serverCount];
			final int[] serverPorts = new int[serverCount];
			final String[] rconPasses = new String[serverCount];

			// Generate list of servers
			generateServerList(serverNames, serverIPs, serverPorts, rconPasses);

			// Allow the user to choose a server from the list
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.chooseServerTitle));
			builder.setSingleChoiceItems(serverNames, -1,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							editServerNumber = item;
							currentName = serverNames[item];
							currentIP = serverIPs[item];
							currentPort = serverPorts[item];
							currentRconPass = rconPasses[item];
							dialog.cancel();
							editServer();
						}
					});
			AlertDialog alert = builder.create();
			alert.show();
			return true;

		} else {
			showEmptyListDialog();

			// Used in restoring state after orientation flip
			STATE_EDITING_SERVER = false;
			return true;
		}
	}

	/** Handles editing servers that have already been added **/
	public void editServer() {
		// Create a dialog builder
		final Builder winAlert;
		Dialog winDialog;

		OnClickListener editListener = new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {

				// Save user preferences. We need an Editor object to make
				// changes.
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				SharedPreferences.Editor editor = settings.edit();

				// Make sure we look in the CURRENT dialog for the EditText
				// objects
				Dialog curDialog = (Dialog) dialog;
				EditText serverNameText = (EditText) curDialog
						.findViewById(R.id.serverName);
				EditText serverIPText = (EditText) curDialog
						.findViewById(R.id.serverIP);
				EditText serverPortText = (EditText) curDialog
						.findViewById(R.id.serverPort);
				EditText rconPassText = (EditText) curDialog
						.findViewById(R.id.rconPass);

				// Save these settings
				editor.putString("serverName" + editServerNumber,
						serverNameText.getText().toString());
				editor.putString("serverIP" + editServerNumber, serverIPText
						.getText().toString());
				editor.putInt("serverPort" + editServerNumber, Integer
						.parseInt(serverPortText.getText().toString()));
				editor.putString("rconPass" + editServerNumber, rconPassText
						.getText().toString());

				// Commit changes
				editor.commit();

				// Close the dialog
				dialog.cancel();

				// Used in restoring state after orientation flip
				STATE_EDITING_SERVER = false;
				return;
			}
		};

		OnClickListener cancelListener = new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();

				// Used in restoring state after orientation flip
				STATE_EDITING_SERVER = false;
				return;
			}
		};

		LayoutInflater li = LayoutInflater.from(this);
		View view = li.inflate(R.layout.edit_server, null);

		winAlert = new AlertDialog.Builder(this).setIcon(R.drawable.icon)
				.setTitle(getString(R.string.editServerTitle))
				.setPositiveButton(getString(R.string.saveText), editListener).setNegativeButton(
						getString(R.string.cancelText), cancelListener).setView(view);

		// Ensure that we do not show the dialog if we rotate the screen
		// after pushing the back button
		winAlert.setOnKeyListener(new DialogInterface.OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					// Used in restoring state after orientation flip
					STATE_EDITING_SERVER = false;
				}
				return false;
			}
		});

		winDialog = winAlert.create();

		winDialog.show();

		// Add in editing details
		EditText serverNameText = (EditText) winDialog
				.findViewById(R.id.serverName);
		EditText serverIPText = (EditText) winDialog
				.findViewById(R.id.serverIP);
		EditText serverPortText = (EditText) winDialog
				.findViewById(R.id.serverPort);
		EditText rconPassText = (EditText) winDialog
				.findViewById(R.id.rconPass);

		serverNameText.setText(currentName);
		serverIPText.setText(currentIP);
		serverPortText.setText(Integer.toString(currentPort));
		rconPassText.setText(currentRconPass);

		// Only allow certain characters in IP EditText
		serverIPText.setKeyListener(new NumberKeyListener() {
			@Override
			protected char[] getAcceptedChars() {
				char[] numberChars = { '1', '2', '3', '4', '5', '6', '7', '8',
						'9', '0', '.' };
				return numberChars;
			}

			// Use numeric on-screen keyboard
			@Override
			public int getInputType() {
				return InputType.TYPE_CLASS_NUMBER;
			}
		});

		return;
	}

	/**
	 * Generate a list of servers from the saved preferences.
	 * 
	 * @param serverNames
	 *            Empty array of length serverCount that will be filled with
	 *            server names.
	 * @param serverIPs
	 *            Empty array of length serverCount that will be filled with
	 *            server ip addresses.
	 * @param serverPorts
	 *            Empty array of length serverCount that will be filled with
	 *            server ports.
	 * @param rconPasses
	 *            Empty array of length serverCount that will be filled with
	 *            RCON passwords.
	 **/
	public void generateServerList(String[] serverNames, String[] serverIPs,
			int[] serverPorts, String[] rconPasses) {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME,
				MODE_PRIVATE);

		// Generate list of servers
		for (int i = 0; i < serverCount; i++) {
			serverNames[i] = settings.getString("serverName" + i, "NoName");
			serverIPs[i] = settings.getString("serverIP" + i, "192.168.1.1");
			serverPorts[i] = settings.getInt("serverPort" + i, 27015);
			rconPasses[i] = settings.getString("rconPass" + i, "");
		}
	}

	/** Handles selecting a server **/
	public boolean selectServer() {
		if (serverCount != 0) {
			final String[] serverNames = new String[serverCount];
			final String[] serverIPs = new String[serverCount];
			final int[] serverPorts = new int[serverCount];
			final String[] rconPasses = new String[serverCount];

			// Generate list of servers
			generateServerList(serverNames, serverIPs, serverPorts, rconPasses);

			// Allow the user to choose a server
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.chooseServerTitle));
			builder.setSingleChoiceItems(serverNames, -1,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							final Button serverSelectButton = (Button) findViewById(R.id.serverSelectButton);
							serverSelectButton.setText(serverNames[item] + " "
									+ getString(R.string.serverSelectText));
							currentName = serverNames[item];
							currentIP = serverIPs[item];
							currentPort = serverPorts[item];
							currentRconPass = rconPasses[item];
							dialog.cancel();
						}
					});
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		} else {
			showEmptyListDialog();
			return true;
		}
	}

	/** Handles removing a server **/
	public boolean removeServer() {
		// Used in restoring state after orientation flip
		STATE_REMOVING_SERVER = true;

		if (serverCount != 0) {
			final String[] serverNames = new String[serverCount];
			final String[] serverIPs = new String[serverCount];
			final int[] serverPorts = new int[serverCount];
			final String[] rconPasses = new String[serverCount];

			// Generate list of servers that can be removed
			generateServerList(serverNames, serverIPs, serverPorts, rconPasses);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.removeServerTitle));
			builder.setSingleChoiceItems(serverNames, -1,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							SharedPreferences settings = getSharedPreferences(
									PREFS_NAME, MODE_PRIVATE);
							SharedPreferences.Editor editor = settings.edit();

							// Shift all servers to the left by 1
							for (int i = item; i < serverCount - 1; i++) {
								editor.putString("serverName" + i,
										serverNames[i + 1]);
								editor.putString("serverIP" + i,
										serverIPs[i + 1]);
								editor.putInt("serverPort" + i,
										serverPorts[i + 1]);
								editor.putString("rconPass" + i,
										rconPasses[i + 1]);
							}
							// Remove the server in the last slot
							editor.remove("serverName" + (serverCount - 1));
							editor.remove("serverIP" + (serverCount - 1));
							editor.remove("serverPort" + (serverCount - 1));
							editor.remove("rconPass" + (serverCount - 1));
							serverCount--;
							editor.putInt("serverCount", serverCount);

							// Commit changes
							editor.commit();

							dialog.cancel();

							// Used in restoring state after orientation flip
							STATE_REMOVING_SERVER = false;
						}
					});

			// Ensure that we do not show the dialog if we rotate the screen
			// after pushing the back button
			builder.setOnKeyListener(new DialogInterface.OnKeyListener() {

				@Override
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_BACK) {
						// Used in restoring state after orientation flip
						STATE_REMOVING_SERVER = false;
					}
					return false;
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		}
		// Used in restoring state after orientation flip
		STATE_REMOVING_SERVER = false;
		return false;
	}

	protected boolean threadRconRequest() {
		final AutoCompleteTextView rconCommandText = (AutoCompleteTextView) findViewById(R.id.rconCommand);

		// Fire off a thread to do some work that we shouldn't do directly in
		// the UI thread
		Thread t = new Thread() {
			public void run() {
				sendRconRequest(rconCommandText.getText().toString());
				mHandler.post(mUpdateResults);
			}
		};
		t.start();
		return true;
	}

	/** Send the request to the server **/
	public void sendRconRequest(String command) {
		try {
			// Call Source (Half Life 2 & others) rcon without local port
			serverResponse = SourceRcon.send(currentIP, currentPort, currentRconPass, command);

		} catch (ResponseEmpty e) {
			serverResponse = getString(R.string.emptyRcon);
		} catch (BadRcon e) {
			// Wrong RCON password
			serverResponse = getString(R.string.badRcon);
		} catch (IOException e) {
			// The socket timed out on HL2 style, try HL1! (inefficient, I know, but I don't want to add anything to server prefs now)
			try {
				// Call HL1 rcon with local port 0
				serverResponse = Rcon.send(0, currentIP, currentPort, currentRconPass, command);
			} catch (ResponseEmpty e2) {
				serverResponse = getString(R.string.emptyRcon);
			} catch (SocketTimeoutException e2) {
				serverResponse = getString(R.string.socketTimeout);
			} catch (BadRcon e2) {
				// Wrong RCON password
				serverResponse = getString(R.string.badRcon);
			} catch (Exception e2) {
				// Something else happened...
				serverResponse = getString(R.string.failedRcon);
			}
		} catch (Exception e) {
			// Something else happened...
			serverResponse = getString(R.string.failedRcon);
		}
	}

	/**
	 * Shown a dialog to the user telling them that they need to add server
	 * details to continue
	 **/
	public void showEmptyListDialog() {
		new AlertDialog.Builder(sourceServerManager.this).setTitle(
				getString(R.string.app_name)).setMessage(
				getString(R.string.noServerMsg)).setNeutralButton(getString(R.string.closeText),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				}).show();
	}

	/**
	 * Shown a help dialog to the user
	 **/
	public void showHelpDialog() {
		STATE_VIEW_HELP = true;
		new AlertDialog.Builder(sourceServerManager.this).setTitle(
				getString(R.string.app_name)).setMessage(
				getString(R.string.helpMsg)).setNeutralButton(getString(R.string.closeText),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						STATE_VIEW_HELP = false;
						dialog.cancel();
					}
				}).show();
	}
}