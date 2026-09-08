package analisiscromatico;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class AnalisisCromaticoApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            VentanaPrincipal ventana = new VentanaPrincipal();
            ventana.setVisible(true);
        });
    }
}

class VentanaPrincipal extends JFrame {

    private final JLabel lblEstado = new JLabel("Carga una imagen para comenzar.");
    private final ModuloCuentagotas moduloCuentagotas = new ModuloCuentagotas();
    private final ModuloCuadrantes moduloCuadrantes = new ModuloCuadrantes();
    private final ModuloAccesibilidad moduloAccesibilidad = new ModuloAccesibilidad();

    VentanaPrincipal() {
        setTitle("Sistema de Analisis Cromatico Avanzado");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 760);
        setMinimumSize(new Dimension(950, 620));
        setLocationRelativeTo(null);

        JLabel titulo = new JLabel("Sistema de Analisis Cromatico Avanzado");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 22));

        JButton btnCargar = new JButton("Cargar imagen");
        btnCargar.setFont(btnCargar.getFont().deriveFont(Font.BOLD));
        btnCargar.addActionListener(e -> cargarImagen());

        JPanel cabecera = new JPanel(new BorderLayout(12, 0));
        cabecera.setBorder(new EmptyBorder(12, 14, 10, 14));
        cabecera.add(titulo, BorderLayout.WEST);
        cabecera.add(btnCargar, BorderLayout.EAST);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Cuentagotas", moduloCuentagotas);
        pestanas.addTab("Cuadrantes", moduloCuadrantes);
        pestanas.addTab("Accesibilidad", moduloAccesibilidad);

        lblEstado.setBorder(new EmptyBorder(6, 14, 8, 14));

        add(cabecera, BorderLayout.NORTH);
        add(pestanas, BorderLayout.CENTER);
        add(lblEstado, BorderLayout.SOUTH);
    }

    private void cargarImagen() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Selecciona una imagen");

        int opcion = selector.showOpenDialog(this);
        if (opcion != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File archivo = selector.getSelectedFile();

        try {
            BufferedImage imagen = ImageIO.read(archivo);

            if (imagen == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "El archivo seleccionado no es una imagen compatible.",
                        "Archivo no valido",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            moduloCuentagotas.setImagen(imagen);
            moduloCuadrantes.setImagen(imagen);
            moduloAccesibilidad.setImagen(imagen);

            lblEstado.setText(
                    "Imagen: " + archivo.getName()
                            + " | Resolucion: " + imagen.getWidth()
                            + " x " + imagen.getHeight()
            );

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "No fue posible cargar la imagen:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}

interface ModuloImagen {
    void setImagen(BufferedImage imagen);
}

class ImagenEscaladaLabel extends JLabel {

    private BufferedImage imagen;
    private int anchoDibujado;
    private int altoDibujado;
    private int offsetX;
    private int offsetY;

    ImagenEscaladaLabel() {
        setOpaque(true);
        setBackground(new Color(35, 35, 35));
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setPreferredSize(new Dimension(650, 500));
    }

    public void setImagen(BufferedImage imagen) {
        this.imagen = imagen;
        repaint();
    }

    public BufferedImage getImagen() {
        return imagen;
    }

    public Point coordenadaReal(int xComponente, int yComponente) {
        if (imagen == null || anchoDibujado <= 0 || altoDibujado <= 0) {
            return null;
        }

        int xLocal = xComponente - offsetX;
        int yLocal = yComponente - offsetY;

        if (xLocal < 0 || yLocal < 0
                || xLocal >= anchoDibujado || yLocal >= altoDibujado) {
            return null;
        }

        double factorX = (double) imagen.getWidth() / anchoDibujado;
        double factorY = (double) imagen.getHeight() / altoDibujado;

        int xImagen = (int) Math.floor(xLocal * factorX);
        int yImagen = (int) Math.floor(yLocal * factorY);

        xImagen = limitar(xImagen, 0, imagen.getWidth() - 1);
        yImagen = limitar(yImagen, 0, imagen.getHeight() - 1);

        return new Point(xImagen, yImagen);
    }

    public Point coordenadaPantalla(int xImagen, int yImagen) {
        if (imagen == null || anchoDibujado <= 0 || altoDibujado <= 0) {
            return null;
        }

        int x = offsetX
                + (int) Math.round((double) xImagen * anchoDibujado / imagen.getWidth());
        int y = offsetY
                + (int) Math.round((double) yImagen * altoDibujado / imagen.getHeight());

        return new Point(x, y);
    }

    private int limitar(int valor, int minimo, int maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (imagen == null) {
            dibujarMensaje(g, "No hay imagen cargada");
            return;
        }

        int areaW = getWidth();
        int areaH = getHeight();

        double escala = Math.min(
                (double) areaW / imagen.getWidth(),
                (double) areaH / imagen.getHeight()
        );

        anchoDibujado = Math.max(1, (int) Math.round(imagen.getWidth() * escala));
        altoDibujado = Math.max(1, (int) Math.round(imagen.getHeight() * escala));

        offsetX = (areaW - anchoDibujado) / 2;
        offsetY = (areaH - altoDibujado) / 2;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        g2.drawImage(
                imagen,
                offsetX,
                offsetY,
                anchoDibujado,
                altoDibujado,
                null
        );

        g2.dispose();
    }

    private void dibujarMensaje(Graphics g, String mensaje) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Color.LIGHT_GRAY);
        FontMetrics fm = g2.getFontMetrics();
        int x = Math.max(10, (getWidth() - fm.stringWidth(mensaje)) / 2);
        int y = Math.max(20, getHeight() / 2);
        g2.drawString(mensaje, x, y);
        g2.dispose();
    }
}

class ModuloCuentagotas extends JPanel implements ModuloImagen {

    private final VistaCuentagotas vista = new VistaCuentagotas();
    private final JLabel lblCoordenadas = new JLabel("X: -   Y: -");
    private final JLabel lblRGB = new JLabel("RGB: -");
    private final JLabel lblHex = new JLabel("HEX: -");
    private final JPanel muestraColor = new JPanel();

    ModuloCuentagotas() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel datos = new JPanel();
        datos.setLayout(new BoxLayout(datos, BoxLayout.Y_AXIS));
        datos.setBorder(BorderFactory.createTitledBorder("Pixel seleccionado"));
        datos.setPreferredSize(new Dimension(250, 200));

        lblCoordenadas.setBorder(new EmptyBorder(8, 8, 8, 8));
        lblRGB.setBorder(new EmptyBorder(8, 8, 8, 8));
        lblHex.setBorder(new EmptyBorder(8, 8, 8, 8));

        muestraColor.setPreferredSize(new Dimension(180, 80));
        muestraColor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        muestraColor.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JLabel ayuda = new JLabel(
                "<html>Haz clic sobre la imagen.<br>"
                        + "El sistema convierte las coordenadas<br>"
                        + "del JLabel a las coordenadas reales<br>"
                        + "del pixel de la imagen.</html>"
        );
        ayuda.setBorder(new EmptyBorder(10, 8, 8, 8));

        datos.add(lblCoordenadas);
        datos.add(lblRGB);
        datos.add(lblHex);
        datos.add(Box.createVerticalStrut(8));
        datos.add(muestraColor);
        datos.add(ayuda);

        vista.setListener((x, y, color) -> {
            lblCoordenadas.setText("X: " + x + "   Y: " + y);
            lblRGB.setText(
                    "RGB: (" + color.getRed()
                            + ", " + color.getGreen()
                            + ", " + color.getBlue() + ")"
            );
            lblHex.setText(String.format(
                    "HEX: #%02X%02X%02X",
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue()
            ));
            muestraColor.setBackground(color);
        });

        add(vista, BorderLayout.CENTER);
        add(datos, BorderLayout.EAST);
    }

    @Override
    public void setImagen(BufferedImage imagen) {
        vista.setImagen(imagen);
        lblCoordenadas.setText("X: -   Y: -");
        lblRGB.setText("RGB: -");
        lblHex.setText("HEX: -");
        muestraColor.setBackground(getBackground());
    }
}

