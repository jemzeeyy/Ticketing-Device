package ticketingdevice;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import javax.swing.table.DefaultTableModel;


public class TicketingDevice extends JFrame {

    private static final double SCALE = 0.35; 

    // --- EXACT COLORS FROM THE IMAGE ---
    private final Color BG_PLATE = Color.decode("#FFCDC7");
    private final Color WHITE_BOX = Color.WHITE;
    private final Color TEXT_DARK = Color.decode("#2B434A"); 
    private final Color TEXT_RED = Color.decode("#FF6969");  
    private final Color TEXT_SUB = Color.decode("#F8716D");  
    private final Color FIELD_BG = Color.decode("#FFCCCC");  
    private final Color FIELD_TEXT = Color.decode("#B68787"); 
    
    // Status Plate Colors
    private final Color STATUS_OUTER = Color.decode("#2B434A");
    private final Color STATUS_INNER = Color.decode("#6B909A"); 

    // --- STATE VARIABLES ---
    private List<Integer> selectedSeats = new ArrayList<>(); // Keeps track of current transaction seats (non-static is correct here)
    
    // --- CHANGED TO STATIC TO PRESERVE ACROSS SCREENS ---
    public static List<Integer> confirmedSeats = new ArrayList<>(); 
    public static String userName = "Driver";
    public static ImageIcon profileIcon = null; // Saves your uploaded circular profile picture

    // --- UI COMPONENTS ---
    private JPanel mainContent;
    private JLabel profilePicLabel, welcomeLabel;
    private RoundedButton btnFrom, btnTo, confirmBtn;
    private JLabel summaryFare, summarySeat;
    private JLabel countPaid, countUnpaid, countAvail;
    private List<SeatButton> seatButtons = new ArrayList<>();
    private List<SquareRadio> discountRadios = new ArrayList<>();
    private boolean changeSeatMode = false;
    private Integer seatToChange = null;
    private RoundedButton changeSeatBtn;
    
    // Steppers
    private Stepper senStepper, stuStepper, pwdStepper;

    // --- STATIC STATE TRACKERS ---
    public static String selectedFrom = "Starting Point";
    public static String selectedTo = "Arrival Point";
    public TicketingDevice(String city, String bgy, String landmark) {
    setTitle("Victory Liner - Ticketing Device");
    setSize(scale(1186) + 40, 900); 
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    mainContent = new JPanel(null);
    mainContent.setBackground(BG_PLATE);
    mainContent.setPreferredSize(new Dimension(scale(1186), scale(2900))); 

    buildHeader();
    buildDestinationSection();
    buildMiddleSection(); 
    buildBottomSection(); 

    JScrollPane scrollPane = new JScrollPane(mainContent);
    scrollPane.setBorder(null);
    scrollPane.getVerticalScrollBar().setUnitIncrement(20);
    customizeScrollBar(scrollPane);
    setContentPane(scrollPane);

    // --- UPDATE: Apply Saved States ---
    if (!selectedFrom.equals("Starting Point")) {
        setStartingPoint(selectedFrom);
    }
    if (!selectedTo.equals("Arrival Point")) {
        setArrivalPoint(selectedTo);
    }

    updateSummary(); 
}

