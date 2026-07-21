package itv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.logging.*;
import javax.net.ssl.SSLSocket;

/**
 * Maneja las peticiones HTTP de cada cliente en un hilo independiente. Soporta
 * SSL/TLS, autenticación de usuarios y gestión de citas ITV.
 *
 * @author AC.
 */
public class HiloServidor implements Runnable {

  private final int OK = 200;
  private final int NOTFOUND = 404;

  private SSLSocket socketHiloSsl;
  private RecursoCompartido recursoItv;

  private final Logger logger = Logger.getLogger("MiLog");

  /**
   * Constructor del hilo servidor.
   *
   * @param socketCliente Socket SSL del cliente conectado
   * @param rcCons Recurso compartido para acceso a datos
   */
  public HiloServidor(SSLSocket socketCliente, RecursoCompartido rcCons) {
    this.socketHiloSsl = socketCliente;
    this.recursoItv = rcCons;
  }

  @Override
  public void run() {

    try ( SSLSocket socketHiloRun = this.socketHiloSsl;  BufferedReader entrada = new BufferedReader(new InputStreamReader(socketHiloRun.getInputStream()));  PrintWriter salida = new PrintWriter(socketHiloRun.getOutputStream(), true);) {
      
      // Leer la primera línea de la petición HTTP (GET /ruta HTTP/1.1)
      String peticion = entrada.readLine();
      System.out.println("La peticion que nos llega es " + peticion);

      if (peticion != null && (peticion.startsWith("GET") || peticion.startsWith("POST"))) {

        String[] partePeticion = peticion.split(" ");
        String ruta = partePeticion[1];

        // Procesar los headers HTTP para obtener el Content-Length
        int cantidadMetadatos = 0;
        String linea;

        while (!(linea = entrada.readLine()).isBlank()) {

          if (linea.startsWith("Content-Length")) {

            String[] parteCuerpo = linea.split(":");
            String numeroMetadatos = parteCuerpo[1].trim();
            cantidadMetadatos = Integer.parseInt(numeroMetadatos);
            System.out.println("DEBUG>> La cantidad de metadatos que vienen son " + linea);
          }
        }

        // Leer el cuerpo de la petición si existe (datos POST)
        StringBuilder cuerpoMetadatos = new StringBuilder();
        if (cantidadMetadatos > 0) {
          for (int i = 0; i < cantidadMetadatos; i++) {
            int contenidoMetadatos = entrada.read();
            char caracter = (char) contenidoMetadatos;
            cuerpoMetadatos.append(caracter);
            System.out.println("DEBUG >>> caracter es: " + caracter);
          }
          System.out.println("DEBUG>> Los caracteres de los metadatos forman: " + cuerpoMetadatos);
        }
        
        // Procesar la ruta solicitada
        String respuestaHTML;

        
     // ---------- RUTAS DEL SERVIDOR ----------
        // Página de login
        if (ruta.equals("/")) {
          respuestaHTML = construirRespuesta(OK, PaginasHTML.login(""));
        
        // Registro de nuevo usuario
        } else if (ruta.startsWith("/registro") && peticion.startsWith("POST")) {

          try {
            String datosCuerpo = cuerpoMetadatos.toString();

            String[] campos = datosCuerpo.split("&");

            String emailCodificado = campos[0].split("=")[1];

            String emailPostRegistro = java.net.URLDecoder.decode(emailCodificado, "UTF-8");
            String passPostRegistro = campos[1].split("=")[1];
            String patronusEmail = "^[A-Za-z0-9_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,10}$";; 
            String patronusPass = "^[A-Za-z0-9]{6,}$"; // 
            Pattern pEmail = Pattern.compile(patronusEmail);
            Pattern pPass = Pattern.compile(patronusPass);
            Matcher matchEmail = pEmail.matcher(emailPostRegistro);
            Matcher matchPass = pPass.matcher(passPostRegistro);

            boolean emailValido = matchEmail.matches(); 
            boolean passValida = matchPass.matches();

            if (!emailValido || !passValida) {
              respuestaHTML = construirRespuesta(OK, PaginasHTML.login("Error: Email NO valido"));

              logger.log(Level.WARNING, "Error Registro : no se cumplen los parametros de registro " + emailPostRegistro + " y " + passPostRegistro);
            } else {

              boolean registrado = recursoItv.registrarUsuario(emailPostRegistro, passPostRegistro);

              if (registrado) {

                logger.log(Level.WARNING, "Registro completado de usuario " + emailPostRegistro);

                String panel = recursoItv.generarPanel();
                respuestaHTML = construirRespuesta(OK, PaginasHTML.htmlIndex(panel));

              } else {
                respuestaHTML = construirRespuesta(OK, PaginasHTML.login("Error: No se pudo registrar"));
              }
            }

          } catch (Exception e) {

            logger.log(Level.WARNING, "Registro fallido: " + e.getMessage());
            respuestaHTML = construirRespuesta(OK, PaginasHTML.login("Error interno en el registro"));
          }
          
        // Inicio de sesión
        } else if (ruta.startsWith("/inicio") && peticion.startsWith("POST")) {

          try {

            String datosCuerpo = cuerpoMetadatos.toString();

            String[] campos = datosCuerpo.split("&");

            String emailCodificado = campos[0].split("=")[1];
            String emailPostLogin = java.net.URLDecoder.decode(emailCodificado, "UTF-8");
            String passPostLogin = campos[1].split("=")[1];

            boolean autenticado = recursoItv.autenticarUsuario(emailPostLogin, passPostLogin);

            if (autenticado) {

              String panel = recursoItv.generarPanel();
              respuestaHTML = construirRespuesta(OK, PaginasHTML.htmlIndex(panel));
            } else {

              logger.log(Level.SEVERE, "Login Incorrecto para el USUARIO: " + emailPostLogin);
              respuestaHTML = construirRespuesta(OK, PaginasHTML.login("Email o contraseña incorrectos"));
            }
          } catch (Exception e) {
            logger.log(Level.WARNING, "Login fallido: " + e.getMessage());
            respuestaHTML = construirRespuesta(OK, PaginasHTML.login("Error en el login"));
          }
          
        // Panel principal
        } else if (ruta.equals("/inicio")) {

          String panel = recursoItv.generarPanel();

          respuestaHTML = construirRespuesta(OK, PaginasHTML.htmlIndex(panel));

         // Formulario de reserva con matriculas aleatorias
        } else if (ruta.startsWith("/reservar") && peticion.startsWith("GET")) {

          recursoItv.cargarMatriculasAleatorias();

          respuestaHTML = construirRespuesta(OK, PaginasHTML.htmlReservar);

        // Procesa reserva manual
        } else if (ruta.startsWith("/reservar") && peticion.startsWith("POST")) {

          String datosCuerpo = cuerpoMetadatos.toString();
          String matricula = datosCuerpo.split("=")[1];   

          boolean reservada = recursoItv.reservarCitaManual(matricula);

          if (reservada == true) {
            String panel = recursoItv.generarPanel();
            respuestaHTML = construirRespuesta(OK, PaginasHTML.htmlIndex(panel));

          } else {
            String error = "La matricula  " + matricula + " ya tiene cita";
            respuestaHTML = construirRespuesta(OK, PaginasHTML.htmlPasarITV(matricula, error));
          }

        // Muestra formulario para pasar ITV
        } else if (ruta.startsWith("/pasar") && peticion.startsWith("GET")) {

          respuestaHTML = construirRespuesta(OK, PaginasHTML.htmlPasarITV("", ""));
        
        // Procesa la inspección ITV
        } else if (ruta.startsWith("/pasar") && peticion.startsWith("POST")) { // metemos  matricula en pagina 

          String datosCuerpo = cuerpoMetadatos.toString();
          String matricula = datosCuerpo.split("=")[1];
          if (!recursoItv.existeReserva(matricula)) {

            String error = "La Matricula - " + matricula + " - NO tiene cita Asignada en esta ITV <br>" + "<span style='font-size:5px;'>Estas a tiempo! huye a otra ITV! " + "y deja de hacer ZOOM en el taller EL OPORTUNO! :)</span>";
            respuestaHTML = construirRespuesta(OK, PaginasHTML.htmlPasarITV(matricula, error));
          } else {

            recursoItv.ocuparLinea(matricula);

            String[] respuestas = new String[5];
            for (int i = 0; i < 5; i++) {
              respuestas[i] = recursoItv.fraseSeleccionada();
            }

            String resultadoPruebas = recursoItv.realizarITV(matricula, respuestas);

            recursoItv.vaciarLinea(matricula);

            String resultadoConDiseno = PaginasHTML.htmlResultado(resultadoPruebas);

            respuestaHTML = construirRespuesta(OK, PaginasHTML.htmlPasarITV(matricula, resultadoConDiseno));
          }

         // Ruta no encontrada - Error 404
        } else {

          respuestaHTML = construirRespuesta(NOTFOUND, PaginasHTML.html_notFound);
        }

        // Enviar respuesta al cliente
        salida.print(respuestaHTML);
        salida.flush();

      }

    } catch (Exception ex) {
      ex.printStackTrace();
    }

  }

  //***** METODO CONSTRUIR RESPUESTA HTTP ****
   /**
   * Construye una respuesta HTTP completa.
   * 
   * @param codigo Código de estado HTTP (200 o 404)
   * @param contenido Contenido HTML de la página
   * @return Respuesta HTTP formateada
   */
  public String construirRespuesta(int codigo, String contenido) {

    String respuesta;

    if (codigo == 200) {
      respuesta = "HTTP/1.1 200 OK" + "\n";
    } else {
      respuesta = "HTTP/1.1 404 NOT FOUND" + "\n";
    }

    respuesta += "Content-Type: text/html; charset=UTF-8" + "\n"; // metadatos 
    respuesta += "Content-Length: " + contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + "\n"; 

    respuesta += "\n";

    respuesta += contenido;

    return respuesta;
  }

}