interface PixelSeleccionadoListener {
    void pixelSeleccionado(int x, int y, Color color);
}

class VistaCuentagotas extends ImagenEscaladaLabel {

    private PixelSeleccionadoListener listener;
    private Point pixelSeleccionado;

    VistaCuentagotas() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                seleccionarPixel(e.getX(), e.getY());
            }
        });
    }

    public void setListener(PixelSeleccionadoListener listener) {
        this.listener = listener;
    }

    private void seleccionarPixel(int xPanel, int yPanel) {
        BufferedImage imagen = getImagen();

        if (imagen == null) {
            return;
        }

        Point p = coordenadaReal(xPanel, yPanel);

        if (p == null) {
            return;
        }

        pixelSeleccionado = p;
        Color color = new Color(imagen.getRGB(p.x, p.y), true);

        if (listener != null) {
            listener.pixelSeleccionado(p.x, p.y, color);
        }

        repaint();
    }

    @Override
    public void setImagen(BufferedImage imagen) {
        super.setImagen(imagen);
        pixelSeleccionado = null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (pixelSeleccionado == null) {
            return;
        }

        Point p = coordenadaPantalla(pixelSeleccionado.x, pixelSeleccionado.y);

        if (p == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setStroke(new BasicStroke(3.0f));
        g2.setColor(Color.BLACK);
        g2.drawOval(p.x - 9, p.y - 9, 18, 18);
        g2.drawLine(p.x - 14, p.y, p.x + 14, p.y);
        g2.drawLine(p.x, p.y - 14, p.x, p.y + 14);

        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(Color.WHITE);
        g2.drawOval(p.x - 7, p.y - 7, 14, 14);

        g2.dispose();
    }
}

