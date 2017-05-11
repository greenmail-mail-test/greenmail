<?php

/**
 * SquirrelMail Configuration File
 * Created using the configure script, conf.pl
 */

global $version;
$config_version = '1.4.0';
$config_use_color = 2;

$org_name      = "SquirrelMail";
$org_logo      = SM_PATH . 'images/sm_logo.png';
$org_logo_width  = '308';
$org_logo_height = '111';
$org_title     = "SquirrelMail $version";
$signout_page  = '';
$frame_top     = '_top';

$provider_uri     = 'http://squirrelmail.org/';

$provider_name     = 'SquirrelMail';

$motd = "";

$squirrelmail_default_language = 'en_US';
$default_charset       = 'iso-8859-1';
$lossy_encoding        = false;

$domain                 = trim(implode('', file('/etc/'.(file_exists('/etc/mailname')?'mail':'host').'name')));
$imapServerAddress      = 'greenmail';
$imapPort               = 3143;
$useSendmail            = false;
$smtpServerAddress      = 'greenmail';
$smtpPort               = 3025;
$sendmail_path          = '/usr/sbin/sendmail';
$sendmail_args          = '-i -t';
$pop_before_smtp        = false;
$pop_before_smtp_host   = '';
$imap_server_type       = 'other';
$invert_time            = false;
$optional_delimiter     = 'detect';
$encode_header_key      = '';

$default_folder_prefix          = '';
$trash_folder                   = 'INBOX.Trash';
$sent_folder                    = 'INBOX.Sent';
$draft_folder                   = 'INBOX.Drafts';
$default_move_to_trash          = true;
$default_move_to_sent           = true;
$default_save_as_draft          = true;
$show_prefix_option             = false;
$list_special_folders_first     = true;
$use_special_folder_color       = true;
$auto_expunge                   = true;
$default_sub_of_inbox           = true;
$show_contain_subfolders_option = false;
$default_unseen_notify          = 2;
$default_unseen_type            = 1;
$auto_create_special            = true;
$delete_folder                  = false;
$noselect_fix_enable            = false;

$data_dir                 = '/var/lib/squirrelmail/data/';
$attachment_dir           = '/var/spool/squirrelmail/attach/';
$dir_hash_level           = 0;
$default_left_size        = '150';
$force_username_lowercase = false;
$default_use_priority     = true;
$hide_sm_attributions     = false;
$default_use_mdn          = true;
$edit_identity            = true;
$edit_name                = true;
$hide_auth_header         = false;
$allow_thread_sort        = false;
$allow_server_sort        = false;
$allow_charset_search     = true;
$uid_support              = true;

$plugins[0] = 'view_as_html';

