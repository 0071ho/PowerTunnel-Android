package ru.krlvm.powertunnel.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ru.krlvm.powertunnel.PowerTunnel;
import ru.krlvm.powertunnel.enums.SNITrick;
import tun.utils.Util;

public class PTManager {

    public static List<String> DNS_SERVERS = new ArrayList<>();
    public static boolean DNS_OVERRIDE = false;

    public static void configure(Context context, SharedPreferences prefs) {
        PowerTunnel.SNI_TRICK = prefs.getBoolean("sni", false) ? SNITrick.SPOIL : null;
        if(!prefs.contains("mitm_password")) {
            String newPassword = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("mitm_password", newPassword);
            editor.commit();
            PowerTunnel.MITM_PASSWORD = newPassword.toCharArray();
        } else {
            PowerTunnel.MITM_PASSWORD = prefs.getString("mitm_password", UUID.randomUUID().toString()).toCharArray();
        }
        PowerTunnel.DOT_AFTER_HOST_HEADER = prefs.getBoolean("dot_after_host", true);
        PowerTunnel.MIX_HOST_HEADER_CASE = prefs.getBoolean("mix_host_header_case", true);
        PowerTunnel.MIX_HOST_CASE = prefs.getBoolean("mix_host_case", false);
        PowerTunnel.COMPLETE_MIX_HOST_CASE = prefs.getBoolean("complete_mix_host_case", false);
        PowerTunnel.LINE_BREAK_BEFORE_GET = prefs.getBoolean("break_before_get", false);
        PowerTunnel.ADDITIONAL_SPACE_AFTER_GET = prefs.getBoolean("space_after_get", false);
        PowerTunnel.APPLY_HTTP_TRICKS_TO_HTTPS = prefs.getBoolean("apply_http_https", false);
        PowerTunnel.USE_DNS_SEC = prefs.getBoolean("use_dns_sec", false);
        PowerTunnel.ALLOW_REQUESTS_TO_ORIGIN_SERVER = prefs.getBoolean("allow_req_to_oserv", true);
        PowerTunnel.CHUNKING_ENABLED = prefs.getBoolean("chunking", true);
        PowerTunnel.FULL_CHUNKING = prefs.getBoolean("full_chunking", false);
        PowerTunnel.DEFAULT_CHUNK_SIZE = Integer.parseInt(prefs.getString("chunk_size", "2"));
        if (PowerTunnel.DEFAULT_CHUNK_SIZE < 1) {
            PowerTunnel.DEFAULT_CHUNK_SIZE = 2;
        }
        PowerTunnel.PAYLOAD_LENGTH = prefs.getBoolean("send_payload", false) ? 21 : 0;
        DNS_SERVERS = Util.getDefaultDNS(context);
        DNS_OVERRIDE = false;
        PowerTunnel.DOH_ADDRESS = null;
        if (prefs.getBoolean("override_dns", false) || DNS_SERVERS.isEmpty()) {
            String provider = prefs.getString("dns_provider", "CLOUDFLARE");
            DNS_OVERRIDE = true;
            if(provider.equals("SPECIFIED")) {
                String specifiedDnsProvider = prefs.getString("specified_dns_provider", "");
                if(!specifiedDnsProvider.trim().isEmpty()) {
                    if(specifiedDnsProvider.startsWith("https://")) {
                        PowerTunnel.DOH_ADDRESS = specifiedDnsProvider;
                    } else {
                        DNS_SERVERS.clear();
                        DNS_SERVERS.add(specifiedDnsProvider);
                    }
                }
            }
            if (!provider.contains("_DOH")) {
                DNS_SERVERS.clear();
                switch (provider) {
                    default:
                    case "CLOUDFLARE": {
                        DNS_SERVERS.add("1.1.1.1");
                        DNS_SERVERS.add("1.0.0.1");
                        break;
                    }
                    case "GOOGLE": {
                        DNS_SERVERS.add("8.8.8.8");
                        DNS_SERVERS.add("8.8.4.4");
                        break;
                    }
                    case "ADGUARD": {
                        DNS_SERVERS.add("176.103.130.130");
                        DNS_SERVERS.add("176.103.130.131");
                        break;
                    }
                }
            } else {
                switch (provider.replace("_DOH", "")) {
                    case "CLOUDFLARE": {
                        PowerTunnel.DOH_ADDRESS = "https://cloudflare-dns.com/dns-query";
                        break;
                    }
                    case "GOOGLE": {
                        PowerTunnel.DOH_ADDRESS = "https://dns.google/dns-query";
                        break;
                    }
                    case "ADGUARD": {
                        PowerTunnel.DOH_ADDRESS = "https://dns.adguard.com/dns-query";
                        break;
                    }
                    default: {
                        //Invalid setting, former SECDNS?
                        PowerTunnel.DOH_ADDRESS = null;
                        break;
                    }
                }
            }
        }
        String dnsPortVal = prefs.getString("dns_port", "");
        try {
            PowerTunnel.DNS_PORT = Integer.parseInt(dnsPortVal);
        } catch (NumberFormatException ex) {}
    }

    public static Exception safeStartProxy(Context context) {
        try {
            startProxy(context);
            return null;
        } catch (Exception ex) {
            //System.out.println("\n\n\n\nWHAT HAS FAILED? " + ex.getClass().getSimpleName() + " : " + ex.getMessage() + "\n\n\n\n");
            return ex;
        }
    }

    public static void startProxy(Context context) throws Exception {
        PowerTunnel.bootstrap();
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("cert_installed", false)) {
            context.sendBroadcast(new Intent(MainActivity.SERVER_START_BROADCAST));
        }
    }

    public static void stopProxy() {
        if (PowerTunnel.isRunning()) {
            PowerTunnel.stop();
        }
    }

    public static void serverStartupFailureBroadcast(Context context, Exception cause) {
        cause.printStackTrace();
        Intent intent = new Intent(MainActivity.STARTUP_FAIL_BROADCAST);
        intent.putExtra("cause", cause.getLocalizedMessage());
        context.sendBroadcast(intent);
    }
}