class ModuloCuadrantes extends JPanel implements ModuloImagen {

    private static final int PASO = 1;

    private BufferedImage original;
    private final ImagenEscaladaLabel vista = new ImagenEscaladaLabel();
    private final JLabel lblNO = new JLabel("Noroeste: -");
    private final JLabel lblNE = new JLabel("Nordeste: -");
    private final JLabel lblSO = new JLabel("Suroeste: -");
    private final JLabel lblSE = new JLabel("Sudeste: -");
    private final JLabel lblPaso = new JLabel("Paso de escaneo: " + PASO);

    ModuloCuadrantes() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel informacion = new JPanel(new GridLayout(5, 1, 4, 4));
        informacion.setBorder(BorderFactory.createTitledBorder(
                "Color promedio real de cada cuadrante"
        ));

        informacion.add(lblNO);
        informacion.add(lblNE);
        informacion.add(lblSO);
        informacion.add(lblSE);
        informacion.add(lblPaso);

        add(vista, BorderLayout.CENTER);
        add(informacion, BorderLayout.SOUTH);
    }

    @Override
    public void setImagen(BufferedImage imagen) {
        original = imagen;
        procesarCuadrantes();
    }

    private void procesarCuadrantes() {
        if (original == null) {
            vista.setImagen(null);
            return;
        }

        int w = original.getWidth();
        int h = original.getHeight();
        int mitadX = w / 2;
        int mitadY = h / 2;

        Color promedioNO = promedioRegion(0, 0, mitadX, mitadY);
        Color promedioNE = promedioRegion(mitadX, 0, w, mitadY);
        Color promedioSO = promedioRegion(0, mitadY, mitadX, h);
        Color promedioSE = promedioRegion(mitadX, mitadY, w, h);

        BufferedImage resultado = new BufferedImage(
                w,
                h,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = resultado.createGraphics();

        g.setColor(promedioNO);
        g.fillRect(0, 0, mitadX, mitadY);

        g.setColor(promedioNE);
        g.fillRect(mitadX, 0, w - mitadX, mitadY);

        g.setColor(promedioSO);
        g.fillRect(0, mitadY, mitadX, h - mitadY);

        g.setColor(promedioSE);
        g.fillRect(mitadX, mitadY, w - mitadX, h - mitadY);

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(Math.max(1f, Math.min(w, h) / 250f)));
        g.drawLine(mitadX, 0, mitadX, h);
        g.drawLine(0, mitadY, w, mitadY);

        dibujarTextoCuadrante(g, "NO", promedioNO, mitadX / 2, mitadY / 2);
        dibujarTextoCuadrante(g, "NE", promedioNE, mitadX + (w - mitadX) / 2, mitadY / 2);
        dibujarTextoCuadrante(g, "SO", promedioSO, mitadX / 2, mitadY + (h - mitadY) / 2);
        dibujarTextoCuadrante(g, "SE", promedioSE, mitadX + (w - mitadX) / 2, mitadY + (h - mitadY) / 2);

        g.dispose();

        vista.setImagen(resultado);

        lblNO.setText("Noroeste: " + textoColor(promedioNO));
        lblNE.setText("Nordeste: " + textoColor(promedioNE));
        lblSO.setText("Suroeste: " + textoColor(promedioSO));
        lblSE.setText("Sudeste: " + textoColor(promedioSE));
    }

    private Color promedioRegion(int xInicio, int yInicio, int xFin, int yFin) {
        long sumaR = 0;
        long sumaG = 0;
        long sumaB = 0;
        long cantidad = 0;

        for (int y = yInicio; y < yFin; y += PASO) {
            for (int x = xInicio; x < xFin; x += PASO) {
                int rgb = original.getRGB(x, y);

                sumaR += (rgb >> 16) & 0xFF;
                sumaG += (rgb >> 8) & 0xFF;
                sumaB += rgb & 0xFF;
                cantidad++;
            }
        }

        if (cantidad == 0) {
            return Color.BLACK;
        }

        int r = (int) Math.round((double) sumaR / cantidad);
        int g = (int) Math.round((double) sumaG / cantidad);
        int b = (int) Math.round((double) sumaB / cantidad);

        return new Color(r, g, b);
    }

    private void dibujarTextoCuadrante(
            Graphics2D g,
            String nombre,
            Color fondo,
            int centroX,
            int centroY
    ) {
        String texto = nombre + " " + String.format(
                "#%02X%02X%02X",
                fondo.getRed(),
                fondo.getGreen(),
                fondo.getBlue()
        );

        int luminosidad = (fondo.getRed() * 299
                + fondo.getGreen() * 587
                + fondo.getBlue() * 114) / 1000;

        g.setColor(luminosidad >= 140 ? Color.BLACK : Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(14, Math.min(26, original.getWidth() / 35))));

        FontMetrics fm = g.getFontMetrics();
        int x = centroX - fm.stringWidth(texto) / 2;
        int y = centroY + fm.getAscent() / 2;
        g.drawString(texto, x, y);
    }

    private String textoColor(Color c) {
        return String.format(
                "RGB(%d, %d, %d)  #%02X%02X%02X",
                c.getRed(),
                c.getGreen(),
                c.getBlue(),
                c.getRed(),
                c.getGreen(),
                c.getBlue()
        );
    }
}

