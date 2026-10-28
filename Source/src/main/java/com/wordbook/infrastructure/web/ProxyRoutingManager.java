package com.wordbook.infrastructure.web;

import com.wordbook.infrastructure.ui.swing.ThemeManager;
import com.wordbook.infrastructure.ui.swing.VectorIcons;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.PasswordAuthentication;

/**
 * Intelligent VPN, Proxy & Routing IP Address Manager for the in-built browser.
 * Provides system-level routing through custom HTTP/HTTPS proxies, SOCKS5 tunnels (Tor/VPN),
 * and local security gateways (e.g., 127.0.0.1:8000).
 */
public class ProxyRoutingManager {

    public enum RoutingMode {
        DIRECT("Direct Connection (No Proxy)", "Direct internet connection with real IP address"),
        GATEWAY_LOCAL("Local CyberGuard Gateway (127.0.0.1:8000)", "Route traffic through local PHP/CyberGuard router at 127.0.0.1:8000"),
        HTTP_PROXY("HTTP / HTTPS Proxy", "Custom HTTP/HTTPS proxy or VPN gateway IP address"),
        SOCKS5_PROXY("SOCKS5 Proxy (Tor / SSH / VPN Tunnel)", "SOCKS5 routing protocol for Tor, Shadowsocks, or SSH tunnels");

        private final String displayName;
        private final String description;

