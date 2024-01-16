
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/*
Clase de conexión, se encarga de la comunicación entre el cliente y el servidor.
*/
public class Connection implements Runnable {
    private Socket socket;
    private String address;
    private HealthCareConnection hcc;
    private BufferedReader in;
    private PrintWriter out;
    private Integer PORT = 1234;
    private ServerConnector serverConnector;
    private ClientConnector clientConnector;
    private Main controller;

    private long timeReceivedMessage;
    private volatile boolean runState = true;

    private boolean wasClient = false;

    public Connection(String address, Main controller) {
        this.controller = controller;
        try {
            this.address = address;
            boolean seHaPodidoConectarComoCliente;

            this.clientConnector = new ClientConnector(PORT, address);
            clientConnector.setIntentarReconectar(false);
            clientConnector.run();
            this.socket = clientConnector.getSOCKET();
            seHaPodidoConectarComoCliente = clientConnector.isConexionEstablecida();
            if (seHaPodidoConectarComoCliente) {
                wasClient = true;
                System.out.println("Conexion establecida como cliente correctamente");
            } else {
                clientConnector.setIntentarReconectar(true);
                System.out.println("Abortando conexion como cliente...");
                // Iniciar el servidor
                this.serverConnector = new ServerConnector(PORT);
                serverConnector.run();
                this.socket = serverConnector.getClsock();
                System.out.println("Conexion establecida como servidor correctamente");
            }

            // Inicializar los objetos BufferedReader y PrintWriter
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    public void reconnect() {
        try {
            PORT = 1234;
            // Iniciar servidor y esperar conexiones entrantes
            System.out.println("Iniciando servidor...");
            serverConnector = new ServerConnector(PORT);
            serverConnector.run();
            socket = serverConnector.getClsock();
            HealthCareConnection healthCareConnection = new HealthCareConnection(this, 10000);
            setHCC(healthCareConnection);
            new Thread(healthCareConnection).start();

            // Reinicializar los objetos BufferedReader y PrintWriter
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

        } catch (IOException ex) {
            System.err.println("No se ha podido conectar");
        } catch (NullPointerException e) {
            System.out.println("Error en la conexión: " + e);
        }
    }

    public void enviarMensanje(String message) {
        try {
            out.println(message);
        } catch (Exception e) {
            System.out.println(
                    "No se puede enviar el mensaje, no se ha establecido la conexión" + "\n" + "Codigo de error: " + e);
        }
    }

    public void setHCC(HealthCareConnection hch) {
        this.hcc = hch;
    }

    public void mssgIn() {
        String str;
        try {
            while (socket != null && socket.isConnected() && (str = in.readLine()) != null) {
                if (str.equals("ping")) {
                    mssgIn();
                    System.out.println("entra aqui");
                    return;
                }
                controller.recibirMensaje(str);                
                long time = (System.currentTimeMillis());
                setTimeReceivedMessage(time);
            }
        } catch (Exception e) {
            System.err.println("Error en la conexion");
            killSocket();
        }
    }

    public void setTimeReceivedMessage(long timeReceivedMessage) {
        this.timeReceivedMessage = timeReceivedMessage;
    }

    public boolean ping() {
        try {
            out.println("ping");
            return true;
        } catch (Exception e) {
            System.out.println("Error en el envío del heartbeat: " + e);
            return false;
        }
    }

    public void run() {
        while (runState) {
            try {
                // Verificar si la conexión actual está cerrada
                if (socket == null || socket.isClosed() || !socket.isConnected()) {
                    System.out.println("Conexión perdida, intentando reconectar...");
                    Thread.sleep(1000);
                    reconnect();
                }

                // Leer mensajes entrantes
                if (socket != null && socket.isConnected()) {
                    mssgIn();
                }

                // Esperar un poco antes de verificar la conexión de nuevo
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Error en la reconexión: " + e);
            }
        }
    }

    public long getTimeReceivedMessage() {
        return timeReceivedMessage;
    }

    public synchronized void killSocket() {
        try {
            stopHCC();
            in.close();
            out.close();
            if (serverConnector != null) {
                socket.close();
                if (!serverConnector.isSocketClosed()) {
                    serverConnector.killSocket();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket = null;
            PORT = null;
            System.err.println("Matando el socket...");
        }

    }

    public void stopHCC() {
        if (hcc != null) {
            hcc.pararEjecucion();
            hcc = null;
        }
    }

}