class ModuloAccesibilidad extends JPanel implements ModuloImagen {

    private static final double[][] MATRIZ_DEUTERANOPIA = {
            {0.367322, 0.860646, -0.227968},
            {0.280085, 0.672501, 0.047413},
            {-0.011820, 0.042940, 0.968881}
    };

    private final ImagenEscaladaLabel originalView = new ImagenEscaladaLabel();
    private final ImagenEscaladaLabel procesadaView = new ImagenEscaladaLabel();

    ModuloAccesibilidad() {
        setLayout(new GridLayout(1, 2, 10, 0));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        add(crearTarjeta("ORIGINAL EN ESPEJO", originalView));
        add(crearTarjeta("SIMULACION DE DEUTERANOPIA", procesadaView));
    }

    private JPanel crearTarjeta(String titulo, ImagenEscaladaLabel vista) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JLabel etiqueta = new JLabel(titulo, SwingConstants.CENTER);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD));
        etiqueta.setBorder(new EmptyBorder(6, 4, 6, 4));

        panel.add(etiqueta, BorderLayout.NORTH);
        panel.add(vista, BorderLayout.CENTER);

        return panel;
    }

    @Override
    public void setImagen(BufferedImage imagen) {
        if (imagen == null) {
            originalView.setImagen(null);
            procesadaView.setImagen(null);
            return;
        }

        BufferedImage espejo = crearEspejoHorizontal(imagen);
        BufferedImage procesada = aplicarDeuteranopia(espejo);

        originalView.setImagen(espejo);
        procesadaView.setImagen(procesada);
    }

    private BufferedImage crearEspejoHorizontal(BufferedImage imagen) {
        int w = imagen.getWidth();
        int h = imagen.getHeight();

        BufferedImage espejo = new BufferedImage(
                w,
                h,
                BufferedImage.TYPE_INT_RGB
        );

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = imagen.getRGB(x, y);
                espejo.setRGB(w - 1 - x, y, rgb);
            }
        }

        return espejo;
    }

    private BufferedImage aplicarDeuteranopia(BufferedImage entrada) {
        int w = entrada.getWidth();
        int h = entrada.getHeight();

        BufferedImage salida = new BufferedImage(
                w,
                h,
                BufferedImage.TYPE_INT_RGB
        );

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = entrada.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                double nuevoR =
                        MATRIZ_DEUTERANOPIA[0][0] * r
                                + MATRIZ_DEUTERANOPIA[0][1] * g
                                + MATRIZ_DEUTERANOPIA[0][2] * b;

                double nuevoG =
                        MATRIZ_DEUTERANOPIA[1][0] * r
                                + MATRIZ_DEUTERANOPIA[1][1] * g
                                + MATRIZ_DEUTERANOPIA[1][2] * b;

                double nuevoB =
                        MATRIZ_DEUTERANOPIA[2][0] * r
                                + MATRIZ_DEUTERANOPIA[2][1] * g
                                + MATRIZ_DEUTERANOPIA[2][2] * b;

                int rr = limitar((int) Math.round(nuevoR));
                int gg = limitar((int) Math.round(nuevoG));
                int bb = limitar((int) Math.round(nuevoB));

                int colorFinal = (rr << 16) | (gg << 8) | bb;
                salida.setRGB(x, y, colorFinal);
            }
        }

        return salida;
    }

    private int limitar(int valor) {
        return Math.max(0, Math.min(255, valor));
    }
}
