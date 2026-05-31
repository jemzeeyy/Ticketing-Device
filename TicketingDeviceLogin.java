package ticketingdevice;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.Path2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TicketingDeviceLogin extends JFrame {

    private static final double SCALE = 0.35;

    private final Color BG_COLOR      = Color.decode("#FFCDC7");
    private final Color WHITE_PLATE   = Color.WHITE;
    private final Color INPUT_BG      = new Color(255, 124, 124, 97);
    private final Color LINK_COLOR    = Color.decode("#FF6767");
    private final Color BTN_BG        = Color.decode("#FFF6F6");
    private final Color TEXT_DARKPINK = Color.decode("#FF8B8B");

    public TicketingDeviceLogin() {
        setTitle("Victory Liner Ticketing Device");
        setSize(scale(1086), scale(2000));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(false);

        JPanel mainPanel = new JPanel(null);
        mainPanel.setBackground(BG_COLOR);

        // VICTORY label
        JLabel victoryLabel = new JLabel("VICTORY");
        victoryLabel.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, scale(220)));
        victoryLabel.setForeground(Color.WHITE);
        victoryLabel.setBounds(center(1150), scale(70), scale(1111), scale(243));
        victoryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(victoryLabel);

        // Gradient line
        JPanel gradientLine = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float[] fractions = {0.0f, 0.5f, 1.0f};
                Color[] colors = {Color.decode("#F01011"), Color.decode("#ED590F"), Color.decode("#EAEB0B")};
                g2.setPaint(new LinearGradientPaint(0, 0, getWidth(), 0, fractions, colors));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), scale(35), scale(35));
                g2.dispose();
            }
        };
        gradientLine.setBounds(center(984), scale(310), scale(984), scale(29));
        gradientLine.setOpaque(false);
        mainPanel.add(gradientLine);

        // Liner label
        JLabel linerLabel = new JLabel("Liner");
        linerLabel.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, scale(96)));
        linerLabel.setForeground(Color.WHITE);
        linerLabel.setBounds(0, scale(345), scale(1086), scale(116));
        linerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(linerLabel);

        // ── FIX 1: plate height increased to fit all fields ──────────────
        JPanel whitePlate = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE_PLATE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), scale(93), scale(93));
                g2.dispose();
            }
        };
        whitePlate.setBounds(center(1012), scale(498), scale(1012), scale(1050)); // was 1100
        whitePlate.setOpaque(false);

        // LOG IN heading
        JLabel loginLabel = new JLabel("LOG IN");
        loginLabel.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, scale(96)));
        loginLabel.setForeground(TEXT_DARKPINK);
        loginLabel.setBounds(0, scale(20), scale(1012), scale(140));
        loginLabel.setHorizontalAlignment(SwingConstants.CENTER);
        whitePlate.add(loginLabel);

        // ID Number
        whitePlate.add(createLabel("ID Number", 91, 170));
        RoundedTextField idInput = new RoundedTextField("Enter your IDNumber", INPUT_BG, scale(93));
        idInput.setBounds(centerInPlate(955, 1012), scale(230), scale(955), scale(95));
        whitePlate.add(idInput);

        // Password
        whitePlate.add(createLabel("Password", 91, 380));
        JPanel passWrapper = new JPanel(null);
        passWrapper.setOpaque(false);
        int pWrapW = scale(955), pWrapH = scale(95);
        passWrapper.setBounds(centerInPlate(955, 1012), scale(440), pWrapW, pWrapH);

        RoundedPasswordField passInput = new RoundedPasswordField("Enter your password", INPUT_BG, scale(93));
        passInput.setBounds(0, 0, pWrapW, pWrapH);

        EyeToggleButton eyeBtn = new EyeToggleButton();
        int eyeSize = scale(65);
        eyeBtn.setBounds(pWrapW - eyeSize - scale(25), (pWrapH - eyeSize) / 2, eyeSize, eyeSize);
        eyeBtn.addActionListener(e -> {
            boolean ns = !eyeBtn.isShowPassword();
            eyeBtn.setShowPassword(ns);
            passInput.setPasswordVisible(ns);
        });
        passWrapper.add(eyeBtn);
        passWrapper.add(passInput);
        whitePlate.add(passWrapper);

        // Conductor label + dropdown
        whitePlate.add(createLabel("Conductor", 91, 550));

        String[] conductors = {"Select your Conductor", "Ariston Valdivia", "Mateo Reyes", "Dhel Rikk Nabor", "+ Add Driver name"};
        RoundedComboBox conductorBox = new RoundedComboBox(conductors, INPUT_BG, scale(93));
        conductorBox.setBounds(centerInPlate(955, 1012), scale(605), scale(955), scale(95));
        whitePlate.add(conductorBox);

        // Bus Code label + field
        whitePlate.add(createLabel("Bus Code", 91, 730));

        // ── FIX 4: VL- prefix is locked; only 3 digits editable ──────────
        RoundedTextField busCodeInput = new RoundedTextField("", INPUT_BG, scale(93)) {
            {
                setText("VL-");
                setForeground(Color.DARK_GRAY);
                // Remove the placeholder focus listener inherited from RoundedTextField
                // and replace it with a caret guard only
                ((AbstractDocument) getDocument()).setDocumentFilter(new VLPrefixFilter());
            }
        };
        busCodeInput.setBounds(centerInPlate(955, 1012), scale(785), scale(955), scale(95));
        whitePlate.add(busCodeInput);

        // Forgot password link — repositioned below Bus Code
        JLabel forgotPass = new JLabel("<html><u>Forgot password?</u></html>");
        forgotPass.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, scale(35)));
        forgotPass.setForeground(LINK_COLOR);
        forgotPass.setBounds(scale(630), scale(890), scale(300), scale(48));
        whitePlate.add(forgotPass);

        mainPanel.add(whitePlate);

        // ── FIX 2: button Y positions pushed down to clear the taller plate ─
        JButton loginBtn = new JButton("LOGIN") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEXT_DARKPINK);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), scale(93), scale(93));
                super.paintComponent(g);
                g2.dispose();
            }
        };
        loginBtn.setBounds(center(890), scale(1580), scale(862), scale(150)); // was 1619
        loginBtn.setFont(new Font("SansSerif", Font.BOLD, scale(90)));
        loginBtn.setForeground(WHITE_PLATE);
        loginBtn.setContentAreaFilled(false);
        loginBtn.setBorderPainted(false);
        loginBtn.setFocusPainted(false);
        loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        loginBtn.addActionListener(e -> {
    String enteredID       = idInput.getText().trim();
    String enteredPassword = new String(passInput.getPassword());
    String busCode         = busCodeInput.getText().trim();
    String conductor       = (String) conductorBox.getSelectedItem();

    // Basic validation
    if (!busCode.matches("VL-\\d{3}")) {
        JOptionPane.showMessageDialog(this, "Please enter a valid Bus Code (VL-XXX).",
                "Input Error", JOptionPane.WARNING_MESSAGE);
        return;
    }

    if (conductor == null || conductor.equals("Select your Conductor")) {
        JOptionPane.showMessageDialog(this, "Please select a conductor.",
                "Input Error", JOptionPane.WARNING_MESSAGE);
        return;
    }

    try {
        Connection conn = DBConnection.getConnection();

        // CHANGE TABLE NAME IF IBA SA DB MO
        String sql = "SELECT * FROM conductors WHERE driver_id=? AND password=?";

        PreparedStatement pst = conn.prepareStatement(sql);

        pst.setInt(1, Integer.parseInt(enteredID));
        pst.setString(2, enteredPassword);

        ResultSet rs = pst.executeQuery();

        if (rs.next()) {

            SwingUtilities.invokeLater(() ->
                new TicketingDevice("", "", "").setVisible(true)
            );

            dispose();

        } else {
            JOptionPane.showMessageDialog(this,
                    "Wrong ID number or password.",
                    "Login Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        rs.close();
        pst.close();
        conn.close();

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this,
                "Database Error: " + ex.getMessage());
    }
});
        mainPanel.add(loginBtn);

        JButton createAccBtn = new JButton("Create Account") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), scale(93), scale(93));
                g2.setColor(LINK_COLOR);
                g2.setStroke(new BasicStroke(scale(6)));
                g2.drawRoundRect(scale(3), scale(3), getWidth() - scale(6), getHeight() - scale(6), scale(93), scale(93));
                super.paintComponent(g);
                g2.dispose();
            }
        };
        createAccBtn.setBounds(center(995), scale(1780), scale(955), scale(150)); // was 1800
        createAccBtn.setFont(new Font("SansSerif", Font.BOLD, scale(75)));
        createAccBtn.setForeground(LINK_COLOR);
        createAccBtn.setContentAreaFilled(false);
        createAccBtn.setBorderPainted(false);
        createAccBtn.setFocusPainted(false);
        createAccBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        mainPanel.add(createAccBtn);

        setContentPane(mainPanel);
    }

    // ── DocumentFilter: locks "VL-" prefix, allows only 3 digits after it ──
    class VLPrefixFilter extends DocumentFilter {
        private static final String PREFIX = "VL-";

        @Override
        public void insertString(FilterBypass fb, int offset, String str, AttributeSet attr)
                throws BadLocationException {
            if (str == null) return;
            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String proposed = current.substring(0, offset) + str + current.substring(offset);
            if (isValid(proposed)) super.insertString(fb, offset, str, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String str, AttributeSet attr)
                throws BadLocationException {
            if (str == null) str = "";
            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String proposed = current.substring(0, offset) + str + current.substring(offset + length);
            if (isValid(proposed)) super.replace(fb, offset, length, str, attr);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            String current = fb.getDocument().getText(0, fb.getDocument().getLength());
            String proposed = current.substring(0, offset) + current.substring(offset + length);
            if (isValid(proposed)) super.remove(fb, offset, length);
        }

        /** Text must start with "VL-" and have at most 3 digits after it. */
        private boolean isValid(String text) {
            if (!text.startsWith(PREFIX)) return false;
            String suffix = text.substring(PREFIX.length());
            return suffix.matches("\\d{0,3}");
        }
    }

    private int scale(double v)                                    { return (int) Math.round(v * SCALE); }
    private int center(double w)                                   { return scale((1086 - w) / 2); }
    private int centerInPlate(double cw, double pw)                { return scale((pw - cw) / 2); }
    private JLabel createLabel(String text, double x, double y) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, scale(40)));
        lbl.setForeground(Color.BLACK);
        lbl.setBounds(scale(x), scale(y), scale(351), scale(50));
        return lbl;
    }

    // ── Inner classes (unchanged except package-level visibility) ──────────

    class RoundedTextField extends JTextField {
        private Color bgColor; private int radius; private String placeholder;
        public RoundedTextField(String placeholder, Color bgColor, int radius) {
            this.placeholder = placeholder; this.bgColor = bgColor; this.radius = radius;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, scale(40), 0, scale(40)));
            setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, scale(35)));
            setForeground(TEXT_DARKPINK);
            if (!placeholder.isEmpty()) {
                setText(placeholder);
                addFocusListener(new FocusListener() {
                    @Override public void focusGained(FocusEvent e) {
                        if (getText().equals(placeholder)) { setText(""); setForeground(Color.DARK_GRAY); }
                    }
                    @Override public void focusLost(FocusEvent e) {
                        if (getText().isEmpty()) { setForeground(TEXT_DARKPINK); setText(placeholder); }
                    }
                });
            }
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor); g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            super.paintComponent(g2); g2.dispose();
        }
    }

    class RoundedPasswordField extends JPasswordField {
        private Color bgColor; private int radius; private String placeholder;
        private boolean isPlaceholderActive = true;
        private char defaultEchoChar; private boolean passwordVisible = false;
        public RoundedPasswordField(String placeholder, Color bgColor, int radius) {
            this.placeholder = placeholder; this.bgColor = bgColor; this.radius = radius;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, scale(40), 0, scale(110)));
            setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, scale(35)));
            setForeground(TEXT_DARKPINK);
            this.defaultEchoChar = getEchoChar();
            setEchoChar((char) 0); setText(placeholder);
            addFocusListener(new FocusListener() {
                @Override public void focusGained(FocusEvent e) {
                    if (isPlaceholderActive) {
                        setText(""); setEchoChar(passwordVisible ? (char) 0 : defaultEchoChar);
                        setForeground(Color.DARK_GRAY); isPlaceholderActive = false;
                    }
                }
                @Override public void focusLost(FocusEvent e) {
                    if (getPassword().length == 0) {
                        setEchoChar((char) 0); setForeground(TEXT_DARKPINK);
                        setText(placeholder); isPlaceholderActive = true;
                    }
                }
            });
        }
        public void setPasswordVisible(boolean visible) {
            this.passwordVisible = visible;
            if (!isPlaceholderActive) setEchoChar(visible ? (char) 0 : defaultEchoChar);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor); g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            super.paintComponent(g2); g2.dispose();
        }
    }

    class EyeToggleButton extends JButton {
        private boolean showPassword = false;
        public EyeToggleButton() {
            setContentAreaFilled(false); setBorderPainted(false);
            setFocusPainted(false); setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        public boolean isShowPassword() { return showPassword; }
        public void setShowPassword(boolean v) { this.showPassword = v; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int size = Math.min(w, h) - scale(15);
            int x = (w - size) / 2, y = (h - size) / 2;
            g2.setColor(TEXT_DARKPINK);
            g2.setStroke(new BasicStroke(scale(4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int eyeW = size, eyeH = (int)(size * 0.6);
            int eyeX = x, eyeY = y + (size - eyeH) / 2;
            Path2D ep = new Path2D.Double();
            ep.moveTo(eyeX, eyeY + eyeH / 2.0);
            ep.quadTo(eyeX + eyeW / 2.0, eyeY - scale(3), eyeX + eyeW, eyeY + eyeH / 2.0);
            ep.quadTo(eyeX + eyeW / 2.0, eyeY + eyeH + scale(3), eyeX, eyeY + eyeH / 2.0);
            ep.closePath();
            g2.draw(ep);
            int ps = (int)(eyeH * 0.5);
            g2.fillOval(eyeX + (eyeW - ps) / 2, eyeY + (eyeH - ps) / 2, ps, ps);
            if (!showPassword) {
                g2.setStroke(new BasicStroke(scale(5), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(eyeX + scale(5), eyeY + scale(5), eyeX + eyeW - scale(5), eyeY + eyeH - scale(5));
            }
            g2.dispose();
        }
    }

    class RoundedComboBox extends JComboBox<String> {
        private Color bgColor; private int radius;
        public RoundedComboBox(String[] items, Color bgColor, int radius) {
            super(items); this.bgColor = bgColor; this.radius = radius;
            setOpaque(false);
            setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, scale(35)));
            setForeground(TEXT_DARKPINK);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setUI(new BasicComboBoxUI() {
                @Override protected JButton createArrowButton() {
                    JButton btn = new JButton("see more... ⌄  ");
                    btn.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, scale(28)));
                    btn.setForeground(TEXT_DARKPINK);
                    btn.setContentAreaFilled(false); btn.setBorderPainted(false);
                    btn.setFocusPainted(false); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    return btn;
                }
                @Override public void paintCurrentValueBackground(Graphics g, Rectangle b, boolean f) {}
                @Override protected ComboPopup createPopup() {
                    BasicComboPopup popup = new BasicComboPopup(comboBox) {
                        @Override protected void configurePopup() {
                            super.configurePopup(); setOpaque(true); setBackground(Color.WHITE);
                            setBorder(new javax.swing.border.LineBorder(Color.decode("#8A7A7A"), scale(4), true));
                        }
                    };
                    return popup;
                }
            });
            setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(
                        JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    lbl.setBorder(BorderFactory.createEmptyBorder(scale(18), 0, scale(18), 0));
                    if (value != null && value.toString().equals("+ Add Driver name")) {
                        lbl.setForeground(LINK_COLOR);
                        lbl.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, scale(30)));
                    } else {
                        lbl.setForeground(Color.BLACK);
                        lbl.setFont(new Font("SansSerif", Font.PLAIN, scale(30)));
                    }
                    lbl.setBackground(isSelected ? Color.decode("#F5F5F5") : Color.WHITE);
                    return lbl;
                }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor); g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            super.paintComponent(g2); g2.dispose();
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new TicketingDeviceLogin().setVisible(true));
    }
}