$theme_css = '';
$theme_default = 0;
$theme[0]['PATH'] = SM_PATH . 'themes/default_theme.php';
$theme[0]['NAME'] = 'Default';
$theme[1]['PATH'] = SM_PATH . 'themes/plain_blue_theme.php';
$theme[1]['NAME'] = 'Plain Blue';
$theme[2]['PATH'] = SM_PATH . 'themes/sandstorm_theme.php';
$theme[2]['NAME'] = 'Sand Storm';
$theme[3]['PATH'] = SM_PATH . 'themes/deepocean_theme.php';
$theme[3]['NAME'] = 'Deep Ocean';
$theme[4]['PATH'] = SM_PATH . 'themes/slashdot_theme.php';
$theme[4]['NAME'] = 'Slashdot';
$theme[5]['PATH'] = SM_PATH . 'themes/purple_theme.php';
$theme[5]['NAME'] = 'Purple';
$theme[6]['PATH'] = SM_PATH . 'themes/forest_theme.php';
$theme[6]['NAME'] = 'Forest';
$theme[7]['PATH'] = SM_PATH . 'themes/ice_theme.php';
$theme[7]['NAME'] = 'Ice';
$theme[8]['PATH'] = SM_PATH . 'themes/seaspray_theme.php';
$theme[8]['NAME'] = 'Sea Spray';
$theme[9]['PATH'] = SM_PATH . 'themes/bluesteel_theme.php';
$theme[9]['NAME'] = 'Blue Steel';
$theme[10]['PATH'] = SM_PATH . 'themes/dark_grey_theme.php';
$theme[10]['NAME'] = 'Dark Grey';
$theme[11]['PATH'] = SM_PATH . 'themes/high_contrast_theme.php';
$theme[11]['NAME'] = 'High Contrast';
$theme[12]['PATH'] = SM_PATH . 'themes/black_bean_burrito_theme.php';
$theme[12]['NAME'] = 'Black Bean Burrito';
$theme[13]['PATH'] = SM_PATH . 'themes/servery_theme.php';
$theme[13]['NAME'] = 'Servery';
$theme[14]['PATH'] = SM_PATH . 'themes/maize_theme.php';
$theme[14]['NAME'] = 'Maize';
$theme[15]['PATH'] = SM_PATH . 'themes/bluesnews_theme.php';
$theme[15]['NAME'] = 'BluesNews';
$theme[16]['PATH'] = SM_PATH . 'themes/deepocean2_theme.php';
$theme[16]['NAME'] = 'Deep Ocean 2';
$theme[17]['PATH'] = SM_PATH . 'themes/blue_grey_theme.php';
$theme[17]['NAME'] = 'Blue Grey';
$theme[18]['PATH'] = SM_PATH . 'themes/dompie_theme.php';
$theme[18]['NAME'] = 'Dompie';
$theme[19]['PATH'] = SM_PATH . 'themes/methodical_theme.php';
$theme[19]['NAME'] = 'Methodical';
$theme[20]['PATH'] = SM_PATH . 'themes/greenhouse_effect.php';
$theme[20]['NAME'] = 'Greenhouse Effect (Changes)';
$theme[21]['PATH'] = SM_PATH . 'themes/in_the_pink.php';
$theme[21]['NAME'] = 'In The Pink (Changes)';
$theme[22]['PATH'] = SM_PATH . 'themes/kind_of_blue.php';
$theme[22]['NAME'] = 'Kind of Blue (Changes)';
$theme[23]['PATH'] = SM_PATH . 'themes/monostochastic.php';
$theme[23]['NAME'] = 'Monostochastic (Changes)';
$theme[24]['PATH'] = SM_PATH . 'themes/shades_of_grey.php';
$theme[24]['NAME'] = 'Shades of Grey (Changes)';
$theme[25]['PATH'] = SM_PATH . 'themes/spice_of_life.php';
$theme[25]['NAME'] = 'Spice of Life (Changes)';
$theme[26]['PATH'] = SM_PATH . 'themes/spice_of_life_lite.php';
$theme[26]['NAME'] = 'Spice of Life - Lite (Changes)';
$theme[27]['PATH'] = SM_PATH . 'themes/spice_of_life_dark.php';
$theme[27]['NAME'] = 'Spice of Life - Dark (Changes)';
$theme[28]['PATH'] = SM_PATH . 'themes/christmas.php';
$theme[28]['NAME'] = 'Holiday - Christmas';
$theme[29]['PATH'] = SM_PATH . 'themes/darkness.php';
$theme[29]['NAME'] = 'Darkness (Changes)';
$theme[30]['PATH'] = SM_PATH . 'themes/random.php';
$theme[30]['NAME'] = 'Random (Changes every login)';
$theme[31]['PATH'] = SM_PATH . 'themes/midnight.php';
$theme[31]['NAME'] = 'Midnight';
$theme[32]['PATH'] = SM_PATH . 'themes/alien_glow.php';
$theme[32]['NAME'] = 'Alien Glow';
$theme[33]['PATH'] = SM_PATH . 'themes/dark_green.php';
$theme[33]['NAME'] = 'Dark Green';
$theme[34]['PATH'] = SM_PATH . 'themes/penguin.php';
$theme[34]['NAME'] = 'Penguin';
$theme[35]['PATH'] = SM_PATH . 'themes/minimal_bw.php';
$theme[35]['NAME'] = 'Minimal BW';
$theme[36]['PATH'] = SM_PATH . 'themes/redmond.php';
$theme[36]['NAME'] = 'Redmond';
$theme[37]['PATH'] = SM_PATH . 'themes/netstyle_theme.php';
$theme[37]['NAME'] = 'Net Style';
$theme[38]['PATH'] = SM_PATH . 'themes/silver_steel_theme.php';
$theme[38]['NAME'] = 'Silver Steel';
$theme[39]['PATH'] = SM_PATH . 'themes/simple_green_theme.php';
$theme[39]['NAME'] = 'Simple Green';
$theme[40]['PATH'] = SM_PATH . 'themes/wood_theme.php';
$theme[40]['NAME'] = 'Wood';
$theme[41]['PATH'] = SM_PATH . 'themes/bluesome.php';
$theme[41]['NAME'] = 'Bluesome';
$theme[42]['PATH'] = SM_PATH . 'themes/simple_green2.php';
$theme[42]['NAME'] = 'Simple Green 2';
$theme[43]['PATH'] = SM_PATH . 'themes/simple_purple.php';
$theme[43]['NAME'] = 'Simple Purple';
$theme[44]['PATH'] = SM_PATH . 'themes/autumn.php';
$theme[44]['NAME'] = 'Autumn';
$theme[45]['PATH'] = SM_PATH . 'themes/autumn2.php';
$theme[45]['NAME'] = 'Autumn 2';
$theme[46]['PATH'] = SM_PATH . 'themes/blue_on_blue.php';
$theme[46]['NAME'] = 'Blue on Blue';
$theme[47]['PATH'] = SM_PATH . 'themes/classic_blue.php';
$theme[47]['NAME'] = 'Classic Blue';
$theme[48]['PATH'] = SM_PATH . 'themes/classic_blue2.php';
$theme[48]['NAME'] = 'Classic Blue 2';
$theme[49]['PATH'] = SM_PATH . 'themes/powder_blue.php';
$theme[49]['NAME'] = 'Powder Blue';
$theme[50]['PATH'] = SM_PATH . 'themes/techno_blue.php';
$theme[50]['NAME'] = 'Techno Blue';
$theme[51]['PATH'] = SM_PATH . 'themes/turquoise.php';
$theme[51]['NAME'] = 'Turquoise';

$default_use_javascript_addr_book = false;
$abook_global_file = '';
$abook_global_file_writeable = false;
$abook_global_file_listing = true;
$abook_file_line_length = 2048;

$addrbook_dsn = '';
$addrbook_table = 'address';

$prefs_dsn = '';
$prefs_table = 'userprefs';
$prefs_user_field = 'user';
$prefs_key_field = 'prefkey';
$prefs_val_field = 'prefval';
$addrbook_global_dsn = '';
$addrbook_global_table = 'global_abook';
$addrbook_global_writeable = false;
$addrbook_global_listing = false;

$no_list_for_subscribe = false;
$smtp_auth_mech = 'none';
$imap_auth_mech = 'login';
$smtp_sitewide_user = '';
$smtp_sitewide_pass = '';
$use_imap_tls = false;
$use_smtp_tls = false;
$session_name = 'SQMSESSID';
$only_secure_cookies     = true;
$disable_security_tokens = false;
$check_referrer          = '';

$config_location_base    = '';

@include SM_PATH . 'config/config_local.php';

