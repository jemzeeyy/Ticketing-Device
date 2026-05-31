package ticketingdevice;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class TicketingFromDestination extends JFrame {

    private static final double SCALE = 0.35;

    // Figma Colors & Opacities
    private final Color BG_PLATE = Color.decode("#FFCDC7");
    private final Color WHITE_PLATE = new Color(255, 255, 255, 143); // FFFFFF @ 56%
    private final Color TEXT_DESTINATION = Color.decode("#FF6969");
    private final Color TEXT_FROM = Color.decode("#F00000");
    private final Color FIELD_BG = new Color(255, 136, 136, 143); // FF8888 @ 56%
    private final Color BTN_INACTIVE = new Color(255, 136, 136, 224); // FF8888 @ 88%
    private final Color BTN_ACTIVE = Color.decode("#FF6969"); 
    private final Color ITEM_BG = Color.decode("#FF8888"); 

    // Data Maps
    private Map<String, Map<String, List<String>>> locationData = new LinkedHashMap<>();

    // UI Components
    private JPanel mainContent;
    private JLabel cityLabel, bgyLabel, lmLabel;
    private FieldButton cityField, bgyField, lmField;
    private DropdownPanel cityDropdown, bgyDropdown, lmDropdown;
    private JLabel thankYouLabel;
    private RoundedButton confirmBtn;

    // State
    private String selectedCity = "";
    private String selectedBgy = "";
    private boolean isFormComplete = false; // Tracks if the form is fully filled out
    
    // --- Dynamic White Plate Height ---
    private int whitePlateHeight = 0; 

    public TicketingFromDestination() {
        setTitle("Victory Liner - Destination");
        setSize(scale(1086), scale(2511));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initData();

        // Scrollable Main Container with DYNAMIC background drawing
        mainContent = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw White Plate using the DYNAMIC height variable instead of a hardcoded number
                g2.setColor(WHITE_PLATE);
                g2.fillRoundRect(scale(14), 0, scale(1059), whitePlateHeight, scale(49), scale(49));
                g2.dispose();
            }
        };
        mainContent.setBackground(BG_PLATE);

        buildUI();

        JScrollPane mainScroll = new JScrollPane(mainContent);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(20);
        customizeScrollBar(mainScroll);
        setContentPane(mainScroll);

        recalculateLayout(); 
    }

    private void buildUI() {
        // Headers
        JLabel destLabel = createLabel("DESTINATION", TEXT_DESTINATION, 120);
        destLabel.setBounds(0, scale(142), scale(1086), scale(140));
        mainContent.add(destLabel);

        // Red Line
        JPanel redLine = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.decode("#FF8888"));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), scale(19), scale(19));
                g2.dispose();
            }
        };
        redLine.setOpaque(false);
        redLine.setBounds(scale(63), scale(298), scale(960), scale(19));
        mainContent.add(redLine);

        JLabel fromLabel = createLabel("From", TEXT_FROM, 48);
        fromLabel.setBounds(0, scale(318), scale(1086), scale(60));
        mainContent.add(fromLabel);

        // --- CITY SECTION ---
        cityLabel = createLeftLabel("City:");
        cityField = new FieldButton("Select your City");
        cityDropdown = new DropdownPanel();
        
        cityField.addActionListener(e -> {
            closeAllDropdowns();
            populateDropdown(cityDropdown, new ArrayList<>(locationData.keySet()), this::selectCity);
            cityDropdown.setVisible(true);
            recalculateLayout();
        });
        
        mainContent.add(cityLabel);
        mainContent.add(cityField);
        mainContent.add(cityDropdown);

        // --- BARANGAY SECTION ---
        bgyLabel = createLeftLabel("Barangay:");
        bgyField = new FieldButton("Select your Village");
        bgyDropdown = new DropdownPanel();

        bgyField.addActionListener(e -> {
            if (selectedCity.isEmpty() || locationData.get(selectedCity).isEmpty()) return;
            closeAllDropdowns();
            populateDropdown(bgyDropdown, new ArrayList<>(locationData.get(selectedCity).keySet()), this::selectBgy);
            bgyDropdown.setVisible(true);
            recalculateLayout();
        });

        mainContent.add(bgyLabel);
        mainContent.add(bgyField);
        mainContent.add(bgyDropdown);

        // --- LANDMARK SECTION ---
        lmLabel = createLeftLabel("LandMark:");
        lmField = new FieldButton("Select your Landmarks");
        lmDropdown = new DropdownPanel();

        lmField.addActionListener(e -> {
            if (selectedBgy.isEmpty() || locationData.get(selectedCity).get(selectedBgy).isEmpty()) return;
            closeAllDropdowns();
            populateDropdown(lmDropdown, locationData.get(selectedCity).get(selectedBgy), this::selectLandmark);
            lmDropdown.setVisible(true);
            recalculateLayout();
        });

        mainContent.add(lmLabel);
        mainContent.add(lmField);
        mainContent.add(lmDropdown);

        // --- BOTTOM SECTION ---
        thankYouLabel = createLabel("<html><div style='text-align: center;'>Thank you for completing all the<br>required destination. Your input are<br>now successfully recorded :)</div></html>", TEXT_DESTINATION, 40);
        thankYouLabel.setVisible(false);
        mainContent.add(thankYouLabel);

        confirmBtn = new RoundedButton("CONFIRM", scale(49));
        confirmBtn.setBackground(BTN_INACTIVE);
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, scale(96)));
        mainContent.add(confirmBtn);
        
        confirmBtn.addActionListener(e -> {
            if (selectedCity.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select your destination details.");
                return;
            }

            String landmark = lmField.getText();
            if (landmark.equals("Select your Landmarks")) {
                landmark = "";
            }

            // Format selection
            StringBuilder fullLocation = new StringBuilder(selectedCity);
            if (selectedBgy != null && !selectedBgy.isEmpty()) {
                fullLocation.append(", ").append(selectedBgy);
            }
            if (!landmark.isEmpty()) {
                fullLocation.append(" (").append(landmark).append(")");
            }

            // Save selection to the static variable
            TicketingDevice.selectedFrom = fullLocation.toString();

            // Open TicketingDevice (which automatically reads the static variables)
            new TicketingDevice("", "", "").setVisible(true);
            dispose();
        });
        
        mainContent.add(confirmBtn);
    }

    // --- SELECTION LOGIC ---

    private void selectCity(String city) {
        selectedCity = city;
        cityField.setSelection(city);
        closeAllDropdowns();
        
        selectedBgy = "";
        bgyField.reset("Select your Village");
        lmField.reset("Select your Landmarks");

        Map<String, List<String>> bgys = locationData.get(city);
        if (bgys.isEmpty()) {
            bgyLabel.setVisible(false); bgyField.setVisible(false);
            lmLabel.setVisible(false); lmField.setVisible(false);
            completeForm();
        } else {
            bgyLabel.setVisible(true); bgyField.setVisible(true);
            lmLabel.setVisible(true); lmField.setVisible(true);
            resetForm();
            populateDropdown(bgyDropdown, new ArrayList<>(bgys.keySet()), this::selectBgy);
            bgyDropdown.setVisible(true);
        }
        recalculateLayout();
    }

    private void selectBgy(String bgy) {
        selectedBgy = bgy;
        bgyField.setSelection(bgy);
        closeAllDropdowns();
        lmField.reset("Select your Landmarks");
        
        populateDropdown(lmDropdown, locationData.get(selectedCity).get(bgy), this::selectLandmark);
        lmDropdown.setVisible(true);
        recalculateLayout();
    }

    private void selectLandmark(String lm) {
        lmField.setSelection(lm);
        closeAllDropdowns();
        completeForm();
        recalculateLayout();
    }

    private void completeForm() {
        isFormComplete = true;
        thankYouLabel.setVisible(true);
        confirmBtn.setBackground(BTN_ACTIVE);
        confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void resetForm() {
        isFormComplete = false;
        thankYouLabel.setVisible(false);
        confirmBtn.setBackground(BTN_INACTIVE);
        confirmBtn.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    private void closeAllDropdowns() {
        cityDropdown.setVisible(false);
        bgyDropdown.setVisible(false);
        lmDropdown.setVisible(false);
    }

    // --- DYNAMIC LAYOUT ENGINE ---
    private void recalculateLayout() {
        int currentY = scale(437); 
        int xAlign = scale(63);    
        int fieldW = scale(960);
        int fieldH = scale(135);
        int dropH = scale(665);

        // 1. City
        cityLabel.setBounds(scale(101), currentY, scale(400), scale(60));
        currentY += scale(78);
        cityField.setBounds(xAlign, currentY, fieldW, fieldH);
        currentY += fieldH + scale(20);
        if (cityDropdown.isVisible()) {
            cityDropdown.setBounds(xAlign, currentY, fieldW, dropH);
            currentY += dropH + scale(30);
        }

        // 2. Barangay 
        if (bgyLabel.isVisible()) {
            currentY += scale(30);
            bgyLabel.setBounds(scale(101), currentY, scale(400), scale(60));
            currentY += scale(78);
            bgyField.setBounds(xAlign, currentY, fieldW, fieldH);
            currentY += fieldH + scale(20);
            if (bgyDropdown.isVisible()) {
                bgyDropdown.setBounds(xAlign, currentY, fieldW, dropH);
                currentY += dropH + scale(30);
            }
        }

        // 3. Landmark
        if (lmLabel.isVisible()) {
            currentY += scale(30);
            lmLabel.setBounds(scale(101), currentY, scale(400), scale(60));
            currentY += scale(78);
            lmField.setBounds(xAlign, currentY, fieldW, fieldH);
            currentY += fieldH + scale(20);
            if (lmDropdown.isVisible()) {
                lmDropdown.setBounds(xAlign, currentY, fieldW, dropH);
                currentY += dropH + scale(30);
            }
        }

        // 4. Thank You & Confirm
        currentY += scale(80);
        if (thankYouLabel.isVisible()) {
            thankYouLabel.setBounds(0, currentY, scale(1086), scale(174));
            currentY += scale(174) + scale(50);
        }
        
        // Position Confirm Button
        confirmBtn.setBounds(scale(70), currentY, scale(945), scale(175));
        
        // --- CALCULATE DYNAMIC WHITE PLATE HEIGHT ---
        whitePlateHeight = currentY + scale(175) + scale(70);
        
        // Tell the scroll pane how big the whole document is
        mainContent.setPreferredSize(new Dimension(scale(1086), whitePlateHeight + scale(50)));
        
        mainContent.revalidate();
        mainContent.repaint(); 
    }

    // --- DATA POPULATION ---
    private void initData() {
        Map<String, List<String>> olongapo = new LinkedHashMap<>();
        olongapo.put("Barangay Barretto", Arrays.asList("Ulticare", "Petron", "164", "Andoks", "Sam's Pizza", "Immaculate Church", "Anbon Hotel", "Tindahan ni Juan", "Cebuana", "MLHUILIER", "El Molina", "Baloy Beach", "Willcon", "Victory Subic Center", "Somil", "Easy hardware", "Elementary School", "White rocks"));
        olongapo.put("Barangay East Bajac-Bajac", Arrays.asList("Market", "Plaza")); 
        olongapo.put("Barangay Gordon Heights", Arrays.asList("Gordon Park"));
        olongapo.put("Barangay Kalaklan", Arrays.asList("Lighthouse"));
        olongapo.put("Barangay New Cabalan", Arrays.asList("Cabalan Hall"));
        locationData.put("Olongapo City", olongapo);

        locationData.put("Olongapo, Victory Terminal", new LinkedHashMap<>());

        Map<String, List<String>> subic = new LinkedHashMap<>();
        subic.put("Calapacuan", Arrays.asList("Osave Calapacuan", "willtech", "Compac 1", "Jesus is Lord", "Dunkin", "Iglesia ni Cristo", "Palawan", "Big brew", "Subic beach resort", "Subic 2n2 Resort"));
        subic.put("Calapandayan", Arrays.asList("MLHUILIER", "711", "Go shell"));
        subic.put("Ilwas", Arrays.asList("Andoks", "Ataw"));
        subic.put("Manggahan", Arrays.asList("Waltermart", "Manggahan Plaza"));
        subic.put("Manganvaka", Arrays.asList("Kurbada", "Govic Highway", "St. Theodore"));
        subic.put("Aningway Sacatihan", Arrays.asList("Subic Hills", "Fiesta Prime", "Monte sa bato", "Khonghun Compound"));
        subic.put("Katihan", Arrays.asList("Mukbang", "Iglesya"));
        subic.put("Pamatawan", Arrays.asList("Casa Mia 3", "Lumina", "Don Benitos"));
        locationData.put("Subic", subic);

        Map<String, List<String>> castillejos = new LinkedHashMap<>();
        castillejos.put("Del Pillar", Arrays.asList("Fiesta", "Plaza ng San Pablo", "Juan Petrol", "Petron", "Jhaps", "Alfamart"));
        castillejos.put("San Roque", Arrays.asList("Jesmag", "Iglesia ni Cristo"));
        castillejos.put("San Nicolas", Arrays.asList("167"));
        castillejos.put("San Juan", Arrays.asList("Agra gas"));
        castillejos.put("San Jose", Arrays.asList("Bus Stop", "Pure gold", "Zameco 2", "San Jose Plaza"));
        castillejos.put("Magsaysay", Arrays.asList("711", "PTT", "K9", "Silog", "Water discrict", "Ria Mae"));
        castillejos.put("Nagbunga", Arrays.asList("Nagbunga Plaza", "Gallardo", "Iglesia ni Cristo"));
        locationData.put("Castillejos", castillejos);

        locationData.put("Malolos, Bulacan", new LinkedHashMap<>());
        locationData.put("San Fernando, Bulacan", new LinkedHashMap<>());
        locationData.put("Caloocan", new LinkedHashMap<>());
        locationData.put("Cubao", new LinkedHashMap<>());
    }

    private void populateDropdown(DropdownPanel drop, List<String> items, java.util.function.Consumer<String> onSelect) {
        drop.clearItems();
        for (String item : items) {
            JButton btn = new RoundedButton(item, scale(49));
            btn.setBackground(ITEM_BG);
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, scale(48)));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setBorder(new EmptyBorder(0, scale(40), 0, 0));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setMaximumSize(new Dimension(scale(931), scale(68)));
            btn.setPreferredSize(new Dimension(scale(931), scale(68)));
            
            btn.addActionListener(e -> onSelect.accept(item));
            drop.addItem(btn);
        }
    }

    // --- HELPER UI COMPONENTS ---

    private int scale(double value) { return (int) Math.round(value * SCALE); }

    private JLabel createLabel(String text, Color color, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, scale(size)));
        l.setForeground(color);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    private JLabel createLeftLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, scale(48)));
        l.setForeground(Color.BLACK);
        return l;
    }

    class FieldButton extends JButton {
        public FieldButton(String placeholder) {
            super(placeholder);
            setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, scale(48)));
            setForeground(new Color(255, 102, 102, 150)); 
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(0, scale(40), 0, 0));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        
        public void setSelection(String text) {
            setText(text);
            setForeground(Color.WHITE); 
        }
        
        public void reset(String placeholder) {
            setText(placeholder);
            setForeground(new Color(255, 102, 102, 150));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(FIELD_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), scale(49), scale(49));
            super.paintComponent(g);
            g2.dispose();
        }
    }

    class DropdownPanel extends JPanel {
        private JPanel listPanel;
        
        public DropdownPanel() {
            setLayout(new BorderLayout());
            setOpaque(false);
            setVisible(false);

            listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            listPanel.setOpaque(false);
            listPanel.setBorder(new EmptyBorder(scale(30), scale(15), scale(30), scale(15)));

            JScrollPane scroll = new JScrollPane(listPanel);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.setBorder(null);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            customizeScrollBar(scroll);

            add(scroll, BorderLayout.CENTER);
        }

        public void clearItems() {
            listPanel.removeAll();
        }

        public void addItem(JButton btn) {
            listPanel.add(btn);
            listPanel.add(Box.createRigidArea(new Dimension(0, scale(20)))); 
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(FIELD_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), scale(49), scale(49));
            g2.dispose();
        }
    }

    class RoundedButton extends JButton {
        private int radius;
        public RoundedButton(String text, int radius) {
            super(text); this.radius = radius;
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            super.paintComponent(g);
            g2.dispose();
        }
    }

    private void customizeScrollBar(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { this.thumbColor = new Color(255, 255, 255, 100); this.trackColor = new Color(0,0,0,0); }
            @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        });
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(scale(10), 0));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TicketingFromDestination().setVisible(true));
    }
}

