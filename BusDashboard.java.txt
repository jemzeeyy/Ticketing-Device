package ticketingdevice;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.List;

public class BusDashboard extends JFrame {

    // --- ENLARGED: Increased scale factor from 0.35 to 0.65 ---
    private static final double SCALE = 0.65;

    // Figma Colors & Aesthetics
    private final Color BG_PLATE = Color.decode("#FFCDC7");
    private final Color WHITE_BOX = Color.WHITE;
    private final Color TEXT_DARK = Color.decode("#2B434A"); 
    private final Color TEXT_RED = Color.decode("#FF6969");  
    private final Color FIELD_BG = Color.decode("#FFCCCC");  
    private final Color FIELD_TEXT = Color.decode("#B68787"); 

    // --- STATIC PASSENGER QUEUE ---
    public static List<PassengerStop> upcomingStops = new ArrayList<>();
    public static PassengerStop nextStop = null;
    private static BusDashboard activeInstance = null;

    // Master list of geographic route sequences (to guarantee ascending drop-off sorting)
    // Master list of geographic route sequences (to guarantee ascending drop-off sorting)
    private static final Map<String, Integer> ROUTE_SEQUENCE = new HashMap<>();
    static {
        int seq = 0;
        
        // Build Victory Liner route structure programmatically to generate drop indices
        Map<String, Map<String, List<String>>> data = new LinkedHashMap<>();
        
        // 1. Olongapo City Sequence (First Priority)
        Map<String, List<String>> olongapo = new LinkedHashMap<>();
        olongapo.put("Barangay Barretto", Arrays.asList("Ulticare", "Petron", "164", "Andoks", "Sam's Pizza", "Immaculate Church", "Anbon Hotel", "Tindahan ni Juan", "Cebuana", "MLHUILIER", "El Molina", "Baloy Beach", "Willcon", "Victory Subic Center", "Somil", "Easy hardware", "Elementary School", "White rocks"));
        olongapo.put("Barangay East Bajac-Bajac", Arrays.asList("Market", "Plaza")); 
        olongapo.put("Barangay Gordon Heights", Arrays.asList("Gordon Park"));
        olongapo.put("Barangay Kalaklan", Arrays.asList("Lighthouse"));
        olongapo.put("Barangay New Cabalan", Arrays.asList("Cabalan Hall"));
        data.put("Olongapo City", olongapo);

        // Olongapo Terminal
        data.put("Olongapo, Victory Terminal", new LinkedHashMap<>());

        // 2. Subic Sequence (Second Priority)
        Map<String, List<String>> subic = new LinkedHashMap<>();
        subic.put("Calapacuan", Arrays.asList("Osave Calapacuan", "willtech", "Compac 1", "Jesus is Lord", "Dunkin", "Iglesia ni Cristo", "Palawan", "Big brew", "Subic beach resort", "Subic 2n2 Resort"));
        subic.put("Calapandayan", Arrays.asList("MLHUILIER", "711", "Go shell"));
        subic.put("Ilwas", Arrays.asList("Andoks", "Ataw"));
        subic.put("Manggahan", Arrays.asList("Waltermart", "Manggahan Plaza"));
        subic.put("Manganvaka", Arrays.asList("Kurbada", "Govic Highway", "St. Theodore"));
        subic.put("Aningway Sacatihan", Arrays.asList("Subic Hills", "Fiesta Prime", "Monte sa bato", "Khonghun Compound"));
        subic.put("Katihan", Arrays.asList("Mukbang", "Iglesya"));
        subic.put("Pamatawan", Arrays.asList("Casa Mia 3", "Lumina", "Don Benitos"));
        data.put("Subic", subic);

        // 3. Castillejos Sequence (Third Priority)
        Map<String, List<String>> castillejos = new LinkedHashMap<>();
        castillejos.put("Del Pillar", Arrays.asList("Fiesta", "Plaza ng San Pablo", "Juan Petrol", "Petron", "Jhaps", "Alfamart"));
        castillejos.put("San Roque", Arrays.asList("Jesmag", "Iglesia ni Cristo"));
        castillejos.put("San Nicolas", Arrays.asList("167"));
        castillejos.put("San Juan", Arrays.asList("Agra gas"));
        castillejos.put("San Jose", Arrays.asList("Bus Stop", "Pure gold", "Zameco 2", "San Jose Plaza"));
        castillejos.put("Magsaysay", Arrays.asList("711", "PTT", "K9", "Silog", "Water discrict", "Ria Mae"));
        castillejos.put("Nagbunga", Arrays.asList("Nagbunga Plaza", "Gallardo", "Iglesia ni Cristo"));
        data.put("Castillejos", castillejos);

        // Generate absolute sequential indices
        for (Map.Entry<String, Map<String, List<String>>> cityEntry : data.entrySet()) {
            String city = cityEntry.getKey();
            for (Map.Entry<String, List<String>> bgyEntry : cityEntry.getValue().entrySet()) {
                String bgy = bgyEntry.getKey();
                List<String> landmarks = bgyEntry.getValue();
                
                // Base fallback (if landmark isn't specified, sort at the start of the barangay)
                ROUTE_SEQUENCE.put(city + bgy, seq);
                
                for (String lm : landmarks) {
                    ROUTE_SEQUENCE.put(city + bgy + lm, seq++);
                }
                
                if (landmarks.isEmpty()) {
                    seq++;
                }
            }
        }
    }

