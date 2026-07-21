package itv;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

/**
 * Servidor HTTP seguro con SSL/TLS. Escucha en el puerto 12345 y atiende
 * peticiones HTTPS. Cada cliente se maneja en un hilo independiente.
 *
 * @author AC.
 */
public class ServidorHTTP {

  // Logger para registrar eventos y errores del servidor
  private static final Logger logger = configurarLogger();

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {

    try {
      // PASO 1: Cargar el almacén de claves (KeyStore)
      // Generado con: keytool -genkey -alias claveSsl -keyalg RSA -keystore AlmacenSSL
      KeyStore keyStore = KeyStore.getInstance("JKS");

      try ( FileInputStream keyFile = new FileInputStream("AlmacenSSL")) {
        keyStore.load(keyFile, "123456".toCharArray());
      } catch (IOException e) {
        logger.severe("Error leyendo el archivo de claves: " + e.getMessage());
        throw e;
      }

      // PASO 2: Crear el gestor de claves (KeyManagerFactory)
      // Se encarga de manejar las claves del certificado SSL
      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
      keyManagerFactory.init(keyStore, "123456".toCharArray());

      // PASO 3: Crear el contexto SSL con el protocolo TLS
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

      // PASO 4: Crear el factory que generará los sockets SSL
      SSLServerSocketFactory factory = sslContext.getServerSocketFactory();

      // PASO 5: Crear el socket del servidor en el puerto 12345
      SSLServerSocket socketServidorSsl = (SSLServerSocket) factory.createServerSocket(12345);
      System.out.println("Servidor arrancado en https://localhost:12345");

      logger.info("Servidor SSL escuchando en el puerto 12345");

      // PASO 6: Crear el recurso compartido (gestión de citas y usuarios)
      RecursoCompartido rcitv = new RecursoCompartido();

      // PASO 7: Bucle principal - acepta conexiones y lanza hilos
      while (true) {
        // Esperamos a que un cliente se conecte
        SSLSocket socketSsl = (SSLSocket) socketServidorSsl.accept();
        System.out.println("Cliente conectado");

        // Creamos un hilo para atender a este cliente
        Thread hilo = new Thread(new HiloServidor(socketSsl, rcitv));
        hilo.start(); // El hilo se ejecuta y el servidor sigue escuchando
      }

    } catch (KeyStoreException e) {
      logger.severe("Error con el tipo de KeyStore");
    } catch (NoSuchAlgorithmException e) {
      logger.severe("Algoritmo no soportado");
    } catch (CertificateException e) {
      logger.severe("Error con el certificado");
    } catch (UnrecoverableKeyException e) {
      logger.severe("No se puede acceder a la clave");
    } catch (KeyManagementException e) {
      logger.severe("Error Inicializando SSL");
    } catch (IOException e) {
      logger.severe("Error de entrada/salida: " + e.getMessage());
    }
  }

  /**
   * Configura el sistema de logging del servidor. Los logs se guardan en un
   * archivo "log.txt" con formato personalizado.
   *
   * @return Logger configurado para usar en toda la aplicación
   */
  private static Logger configurarLogger() {
    Logger logger = Logger.getLogger("MiLog");

    try {
      // Creamos el manejador de archivo (append=true para añadir al final)
      FileHandler fh = new FileHandler("log.txt", true);

      // Formato personalizado: (fecha) mensaje
      fh.setFormatter(new Formatter() {
        @Override
        public String format(LogRecord record) {
          String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
          return String.format("(%s) %s%n", fechaHora, record.getMessage());
        }
      });

      // Asignamos el manejador al logger
      logger.addHandler(fh);

      // Los mensajes se muestran tanto en consola como en archivo
      logger.setUseParentHandlers(true);

      // Registramos todos los niveles de mensajes (INFO, WARNING, SEVERE, etc.)
      logger.setLevel(Level.ALL);

    } catch (IOException | SecurityException e) {
      System.err.println("No se pudo configurar el Logger");
    }

    return logger;
  }
}