    // ==========================================
    // 1. HEADER & PROFILE
    // ==========================================
    private void buildHeader() {
        profilePicLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setStroke(new BasicStroke(scale(3)));
                g2.setColor(TEXT_DARK);
                g2.drawOval(1, 1, getWidth()-3, getHeight()-3);
                super.paintComponent(g);
            }
        };
        int picSize = scale(217);
        profilePicLabel.setBounds(scale(1186/2 - 217/2), scale(60), picSize, picSize);
        profilePicLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // --- ADDED: Load existing profile picture if one was already set ---
        if (profileIcon != null) {
            profilePicLabel.setIcon(profileIcon);
        }
        
        profilePicLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "jpeg"));
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    userName = file.getName().replaceFirst("[.][^.]+$", ""); 
                    setProfilePicture(file);
                    welcomeLabel.setText("WELCOME " + userName.toUpperCase() + "!");
                }
            }
        });
        mainContent.add(profilePicLabel);

        RoundedButton logoutBtn = new RoundedButton("Log out", scale(35), TEXT_DARK, Color.WHITE);
        logoutBtn.setBounds(scale(909), scale(40), scale(170), scale(82));
        logoutBtn.setFont(new Font("SansSerif", Font.BOLD, scale(32)));
        logoutBtn.addActionListener(e -> {
            try {
                // Clear active static states on logout
                confirmedSeats.clear();
                userName = "Driver";
                profileIcon = null;
                selectedFrom = "Starting Point";
                selectedTo = "Arrival Point";
                
                new TicketingDeviceLogin().setVisible(true); 
            } catch(Exception ex){}
            dispose();
        });
        mainContent.add(logoutBtn);

        // --- UPDATED: Load the saved static driver name instead of defaulting to "WELCOME!" ---
        String welcomeText = userName.equalsIgnoreCase("Driver") ? "WELCOME!" : "WELCOME " + userName.toUpperCase() + "!";
        welcomeLabel = createLabel(welcomeText, TEXT_RED, 100, true);
        welcomeLabel.setFont(new Font("Serif", Font.BOLD, scale(100))); 
        welcomeLabel.setBounds(0, scale(290), scale(1186), scale(110));
        mainContent.add(welcomeLabel);

        JPanel line = new RoundedPanel(Color.decode("#FF8888"), scale(17));
        line.setBounds(scale(133), scale(410), scale(919), scale(17));
        mainContent.add(line);

        JLabel sub = createLabel("Bus Fare System", TEXT_SUB, 42, true);
        sub.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, scale(42))); 
        sub.setBounds(0, scale(440), scale(1186), scale(50));
        mainContent.add(sub);
    }

    // ==========================================
    // 2. DESTINATION (FROM / TO)
    // ==========================================
    private void buildDestinationSection() {
        JPanel destBox = new RoundedPanel(WHITE_BOX, scale(50));
        destBox.setBounds(scale(87), scale(530), scale(1012), scale(330)); 
        destBox.setLayout(null);

        JLabel fromLbl = createLabel("From:", TEXT_DARK, 36, true);
        fromLbl.setHorizontalAlignment(SwingConstants.LEFT);
        fromLbl.setBounds(scale(66), scale(32), scale(200), scale(40));
        destBox.add(fromLbl);

        btnFrom = new RoundedButton("Starting Point", scale(50), FIELD_BG, FIELD_TEXT);
        btnFrom.setBounds(scale(46), scale(85), scale(919), scale(77));
        btnFrom.setFont(new Font("Serif", Font.BOLD, scale(32)));
        btnFrom.addActionListener(e -> {
            try { 
                new TicketingFromDestination().setVisible(true); 
                dispose(); 
            } catch(Exception ex){ JOptionPane.showMessageDialog(this, "Opens Starting Point Page"); }
        });
        destBox.add(btnFrom);

        JLabel toLbl = createLabel("To:", TEXT_DARK, 36, true);
        toLbl.setHorizontalAlignment(SwingConstants.LEFT);
        toLbl.setBounds(scale(66), scale(175), scale(200), scale(40));
        destBox.add(toLbl);

        btnTo = new RoundedButton("Arrival Point", scale(50), FIELD_BG, FIELD_TEXT);
        btnTo.setBounds(scale(46), scale(225), scale(919), scale(77));
        btnTo.setFont(new Font("Serif", Font.BOLD, scale(32)));
        btnTo.addActionListener(e -> {
            try { 
                new TicketingToDestination().setVisible(true); 
                dispose(); 
            } catch(Exception ex){ JOptionPane.showMessageDialog(this, "Opens Arrival Point Page"); }
        });
        destBox.add(btnTo);

        mainContent.add(destBox);
    }

    public void setStartingPoint(String location) {
        btnFrom.setText(location);
        btnFrom.setForeground(Color.WHITE);
        btnFrom.setBackground(Color.decode("#FF8888"));
    }
    public void setArrivalPoint(String location) {
        btnTo.setText(location);
        btnTo.setForeground(Color.WHITE);
        btnTo.setBackground(Color.decode("#FF8888"));
        updateSummary();
    }

    // ==========================================
  
    private void buildMiddleSection() {
        JPanel midBox = new RoundedPanel(WHITE_BOX, scale(50));
        midBox.setBounds(scale(87), scale(900), scale(1012), scale(840));
        midBox.setLayout(null);

        // --- LEFT SIDE: STATUS PLATE ---
        JPanel statOuter = new RoundedPanel(STATUS_OUTER, scale(40));
        statOuter.setBounds(scale(40), scale(40), scale(420), scale(760));
        statOuter.setLayout(null);
        
        JPanel statInner = new RoundedPanel(STATUS_INNER, scale(30));
        statInner.setBounds(scale(20), scale(20), scale(380), scale(720));
        statInner.setLayout(null);
        
        statInner.add(createLabel("Paid", Color.WHITE, 40, true)).setBounds(0, scale(70), scale(380), scale(40));
        countPaid = createLabel("0", Color.decode("#E2C619"), 80, true); 
        countPaid.setBounds(0, scale(120), scale(380), scale(80));
        statInner.add(countPaid);

        statInner.add(createLabel("Unpaid", Color.WHITE, 40, true)).setBounds(0, scale(300), scale(380), scale(40));
        countUnpaid = createLabel("0", TEXT_RED, 80, true);
        countUnpaid.setBounds(0, scale(350), scale(380), scale(80));
        statInner.add(countUnpaid);

        statInner.add(createLabel("Available", Color.WHITE, 40, true)).setBounds(0, scale(530), scale(380), scale(40));
        countAvail = createLabel("33", Color.decode("#87CEEB"), 80, true); 
        countAvail.setBounds(0, scale(580), scale(380), scale(80));
        statInner.add(countAvail);
        
        statOuter.add(statInner);
        midBox.add(statOuter);

        
        
        // --- RIGHT SIDE: SEATS ---
        JPanel seatPanel = new RoundedPanel(STATUS_OUTER, scale(40));
        seatPanel.setBounds(scale(480), scale(70), scale(490), scale(760));
        seatPanel.setLayout(null);

        int seatNum = 1;
        int yOffset = scale(35);
        for (int row = 0; row < 8; row++) {
            if (row < 7) {
                addSeat(seatPanel, seatNum++, scale(20), yOffset);
                addSeat(seatPanel, seatNum++, scale(100), yOffset);
                addSeat(seatPanel, seatNum++, scale(285), yOffset);
                addSeat(seatPanel, seatNum++, scale(365), yOffset);
            } else {
                addSeat(seatPanel, seatNum++, scale(20), yOffset);
                addSeat(seatPanel, seatNum++, scale(106), yOffset);
                addSeat(seatPanel, seatNum++, scale(192), yOffset);
                addSeat(seatPanel, seatNum++, scale(278), yOffset);
                addSeat(seatPanel, seatNum++, scale(365), yOffset);
            }
            yOffset += scale(88); 
        }
        changeSeatBtn = new RoundedButton("Change Seat", scale(20), Color.WHITE, TEXT_DARK);
        changeSeatBtn.setBounds(scale(480), scale(5), scale(490), scale(55));
        changeSeatBtn.setFont(new Font("SansSerif", Font.BOLD, scale(28)));
        changeSeatBtn.addActionListener(e -> {
        changeSeatMode = !changeSeatMode;
        seatToChange = null;
        if (changeSeatMode) {
        changeSeatBtn.setBackground(Color.decode("#FF8888"));
        changeSeatBtn.setForeground(Color.WHITE);
        JOptionPane.showMessageDialog(this,
            "CHANGE SEAT MODE\n\nStep 1: Tap the yellow (occupied) seat you want to move.\nStep 2: Tap the blue (empty) seat to move them there.");
         } else {
        changeSeatBtn.setBackground(Color.WHITE);
        changeSeatBtn.setForeground(TEXT_DARK);
        JOptionPane.showMessageDialog(this, "Change Seat mode cancelled.");
    }
        });
        midBox.add(changeSeatBtn);
        midBox.add(seatPanel);
        mainContent.add(midBox);
    }

    private void addSeat(JPanel panel, int id, int x, int y) {
        SeatButton btn = new SeatButton(id);
        btn.setBounds(x, y, scale(80), scale(75)); 
        btn.addActionListener(e -> toggleSeat(btn));
        seatButtons.add(btn);
        panel.add(btn);
    }

  // ==========================================
    // 4. BOTTOM SECTION
    // ==========================================
    private void buildBottomSection() {
        // --- CHECKBOXES (Perfectly Spaced) ---
        JPanel discBox = new RoundedPanel(WHITE_BOX, scale(50));
        discBox.setBounds(scale(87), scale(1770), scale(1012), scale(80));
        discBox.setLayout(null); 
        
        SquareRadio rbReg = new SquareRadio("Regular", 1.0);
        rbReg.setBounds(scale(40), scale(15), scale(200), scale(50));
        
        SquareRadio rbSen = new SquareRadio("Senior", 0.8);
        rbSen.setBounds(scale(280), scale(15), scale(200), scale(50));
        
        SquareRadio rbStu = new SquareRadio("Student", 0.8);
        rbStu.setBounds(scale(520), scale(15), scale(200), scale(50));
        
        SquareRadio rbPwd = new SquareRadio("PWD", 0.8);
        rbPwd.setBounds(scale(760), scale(15), scale(200), scale(50));
        
        rbReg.setSelected(true);
        discountRadios.add(rbReg); discountRadios.add(rbSen); discountRadios.add(rbStu); discountRadios.add(rbPwd);
        
        for(SquareRadio rb : discountRadios) {
            rb.addActionListener(e -> {
                for(SquareRadio other : discountRadios) other.setSelected(false);
                rb.setSelected(true);
                updateSummary();
            });
            discBox.add(rb);
        }
        mainContent.add(discBox);

        // --- STEPPERS (- 0 +) (Perfectly Aligned under Checkboxes) ---
        senStepper = new Stepper();
        senStepper.setBounds(scale(260), scale(1870), scale(240), scale(70));
        mainContent.add(senStepper);

        stuStepper = new Stepper();
        stuStepper.setBounds(scale(500), scale(1870), scale(240), scale(70));
        mainContent.add(stuStepper);

        pwdStepper = new Stepper();
        pwdStepper.setBounds(scale(740), scale(1870), scale(240), scale(70));
        mainContent.add(pwdStepper);

        // --- FULL WIDTH TRIP SUMMARY ---
        JPanel sumBox = new RoundedPanel(WHITE_BOX, scale(31));
        sumBox.setBounds(scale(87), scale(1960), scale(1012), scale(600)); 
        sumBox.setLayout(null);
        
        JLabel sumLbl = createLabel("Trip Summary", TEXT_DARK, 70, false);
        sumLbl.setFont(new Font("Serif", Font.PLAIN, scale(70)));
        sumLbl.setBounds(0, scale(40), scale(1012), scale(80));
        sumBox.add(sumLbl);

        JPanel sLine = new RoundedPanel(Color.decode("#FF8888"), scale(4));
        sLine.setBounds(scale(80), scale(160), scale(852), scale(4));
        sumBox.add(sLine);

        JLabel fL = createLabel("Fare:", TEXT_DARK, 90, false);
        fL.setBounds(scale(80), scale(200), scale(250), scale(100));
        fL.setHorizontalAlignment(SwingConstants.LEFT);
        sumBox.add(fL);

        summaryFare = createLabel("₱0", TEXT_DARK, 90, false);
        summaryFare.setBounds(scale(650), scale(200), scale(280), scale(100));
        summaryFare.setHorizontalAlignment(SwingConstants.RIGHT);
        sumBox.add(summaryFare);

        JLabel sL = createLabel("Seat:", TEXT_DARK, 90, false);
        sL.setBounds(scale(80), scale(350), scale(250), scale(100));
        sL.setHorizontalAlignment(SwingConstants.LEFT);
        sumBox.add(sL);

        summarySeat = createLabel("-", TEXT_DARK, 80, false);
        summarySeat.setBounds(scale(400), scale(350), scale(530), scale(100));
        summarySeat.setHorizontalAlignment(SwingConstants.RIGHT);
        sumBox.add(summarySeat);

        mainContent.add(sumBox);

        // --- CONFIRM BUTTON ---
        confirmBtn = new RoundedButton("confirm", scale(49), TEXT_RED, Color.WHITE);
        confirmBtn.setBounds(scale(87), scale(2600), scale(1012), scale(140)); 
        confirmBtn.setFont(new Font("Serif", Font.BOLD, scale(100))); 
        confirmBtn.addActionListener(e -> {
            if(selectedSeats.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please select a seat first.");
                return;
            }
            
            // Get locations
            String toLoc = btnTo.getText();
            if (toLoc.equals("Arrival Point")) {
                JOptionPane.showMessageDialog(this, "Please select an arrival point first.");
                return;
            }

            double baseFare = (toLoc.contains("Castillejos")) ? 60.0 : 50.0;
            int totalDisc = senStepper.count + stuStepper.count + pwdStepper.count;
            int regSeats = selectedSeats.size() - totalDisc;
            double finalFare = (regSeats * baseFare) + (totalDisc * baseFare * 0.8);
            
            // --- AUTOMATIC EXTRAPOLATION FOR DRIVER DASHBOARD ---
            String city = "";
            String bgy = "";
            String landmark = "";
            
            if (toLoc.contains(",")) {
                String[] parts = toLoc.split(",");
                city = parts[0].trim();
                String rest = parts[1].trim();
                if (rest.contains("(")) {
                    bgy = rest.substring(0, rest.indexOf("(")).trim();
                    landmark = rest.substring(rest.indexOf("(") + 1, rest.indexOf(")")).trim();
                } else {
                    bgy = rest;
                }
            } else {
                city = toLoc;
            }

            // Ingest each passenger seat selection straight onto the dashboard list
            for (int seat : selectedSeats) {
                BusDashboard.addPassenger(city, bgy, landmark, seat);
            }
            
            // Mark seats as paid
            confirmedSeats.addAll(selectedSeats);
            selectedSeats.clear();
            
            String receipt = "--- VICTORY LINER RECEIPT ---\n" +
        "Driver: " + userName + "\n" +
        "Total Fare: ₱" + (int)finalFare + "\n" +
        "-----------------------------\n" +
        "Data successfully saved to database.";
//db connection
try {
    Connection conn = DBConnection.getConnection();

    String sql = "INSERT INTO tickets (transaction_code, time, seat, origin, destination, fare, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

    PreparedStatement pst = conn.prepareStatement(sql);

    String seats = selectedSeats.toString();

    pst.setString(1, "TXN-" + System.currentTimeMillis());
    pst.setString(2, "TIME-" + System.currentTimeMillis());
    pst.setString(3, seats);
    pst.setString(4, btnFrom.getText());
    pst.setString(5, btnTo.getText());
    pst.setDouble(6, finalFare);
    pst.setString(7, "PAID");

    pst.executeUpdate();

    pst.close();
    conn.close();

} catch (Exception ex) {
    ex.printStackTrace();
    JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
}

    System.out.println(receipt); // NetBeans console print

            // Build receipt display values
            String fromText = btnFrom.getText();
            String toText   = btnTo.getText();

            // Calculate distance label (simple placeholder based on destination)
            String distText;
            if (toText.contains("Castillejos")) distText = "~30 km";
            else if (toText.contains("Subic"))   distText = "~12 km";
            else                                  distText = "~5 km";

            // Cash tendered = fare (exact amount — player can extend later)
            double cashTendered = finalFare;

            // Close TicketingDevice and open ReceiptPrinter
            final String fFrom  = fromText;
            final String fTo    = toText;
            final String fDist  = distText;
            final double fFare  = finalFare;
            final double fCash  = cashTendered;

            dispose(); // Close the ticketing window

            SwingUtilities.invokeLater(() ->
                new ReceiptPrinter(fFrom, fTo, fDist, fFare, fCash)
            );

            resetAll();
        });
        mainContent.add(confirmBtn);
    }

    // ==========================================
    // LOGIC & EVENTS
    // ==========================================

        private void toggleSeat(SeatButton btn) {

        // ====================================================
        // CHANGE SEAT MODE
        // ====================================================
        if (changeSeatMode) {

            // --- STEP 1: User must first tap an OCCUPIED seat ---
            if (seatToChange == null) {

                boolean isOccupied = confirmedSeats.contains(btn.seatId) || selectedSeats.contains(btn.seatId);

                if (isOccupied) {
                    seatToChange = btn.seatId;
                    btn.isSelected = true;
                    btn.repaint();
                    JOptionPane.showMessageDialog(this,
                        "Seat W" + btn.seatId + " selected.\n\nNow tap an empty (blue) seat to move the passenger there.");
                } else {
                    JOptionPane.showMessageDialog(this,
                        "That seat is empty.\nPlease tap a yellow (occupied) seat first.");
                }

            // --- STEP 2: User now taps the NEW empty seat ---
            } else {

                boolean isOccupied = confirmedSeats.contains(btn.seatId) || selectedSeats.contains(btn.seatId);

                // Can't move to an already occupied seat
                if (isOccupied) {
                    JOptionPane.showMessageDialog(this,
                        "Seat W" + btn.seatId + " is already occupied.\nPlease choose an empty (blue) seat.");
                    return;
                }

                // Can't move a seat to itself
                if (btn.seatId == seatToChange) {
                    JOptionPane.showMessageDialog(this, "That is the same seat. Choose a different empty seat.");
                    return;
                }

                // Perform the swap
                if (confirmedSeats.contains(seatToChange)) {
                    confirmedSeats.remove((Integer) seatToChange);
                    confirmedSeats.add(btn.seatId);
                } else if (selectedSeats.contains(seatToChange)) {
                    selectedSeats.remove((Integer) seatToChange);
                    selectedSeats.add(btn.seatId);
                }

                // Refresh visuals for both the old seat and the new seat
                for (SeatButton sb : seatButtons) {
                    if (sb.seatId == seatToChange || sb.seatId == btn.seatId) {
                        sb.isSelected = selectedSeats.contains(sb.seatId);
                        sb.repaint();
                    }
                }

                JOptionPane.showMessageDialog(this,
                    "✓ Seat changed successfully!\n\nW" + seatToChange + "  →  W" + btn.seatId);

                // Reset change seat mode
                seatToChange = null;
                changeSeatMode = false;
                changeSeatBtn.setBackground(Color.WHITE);
                changeSeatBtn.setForeground(TEXT_DARK);

                updateSummary();
            }

            return; // IMPORTANT: stop here, don't run normal toggle logic below
        }

        // ====================================================
        // NORMAL SEAT SELECTION (unchanged from your original)
        // ====================================================
        if (confirmedSeats.contains(btn.seatId)) {
            JOptionPane.showMessageDialog(this, "This seat is already paid for!");
            return;
        }
        if (selectedSeats.contains(btn.seatId)) {
            selectedSeats.remove((Integer) btn.seatId);
            btn.isSelected = false;
        } else {
            selectedSeats.add(btn.seatId);
            btn.isSelected = true;
        }
        int totalDiscounts = senStepper.count + stuStepper.count + pwdStepper.count;
        if (totalDiscounts > selectedSeats.size()) {
            senStepper.setCount(0); stuStepper.setCount(0); pwdStepper.setCount(0);
        }
        btn.repaint();
        updateSummary();
    }
    
    public void updateSummary() {
        int totalSeats = selectedSeats.size();
        
        if(totalSeats == 0) {
            summaryFare.setText("₱0");
            summarySeat.setText("-");
        } else {
            String toLoc = btnTo.getText();
            double baseFare = (toLoc.contains("Castillejos")) ? 60.0 : 50.0;
            
            int totalDiscounts = senStepper.count + stuStepper.count + pwdStepper.count;
            int regularSeats = totalSeats - totalDiscounts;

            double finalFare = (regularSeats * baseFare) + (totalDiscounts * baseFare * 0.8);
            
            summaryFare.setText("₱" + (int)finalFare);
            
            StringBuilder sb = new StringBuilder();
            for(int i : selectedSeats) sb.append("W").append(i).append(" ");
            summarySeat.setText(sb.toString().trim());
        }

        countPaid.setText(String.valueOf(confirmedSeats.size())); 
        countUnpaid.setText(String.valueOf(selectedSeats.size())); 
        countAvail.setText(String.valueOf(33 - confirmedSeats.size() - selectedSeats.size()));
    }

    private void resetAll() {
        changeSeatMode = false;
        seatToChange = null;
        if (changeSeatBtn != null) {
        changeSeatBtn.setBackground(Color.WHITE);
        changeSeatBtn.setForeground(TEXT_DARK);
}
        senStepper.setCount(0); stuStepper.setCount(0); pwdStepper.setCount(0);
        btnFrom.setText("Starting Point"); btnFrom.setBackground(FIELD_BG); btnFrom.setForeground(FIELD_TEXT);
        btnTo.setText("Arrival Point"); btnTo.setBackground(FIELD_BG); btnTo.setForeground(FIELD_TEXT);
        discountRadios.get(0).doClick(); // Resets to Regular
        updateSummary();
    }

    // ==========================================
    // HELPERS & CUSTOM COMPONENTS
    // ==========================================
    
    private void setProfilePicture(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            int w = profilePicLabel.getWidth(); int h = profilePicLabel.getHeight();
            BufferedImage circleBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = circleBuffer.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setClip(new Ellipse2D.Float(0, 0, w, h));
            g2.drawImage(img, 0, 0, w, h, null);
            g2.dispose();
            
            // --- ADDED: Stash the cropped icon statically to keep it across views ---
            profileIcon = new ImageIcon(circleBuffer);
            profilePicLabel.setIcon(profileIcon);
        } catch (Exception e) {}
    }

    private int scale(double value) { return (int) Math.round(value * SCALE); }

    private JLabel createLabel(String text, Color color, int size, boolean bold) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, scale(size)));
        l.setForeground(color);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    void setFromLocation(String finalDest) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    void setToLocation(String finalDest) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private Object VictoryDashboardLogin() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    // --- FIXED STEPPER (NO MORE "...") ---
    class Stepper extends JPanel {
        int count = 0;
        JLabel lblCount;
        public Stepper() {
            setLayout(null); // Absolute layout prevents wrapping
            setOpaque(false);
            
            JButton btnMinus = new JButton("-");
            btnMinus.setBounds(scale(5), scale(10), scale(60), scale(50));
            styleStepperBtn(btnMinus);
            btnMinus.addActionListener(e -> { if(count > 0) { count--; lblCount.setText(String.valueOf(count)); updateSummary(); } });
            
            lblCount = createLabel("0", TEXT_RED, 40, true);
            lblCount.setBounds(scale(65), scale(10), scale(90), scale(50));
            
            JButton btnPlus = new JButton("+");
            btnPlus.setBounds(scale(155), scale(10), scale(60), scale(50));
            styleStepperBtn(btnPlus);
            btnPlus.addActionListener(e -> { 
                int totalDisc = senStepper.count + stuStepper.count + pwdStepper.count;
                if(totalDisc < selectedSeats.size()) { count++; lblCount.setText(String.valueOf(count)); updateSummary(); }
                else { JOptionPane.showMessageDialog(null, "Discounts cannot exceed selected seats!"); }
            });
            
            add(btnMinus); add(lblCount); add(btnPlus);
        }
        public void setCount(int c) { this.count = c; lblCount.setText(String.valueOf(c)); }
        private void styleStepperBtn(JButton b) {
            b.setFont(new Font("SansSerif", Font.BOLD, scale(40))); // Sized perfectly
            b.setForeground(Color.BLACK);
            b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            b.setMargin(new Insets(0,0,0,0)); // REMOVES THE "..." PADDING ISSUE
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), scale(40), scale(40));
            super.paintComponent(g); g2.dispose();
        }
    }

    class SquareRadio extends JButton {
        boolean selected = false;
        double rate;
        public SquareRadio(String text, double rate) {
            super("   " + text);
            this.rate = rate;
            setFont(new Font("SansSerif", Font.PLAIN, scale(20)));
            setForeground(Color.GRAY);
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setHorizontalAlignment(SwingConstants.LEFT);
            setMargin(new Insets(0,0,0,0));
        }
        public void setSelected(boolean b) { this.selected = b; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(selected ? Color.decode("#8A8A8A") : Color.decode("#D9D9D9"));
            g2.fillRoundRect(0, getHeight()/2 - scale(15), scale(30), scale(30), scale(8), scale(8));
            super.paintComponent(g);
            g2.dispose();
        }
    }

    class SeatButton extends JButton {
        int seatId;
        boolean isSelected = false;
        Image imgBlue = null;
        Image imgYellow = null;

        public SeatButton(int id) {
            this.seatId = id;
            setContentAreaFilled(false); setBorderPainted(false); setCursor(new Cursor(Cursor.HAND_CURSOR));
            try {
                File fBlue = new File("C:\\Users\\dhelrikk jay\\Blue Seat\\" + id + ".png");
                File fYellow = new File("C:\\Users\\dhelrikk jay\\Yellow Seat\\" + (id + 33) + ".png");
                if(fBlue.exists()) imgBlue = ImageIO.read(fBlue).getScaledInstance(scale(80), scale(75), Image.SCALE_SMOOTH);
                if(fYellow.exists()) imgYellow = ImageIO.read(fYellow).getScaledInstance(scale(80), scale(75), Image.SCALE_SMOOTH);
            } catch(Exception e){}
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if(confirmedSeats.contains(seatId)) {
                if (imgYellow != null) g2.drawImage(imgYellow, 0, 0, null);
                else { 
                    g2.setColor(Color.decode("#405860")); g2.fillRoundRect(0, scale(15), getWidth(), getHeight()-scale(15), scale(10), scale(10)); 
                    g2.setColor(Color.decode("#EAEB0B")); g2.fillRoundRect(scale(5), 0, getWidth()-scale(10), getHeight()-scale(20), scale(15), scale(15)); 
                }
            } else if(isSelected) {
                if (imgYellow != null) g2.drawImage(imgYellow, 0, 0, null);
                else { 
                    g2.setColor(Color.decode("#405860")); g2.fillRoundRect(0, scale(15), getWidth(), getHeight()-scale(15), scale(10), scale(10)); 
                    g2.setColor(Color.decode("#EAEB0B")); g2.fillRoundRect(scale(5), 0, getWidth()-scale(10), getHeight()-scale(20), scale(15), scale(15)); 
                }
            } else {
                if (imgBlue != null) g2.drawImage(imgBlue, 0, 0, null);
                else { 
                    g2.setColor(Color.decode("#405860")); g2.fillRoundRect(0, scale(15), getWidth(), getHeight()-scale(15), scale(10), scale(10)); 
                    g2.setColor(Color.decode("#87CEEB")); g2.fillRoundRect(scale(5), 0, getWidth()-scale(10), getHeight()-scale(20), scale(15), scale(15)); 
                }
            }
            
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Serif", Font.BOLD, scale(34)));
            FontMetrics fm = g2.getFontMetrics();
            int sw = fm.stringWidth(String.valueOf(seatId));
            g2.drawString(String.valueOf(seatId), (getWidth()-sw)/2, (getHeight()/2) + scale(5));
            g2.dispose();
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

    class RoundedButton extends JButton {
        private int radius;
        public RoundedButton(String text, int radius, Color bg, Color fg) {
            super(text); this.radius = radius; setBackground(bg); setForeground(fg);
            setContentAreaFilled(false); setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(0,0,0,0)); 
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground()); g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            super.paintComponent(g); g2.dispose();
        }
    }

    private void customizeScrollBar(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { this.thumbColor = new Color(0, 0, 0, 50); this.trackColor = new Color(0,0,0,0); }
            @Override protected JButton createDecreaseButton(int orientation) { JButton b=new JButton(); b.setPreferredSize(new Dimension(0,0)); return b;}
            @Override protected JButton createIncreaseButton(int orientation) { JButton b=new JButton(); b.setPreferredSize(new Dimension(0,0)); return b;}
        });
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(scale(15), 0));
    }
}
