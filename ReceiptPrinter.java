package ticketingdevice;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ReceiptPrinter extends JFrame {

    // Match the app's color scheme
    private static final Color BG_PLATE    = Color.decode("#FFCDC7");
    private static final Color RECEIPT_BG  = new Color(245, 243, 238);
    private static final Color DARK        = new Color(26, 26, 26);
    private static final Color MUTED       = new Color(110, 110, 110);
    private static final Color GREEN       = new Color(34, 110, 34);
    private static final Color DASH_COLOR  = new Color(180, 180, 180);

    // Receipt data — set these before showing the receipt
    private String fromVal;
    private String toVal;
    private String distVal;
    private double fare;
    private double cash;

    public ReceiptPrinter(String from, String to, String distance, double fare, double cash) {
        this.fromVal = from.isEmpty() ? "—" : from;
        this.toVal   = to.isEmpty()   ? "—" : to;
        this.distVal = distance.isEmpty() ? "—" : distance;
        this.fare    = fare;
        this.cash    = cash;

        setTitle("Victory Receipt");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(BG_PLATE);
        setLayout(new GridBagLayout());

        add(buildReceipt());
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel buildReceipt() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(RECEIPT_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(18, 16, 14, 16)
        ));
        panel.setPreferredSize(new Dimension(230, 464));

        // ── HEADER ──
        JLabel brand = new JLabel("VICTORY", SwingConstants.CENTER);
        brand.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 22));
        brand.setForeground(DARK);
        brand.setAlignmentX(CENTER_ALIGNMENT);
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        // Decorative lines flanking the brand name (matching screenshot)
        JPanel brandRow = new JPanel(new BorderLayout(6, 0));
        brandRow.setBackground(RECEIPT_BG);
        brandRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JPanel leftLine = dashLine(); JPanel rightLine = dashLine();
        brandRow.add(leftLine, BorderLayout.WEST);
        brandRow.add(brand, BorderLayout.CENTER);
        brandRow.add(rightLine, BorderLayout.EAST);
        panel.add(brandRow);

        JLabel sub = new JLabel("RECEIPT", SwingConstants.CENTER);
        sub.setFont(new Font("Monospaced", Font.PLAIN, 8));
        sub.setForeground(MUTED);
        sub.setAlignmentX(CENTER_ALIGNMENT);
        sub.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        panel.add(sub);

        panel.add(vgap(10));
        panel.add(hRule());     // solid line
        panel.add(vgap(2));
        panel.add(dashRow());   // — — — — — — — — —
        panel.add(vgap(8));

        // ── TRIP FIELDS ──
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));

        panel.add(fieldRow("From:",     fromVal));
        panel.add(fieldRow("To:",       toVal));
        panel.add(fieldRow("Distance:", distVal));
        panel.add(fieldRow("Date:",     date));
        panel.add(fieldRow("Time:",     time));

        panel.add(vgap(8));
        panel.add(dashRow());   // — — — — — — — — —
        panel.add(vgap(6));

        // ── TOTALS ──
        panel.add(totalRow("Fare:",   pesos(fare),        DARK,  false));
        panel.add(vgap(4));
        panel.add(totalRow("Cash:",   pesos(cash),        MUTED, false));
        panel.add(vgap(4));
        panel.add(totalRow("Change:", pesos(cash - fare), GREEN, true));

        panel.add(vgap(6));
        panel.add(dashRow());   // — — — — — — — — —
        panel.add(vgap(14));

        // ── EXIT BUTTON ──
        JButton exitBtn = new JButton("✕  EXIT");
        exitBtn.setFont(new Font("Monospaced", Font.BOLD, 11));
        exitBtn.setForeground(RECEIPT_BG);
        exitBtn.setBackground(DARK);
        exitBtn.setOpaque(true);
        exitBtn.setBorderPainted(false);
        exitBtn.setFocusPainted(false);
        exitBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exitBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        exitBtn.setAlignmentX(CENTER_ALIGNMENT);

        exitBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { exitBtn.setBackground(new Color(55, 55, 55)); }
            public void mouseExited(MouseEvent e)  { exitBtn.setBackground(DARK); }
        });

        exitBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to exit this receipt?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (choice == JOptionPane.YES_OPTION) {
                dispose();
                // Open the Bus Dashboard after confirming exit
                SwingUtilities.invokeLater(() -> new BusDashboard().setVisible(true));
            }
        });

        panel.add(exitBtn);

        panel.add(vgap(6));

        // ── PRINT RECEIPT BUTTON ──
        Color PRINT_BG    = new Color(34, 110, 34);
        Color PRINT_HOVER = new Color(26, 85, 26);

        JButton printBtn = new JButton("⎙  PRINT RECEIPT");
        printBtn.setFont(new Font("Monospaced", Font.BOLD, 11));
        printBtn.setForeground(Color.WHITE);
        printBtn.setBackground(PRINT_BG);
        printBtn.setOpaque(true);
        printBtn.setBorderPainted(false);
        printBtn.setFocusPainted(false);
        printBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        printBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        printBtn.setAlignmentX(CENTER_ALIGNMENT);

        printBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { printBtn.setBackground(PRINT_HOVER); }
            public void mouseExited(MouseEvent e)  { printBtn.setBackground(PRINT_BG); }
        });

        printBtn.addActionListener(e -> {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Victory Receipt");

            // Scale the panel to fit a standard receipt page
            job.setPrintable((graphics, pageFormat, pageIndex) -> {
                if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
                Graphics2D g2d = (Graphics2D) graphics;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                double scaleX = pageFormat.getImageableWidth()  / panel.getWidth();
                double scaleY = pageFormat.getImageableHeight() / panel.getHeight();
                double scale  = Math.min(scaleX, scaleY);
                g2d.scale(scale, scale);

                panel.printAll(g2d);
                return Printable.PAGE_EXISTS;
            });

            if (job.printDialog()) {
                try {
                    job.print();
                } catch (PrinterException ex) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Printing failed: " + ex.getMessage(),
                        "Print Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        panel.add(printBtn);
        return panel;
    }

    // ── FIELD ROWS ──

    private JPanel fieldRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(RECEIPT_BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lbl.setForeground(MUTED);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Monospaced", Font.PLAIN, 11));
        val.setForeground(DARK);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private JPanel totalRow(String label, String value, Color valColor, boolean bold) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(RECEIPT_BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lbl.setForeground(new Color(60, 60, 60));

        JLabel val = new JLabel(value);
        val.setFont(new Font("Monospaced", bold ? Font.BOLD : Font.PLAIN, 12));
        val.setForeground(valColor);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    // ── DECORATORS ──

    /** Dashed separator: — — — — — — — — — */
    private JPanel dashRow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        p.setBackground(RECEIPT_BG);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
        for (int i = 0; i < 10; i++) {
            JLabel d = new JLabel("—");
            d.setFont(new Font("Monospaced", Font.PLAIN, 11));
            d.setForeground(DASH_COLOR);
            p.add(d);
        }
        return p;
    }

    /** Solid thin horizontal rule */
    private JSeparator hRule() {
        JSeparator s = new JSeparator();
        s.setForeground(new Color(210, 208, 200));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    /** Small decorative horizontal dash panel for flanking the brand name */
    private JPanel dashLine() {
        JPanel p = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(DASH_COLOR);
                float[] dash = {3f, 3f};
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, dash, 0));
                g2.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
                g2.dispose();
            }
        };
        p.setBackground(RECEIPT_BG);
        p.setPreferredSize(new Dimension(30, 20));
        return p;
    }

    private Box.Filler vgap(int h) {
        return (Box.Filler) Box.createVerticalStrut(h);
    }

    private String pesos(double amount) {
        long rounded = Math.round(amount);
        return rounded == 1 ? "1 Peso" : rounded + " Pesos";
    }

    // ── ENTRY POINT (standalone test) ──
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() ->
            new ReceiptPrinter(
                "Olongapo",   // from
                "Subic",      // to
                "12 km",      // distance
                49.0,         // fare
                50.0          // cash
            )
        );
    }
}