    // UI Panel References
    private JPanel listPanel;
    private JLabel nextStopLabel, nextSeatLabel;
    private JPanel nextStopContainer;

    public BusDashboard() {
        setTitle("Victory Liner - Bus Dashboard System");
        setSize(scale(1550), scale(950));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        activeInstance = this;

        JPanel mainPanel = new JPanel(null);
        mainPanel.setBackground(BG_PLATE);

        buildLeftColumn(mainPanel);
        buildRightColumn(mainPanel);

        setContentPane(mainPanel);
        refreshDashboardData();
    }

    // =====================================================================
    // STATIC CONTROLLERS FOR RE-ORDERING AND INGESTION
    // =====================================================================
    // =====================================================================
    // STATIC CONTROLLERS FOR RE-ORDERING AND INGESTION
    // =====================================================================
    public static void addPassenger(String city, String bgy, String landmark, int seatNum) {
        PassengerStop newStop = new PassengerStop(city, bgy, landmark, seatNum);
        
        // 1. If there's already a highlighted Next Stop, temporarily return it to the pool
        if (nextStop != null) {
            upcomingStops.add(nextStop);
            nextStop = null;
        }
        
        // 2. Add the newly purchased ticket stop to the pool
        upcomingStops.add(newStop);
        
        // 3. Sort the entire pool geographically (Olongapo -> Subic -> Castillejos)
        Collections.sort(upcomingStops, Comparator.comparingInt(p -> p.routeIndex));
        
        // 4. Set the highest priority (earliest geographic stop) as the highlighted Next Stop
        if (!upcomingStops.isEmpty()) {
            nextStop = upcomingStops.remove(0);
        }

        // Refresh the visual interface if it is currently open
        if (activeInstance != null) {
            activeInstance.refreshDashboardData();
        }
    }