        RoutingMode(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    private static boolean enabled = false;
    private static RoutingMode activeMode = RoutingMode.DIRECT;
    private static String host = "127.0.0.1";
    private static int port = 8000;
    private static String username = "";
    private static String password = "";
    private static String bypassHosts = "localhost, 127.0.0.1";

    static {
        // Initialize global CookieManager so Cloudflare and auth cookies are properly maintained
        if (CookieHandler.getDefault() == null) {
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        }
    }

    public static synchronized boolean isEnabled() {
        return enabled;
    }

    public static synchronized RoutingMode getActiveMode() {
        return activeMode;
    }

    public static synchronized String getHost() {
        return host;
    }

    public static synchronized int getPort() {
        return port;
    }

    public static synchronized String getStatusBadge() {
        if (!enabled || activeMode == RoutingMode.DIRECT) {
            return "🟢 Direct";
        }
        return "🛡️ " + host + ":" + port;
    }

    public static synchronized String getStatusTooltip() {
        if (!enabled || activeMode == RoutingMode.DIRECT) {
            return "Inbuilt Browser: Direct connection (no proxy active). Click to configure VPN/Proxy.";
        }
        return "Inbuilt Browser: Traffic routed through " + activeMode.getDisplayName() + " [" + host + ":" + port + "]";
    }

    /**
     * Applies routing configuration to Java system networking properties.
     */
    public static synchronized void applyRouting() {
        if (enabled && activeMode != RoutingMode.DIRECT && host != null && !host.isBlank() && port > 0) {
            String cleanHost = host.trim();
            String cleanPort = String.valueOf(port);

            if (activeMode == RoutingMode.SOCKS5_PROXY) {
                System.setProperty("socksProxyHost", cleanHost);
                System.setProperty("socksProxyPort", cleanPort);
                System.clearProperty("http.proxyHost");
                System.clearProperty("http.proxyPort");
                System.clearProperty("https.proxyHost");
                System.clearProperty("https.proxyPort");
            } else {
                // HTTP / HTTPS / Local Gateway
                System.setProperty("http.proxyHost", cleanHost);
                System.setProperty("http.proxyPort", cleanPort);
                System.setProperty("https.proxyHost", cleanHost);
                System.setProperty("https.proxyPort", cleanPort);
                System.clearProperty("socksProxyHost");
                System.clearProperty("socksProxyPort");
            }

            if (bypassHosts != null && !bypassHosts.isBlank()) {
                System.setProperty("http.nonProxyHosts", bypassHosts.replaceAll("\\s+", "|"));
            }

            if (username != null && !username.isBlank()) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password != null ? password.toCharArray() : new char[0]);
                    }
                });
            }
        } else {
            // Clear all proxy settings for Direct mode
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
            System.clearProperty("https.proxyHost");
            System.clearProperty("https.proxyPort");
            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
        }
    }

    /**
     * Displays the interactive VPN & Routing IP Address configuration dialog.
     */
    public static void showRoutingDialog(Component parent, Runnable onApply) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Inbuilt Browser — VPN & Proxy Routing IP", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(18, 22, 18, 22));
        mainPanel.setBackground(ThemeManager.getSurface());

        // Header Banner
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel titleLbl = new JLabel("🛡️ Inbuilt Browser VPN & Routing IP Manager");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLbl.setForeground(new Color(15, 23, 42));
        headerPanel.add(titleLbl, BorderLayout.NORTH);

        JLabel subLbl = new JLabel("Route embedded AI web traffic through your VPN, local gateway, or proxy server.");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl.setForeground(new Color(100, 116, 139));
        subLbl.setBorder(new EmptyBorder(4, 0, 12, 0));
        headerPanel.add(subLbl, BorderLayout.SOUTH);
        mainPanel.add(headerPanel);

        // Enable Checkbox
        JCheckBox enableBox = new JCheckBox("Enable Traffic Routing & Proxy for Inbuilt Browser", enabled);
        enableBox.setFont(new Font("Segoe UI", Font.BOLD, 12));
        enableBox.setForeground(new Color(30, 41, 59));
        enableBox.setOpaque(false);
        mainPanel.add(enableBox);
        mainPanel.add(Box.createVerticalStrut(12));

        // Quick Preset Chips Panel
        JPanel presetBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        presetBox.setOpaque(false);
        presetBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                " Quick Routing Presets ",
                0, 0,
                new Font("Segoe UI", Font.BOLD, 11),
                new Color(71, 85, 105)
        ));

        // Form Fields
        JComboBox<RoutingMode> modeCombo = new JComboBox<>(RoutingMode.values());
        modeCombo.setSelectedItem(activeMode);
        modeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JTextField hostField = new JTextField(host, 16);
        hostField.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JTextField portField = new JTextField(String.valueOf(port), 6);
        portField.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JTextField userField = new JTextField(username, 10);
        userField.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPasswordField passField = new JPasswordField(password, 10);
        passField.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Preset action buttons
        JButton presetGatewayBtn = new JButton("⚡ CyberGuard (127.0.0.1:8000)");
        presetGatewayBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        presetGatewayBtn.addActionListener(e -> {
            enableBox.setSelected(true);
            modeCombo.setSelectedItem(RoutingMode.GATEWAY_LOCAL);
            hostField.setText("127.0.0.1");
            portField.setText("8000");
        });
        presetBox.add(presetGatewayBtn);

        JButton presetSocksBtn = new JButton("🔒 SOCKS5 (127.0.0.1:1080)");
        presetSocksBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        presetSocksBtn.addActionListener(e -> {
            enableBox.setSelected(true);
            modeCombo.setSelectedItem(RoutingMode.SOCKS5_PROXY);
            hostField.setText("127.0.0.1");
            portField.setText("1080");
        });
        presetBox.add(presetSocksBtn);

        JButton presetTorBtn = new JButton("🧅 Tor (127.0.0.1:9050)");
        presetTorBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        presetTorBtn.addActionListener(e -> {
            enableBox.setSelected(true);
            modeCombo.setSelectedItem(RoutingMode.SOCKS5_PROXY);
            hostField.setText("127.0.0.1");
            portField.setText("9050");
        });
        presetBox.add(presetTorBtn);

        JButton presetDirectBtn = new JButton("🌐 Direct (No Proxy)");
        presetDirectBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        presetDirectBtn.addActionListener(e -> {
            enableBox.setSelected(false);
            modeCombo.setSelectedItem(RoutingMode.DIRECT);
        });
        presetBox.add(presetDirectBtn);

        mainPanel.add(presetBox);
        mainPanel.add(Box.createVerticalStrut(10));

        // Form Grid
        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 4, 5, 4);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        JLabel modeLbl = new JLabel("Routing Mode:");
        modeLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formGrid.add(modeLbl, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.7;
        formGrid.add(modeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel hostLbl = new JLabel("IP Address / Host:");
        hostLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formGrid.add(hostLbl, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        formGrid.add(hostField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JLabel portLbl = new JLabel("Port:");
        portLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formGrid.add(portLbl, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        formGrid.add(portField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        JLabel authLbl = new JLabel("Auth (Optional):");
        authLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        formGrid.add(authLbl, gbc);

        JPanel authPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        authPanel.setOpaque(false);
        authPanel.add(new JLabel("User:"));
        authPanel.add(userField);
        authPanel.add(new JLabel("Pass:"));
        authPanel.add(passField);
        gbc.gridx = 1; gbc.gridy = 3;
        formGrid.add(authPanel, gbc);

        mainPanel.add(formGrid);
        mainPanel.add(Box.createVerticalStrut(14));

        // Note Banner
        JLabel noteLbl = new JLabel("<html><font color='#2563eb'><b>Tip:</b></font> When Claude or Cloudflare displays a bot-block screen, routing through your local gateway (127.0.0.1:8000) or VPN proxy bypasses host restrictions. You can also click <b>↗ External</b> to open in native Chrome/Safari.</html>");
        noteLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        noteLbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(191, 219, 254), 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        noteLbl.setOpaque(true);
        noteLbl.setBackground(new Color(239, 246, 255));
        mainPanel.add(noteLbl);

        dialog.add(mainPanel, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        bottomBar.setBackground(ThemeManager.getSurface());
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelBtn.addActionListener(e -> dialog.dispose());
        bottomBar.add(cancelBtn);

        JButton applyBtn = new JButton("✓ Save & Apply Routing");
        applyBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        applyBtn.setBackground(new Color(22, 163, 74));
        applyBtn.setForeground(Color.WHITE);
        applyBtn.setOpaque(true);
        applyBtn.setBorderPainted(false);
        applyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        applyBtn.addActionListener(e -> {
            synchronized (ProxyRoutingManager.class) {
                enabled = enableBox.isSelected();
                activeMode = (RoutingMode) modeCombo.getSelectedItem();
                host = hostField.getText().trim();
                try {
                    port = Integer.parseInt(portField.getText().trim());
                } catch (Exception ex) {
                    port = 8000;
                }
                username = userField.getText().trim();
                password = new String(passField.getPassword());
                applyRouting();
            }
            dialog.dispose();
            if (onApply != null) {
                onApply.run();
            }
        });
        bottomBar.add(applyBtn);

        dialog.add(bottomBar, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
