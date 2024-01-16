public class Main {
    Connection connection;
    HealthCareConnection healthCareConnection;
    Vista vista;
    public Main() {
        vista = new Vista(this);
        connection = new Connection("localhost", this);
        healthCareConnection = new HealthCareConnection(connection, 10000);
        new Thread(connection).start();
        new Thread(healthCareConnection).start();
        connection.setHCC(healthCareConnection);
    }

    public static void main(String[] args) {
        new Main();
    }

    public void enviarMensaje(String s) {
        connection.enviarMensanje(s);
    }

    public void recibirMensaje(String s) {
        vista.recibirMensaje(s);
    }

}