    public void refreshDashboardData() {
        // Render Next Stop Field
        if (nextStop != null) {
            nextStopLabel.setText(nextStop.getAbbreviatedString());
            nextSeatLabel.setText(String.valueOf(nextStop.seatNum));
            nextStopContainer.setVisible(true);
        } else {
            nextStopContainer.setVisible(false);
        }

        // Render Upcoming Stops List
        listPanel.removeAll();
        for (PassengerStop stop : upcomingStops) {
            listPanel.add(createUpcomingItemRow(stop));
            listPanel.add(Box.createRigidArea(new Dimension(0, scale(15))));
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    // =====================================================================
    // UI BUILDERS
    // =====================================================================
    private void buildLeftColumn(JPanel panel) {
        int leftAlignX = scale(30);
        // Widened elementW from scale(320) to scale(380) to prevent text truncation
        int elementW = scale(380);

        // --- DRIVER SECTION ---
        JLabel drHeader = createLabel("Driver", TEXT_DARK, 55, true);
        drHeader.setBounds(leftAlignX, scale(40), elementW, scale(50));
        panel.add(drHeader);

        ProfileCircle drCircle = new ProfileCircle();
        drCircle.setBounds(leftAlignX + scale(75), scale(100), scale(230), scale(230));
        panel.add(drCircle);

        JLabel drName = createLabel("Juan Dela Cruz", TEXT_DARK, 45, true);
        drName.setBounds(leftAlignX, scale(345), elementW, scale(50));
        panel.add(drName);

        JLabel drId = createLabel("202513847", TEXT_DARK, 35, false);
        drId.setBounds(leftAlignX, scale(390), elementW, scale(40));
        panel.add(drId);

        // --- CONDUCTOR SECTION ---
        JLabel cdHeader = createLabel("Conductor", TEXT_DARK, 55, true);
        cdHeader.setBounds(leftAlignX, scale(480), elementW, scale(50));
        panel.add(cdHeader);

        ProfileCircle cdCircle = new ProfileCircle();
        cdCircle.setBounds(leftAlignX + scale(75), scale(540), scale(230), scale(230));
        panel.add(cdCircle);

        JLabel cdName = createLabel("Juan Dela Cruz", TEXT_DARK, 45, true);
        cdName.setBounds(leftAlignX, scale(785), elementW, scale(50));
        panel.add(cdName);

        JLabel cdId = createLabel("202513847", TEXT_DARK, 35, false);
        cdId.setBounds(leftAlignX, scale(830), elementW, scale(40));
        panel.add(cdId);
    }

    private void buildRightColumn(JPanel panel) {
        int colX = scale(440);
        int colW = scale(1050);

        // --- 1. BUS DASHBOARD SYSTEM PLATE (TOP) ---
        JPanel topBox = new RoundedPanel(WHITE_BOX, scale(50));
        topBox.setBounds(colX, scale(40), colW, scale(280));
        topBox.setLayout(null);

        JLabel sysHeader = createLabel("Bus Dashboard System", TEXT_DARK, 60, true);
        sysHeader.setFont(new Font("Georgia", Font.BOLD | Font.ITALIC, scale(55)));
        sysHeader.setBounds(0, scale(25), colW, scale(70));
        topBox.add(sysHeader);

        JLabel nextStopTitle = createLabel("Next Stop", Color.BLACK, 35, true);
        nextStopTitle.setHorizontalAlignment(SwingConstants.LEFT);
        nextStopTitle.setBounds(scale(100), scale(110), scale(400), scale(40));
        topBox.add(nextStopTitle);

        JLabel seatTitle = createLabel("Sit Number", Color.BLACK, 35, true);
        seatTitle.setHorizontalAlignment(SwingConstants.RIGHT);
        seatTitle.setBounds(colW - scale(400), scale(110), scale(300), scale(40));
        topBox.add(seatTitle);

        // Next Stop Dynamic Pill Container
        nextStopContainer = new RoundedPanel(FIELD_BG, scale(55));
        nextStopContainer.setBounds(scale(50), scale(160), scale(880), scale(85));
        nextStopContainer.setLayout(null);

        nextStopLabel = createLabel("CT, BR, LM", TEXT_RED, 45, true);
        nextStopLabel.setHorizontalAlignment(SwingConstants.LEFT);
        nextStopLabel.setBounds(scale(50), 0, scale(650), scale(85));
        nextStopContainer.add(nextStopLabel);

        nextSeatLabel = createLabel("0", TEXT_RED, 45, true);
        nextSeatLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        nextSeatLabel.setBounds(scale(700), 0, scale(130), scale(85));
        nextStopContainer.add(nextSeatLabel);

        topBox.add(nextStopContainer);

        // Circular Red Delete (X) Button
        CircleDeleteButton deleteBtn = new CircleDeleteButton();
        deleteBtn.setBounds(colX + scale(950), scale(170), scale(65), scale(65));
        deleteBtn.addActionListener(e -> {
            // Delete current Next Stop and shift up the first upcoming stop
            if (!upcomingStops.isEmpty()) {
                nextStop = upcomingStops.remove(0);
            } else {
                nextStop = null;
            }
            refreshDashboardData();
        });
        panel.add(deleteBtn);
        panel.add(topBox);

        // --- 2. DECORATIVE GRADIENT SEPARATOR LINE ---
        JPanel gradientLine = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float[] fractions = {0.0f, 0.5f, 1.0f};
                Color[] colors = {Color.decode("#F01011"), Color.decode("#ED590F"), Color.decode("#EAEB0B")};
                LinearGradientPaint paint = new LinearGradientPaint(0, 0, getWidth(), 0, fractions, colors);
                g2.setPaint(paint);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), scale(15), scale(15));
                g2.dispose();
            }
        };
        gradientLine.setBounds(colX, scale(350), colW, scale(17));
        gradientLine.setOpaque(false);
        panel.add(gradientLine);

        // --- 3. UPCOMING STOPS PLATE (BOTTOM) ---
        JPanel bottomBox = new RoundedPanel(WHITE_BOX, scale(50));
        bottomBox.setBounds(colX, scale(400), colW, scale(480));
        bottomBox.setLayout(null);

        JLabel listHeader = createLabel("List of the Upcoming Stop:", Color.BLACK, 35, true);
        listHeader.setHorizontalAlignment(SwingConstants.LEFT);
        listHeader.setBounds(scale(100), scale(35), scale(500), scale(45));
        bottomBox.add(listHeader);

        JLabel listSeatHeader = createLabel("Sit Number", Color.BLACK, 35, true);
        listSeatHeader.setHorizontalAlignment(SwingConstants.RIGHT);
        listSeatHeader.setBounds(colW - scale(400), scale(35), scale(300), scale(45));
        bottomBox.add(listSeatHeader);

        // Container Area for the Scrollable Stops List
        JPanel listOuterContainer = new RoundedPanel(FIELD_BG, scale(50));
        listOuterContainer.setBounds(scale(50), scale(90), scale(950), scale(350));
        listOuterContainer.setLayout(new BorderLayout());

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.setBorder(new EmptyBorder(scale(25), scale(45), scale(25), scale(45)));

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);
        customizeScrollBar(scrollPane);

        listOuterContainer.add(scrollPane, BorderLayout.CENTER);
        bottomBox.add(listOuterContainer);
        panel.add(bottomBox);
    }

    private JPanel createUpcomingItemRow(PassengerStop stop) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(scale(860), scale(60)));
        row.setPreferredSize(new Dimension(scale(860), scale(60)));

        // Bullet point • and Drop details using targeted HTML style matching your mockup
        JLabel dropLabel = new JLabel("<html><span style='color: #FF6969; font-size: 16px;'>&bull;</span>&nbsp;&nbsp;<span style='color: #FF6969; font-weight: bold;'>" + stop.getAbbreviatedString() + "</span></html>");
        dropLabel.setFont(new Font("SansSerif", Font.BOLD, scale(45)));
        row.add(dropLabel, BorderLayout.WEST);

        JLabel seatLabel = new JLabel(String.valueOf(stop.seatNum));
        seatLabel.setFont(new Font("SansSerif", Font.BOLD, scale(45)));
        seatLabel.setForeground(TEXT_RED);
        seatLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(seatLabel, BorderLayout.EAST);

        return row;
    }

    // =====================================================================
    // PASSENGER MODEL CLASS WITH AUTOMATIC GEOGRAPHIC PROGRESSION
    // =====================================================================
    public static class PassengerStop {
        public String city;
        public String bgy;
        public String landmark;
        public int seatNum;
        public int routeIndex;

        public PassengerStop(String city, String bgy, String landmark, int seatNum) {
            this.city = city;
            this.bgy = bgy;
            this.landmark = landmark;
            this.seatNum = seatNum;
            
            // Search for precise City + Barangay + Landmark index first, fallback to City + Barangay
            this.routeIndex = ROUTE_SEQUENCE.getOrDefault(
                city + bgy + landmark, 
                ROUTE_SEQUENCE.getOrDefault(city + bgy, 9999)
            );
        }

        // Handles clean dynamic abbreviations matching your mockup requirements
        public String getAbbreviatedString() {
            String cityAbbr = "";
            if (city.toLowerCase().contains("olongapo")) cityAbbr = "OL";
            else if (city.toLowerCase().contains("subic")) cityAbbr = "SB";
            else if (city.toLowerCase().contains("castillejos")) cityAbbr = "CS";
            else cityAbbr = city.substring(0, Math.min(2, city.length())).toUpperCase();

            String rawBgy = bgy;
            if (rawBgy.toLowerCase().startsWith("barangay ")) {
                rawBgy = rawBgy.substring(9);
            }

            String bgyAbbr = getBgyAbbr(rawBgy);
            String lmStr = (landmark == null || landmark.isEmpty()) ? "" : landmark;
            
            return cityAbbr + ", " + bgyAbbr + ", " + lmStr;
        }

        private String getBgyAbbr(String bName) {
            // Check direct mapping rule sets first
            if (bName.equalsIgnoreCase("Barretto")) return "BT";
            if (bName.equalsIgnoreCase("Calapacuan")) return "CP";
            if (bName.equalsIgnoreCase("Calapandayan")) return "CPD";
            if (bName.equalsIgnoreCase("Ilwas")) return "IW";
            if (bName.equalsIgnoreCase("Manggahan")) return "MG";
            if (bName.equalsIgnoreCase("Manganvaka")) return "MV";
            if (bName.equalsIgnoreCase("Katihan")) return "KT";
            if (bName.equalsIgnoreCase("Pamatawan")) return "PM";
            if (bName.equalsIgnoreCase("Magsaysay")) return "MS";
            if (bName.equalsIgnoreCase("Nagbunga")) return "NB";
            if (bName.equalsIgnoreCase("San Pablo")) return "SP";

            // Splitting Multiwords (e.g., "East Bajac-Bajac" -> "EBB")
            String[] words = bName.split("[\\s\\-]+");
            if (words.length > 1) {
                StringBuilder abbr = new StringBuilder();
                for (String w : words) {
                    if (!w.isEmpty()) abbr.append(Character.toUpperCase(w.charAt(0)));
                }
                return abbr.toString();
            }
            return bName.substring(0, Math.min(2, bName.length())).toUpperCase();
        }
    }

    // =====================================================================
    // HELPER RENDERING COMPONENTS
    // =====================================================================
    private int scale(double value) { return (int) Math.round(value * SCALE); }

    private JLabel createLabel(String text, Color color, int size, boolean bold) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, scale(size)));
        l.setForeground(color);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    class ProfileCircle extends JPanel {
        public ProfileCircle() { setOpaque(false); }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillOval(0, 0, getWidth(), getHeight());
            g2.setStroke(new BasicStroke(scale(11)));
            g2.setColor(Color.decode("#A62A22")); // Rounded Crimson border
            g2.drawOval(5, 5, getWidth() - 11, getHeight() - 11);
            super.paintComponent(g);
        }
    }

    class RoundedPanel extends JPanel {
        private Color bgColor; private int radius;
        public RoundedPanel(Color bgColor, int radius) { this.bgColor = bgColor; this.radius = radius; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor); g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius); g2.dispose();
        }
    }

    class CircleDeleteButton extends JButton {
        public CircleDeleteButton() {
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw background red circle
            g2.setColor(Color.decode("#FF4D4D"));
            g2.fillOval(0, 0, getWidth(), getHeight());

            // Draw white 'X'
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(scale(13), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int offset = scale(45);
            g2.drawLine(offset, offset, getWidth() - offset, getHeight() - offset);
            g2.drawLine(getWidth() - offset, offset, offset, getHeight() - offset);

            g2.dispose();
        }
    }

    private void customizeScrollBar(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override 
            protected void configureScrollBarColors() { 
                this.trackColor = new Color(0, 0, 0, 0); 
            }
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Color Thumb with Gradient styling to match UI layout
                float[] fractions = {0.0f, 0.5f, 1.0f};
                Color[] colors = {Color.decode("#F01011"), Color.decode("#ED590F"), Color.decode("#EAEB0B")};
                LinearGradientPaint paint = new LinearGradientPaint(0, thumbBounds.y, 0, thumbBounds.y + thumbBounds.height, fractions, colors);
                g2.setPaint(paint);
                g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, scale(12), scale(12));
                g2.dispose();
            }
            @Override protected JButton createDecreaseButton(int o) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return createZeroButton(); }
            private JButton createZeroButton() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        });
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(scale(18), 0));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BusDashboard().setVisible(true));
    }
}

