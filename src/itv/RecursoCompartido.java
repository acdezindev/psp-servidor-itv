package itv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Esta clase gestiona toda la lógica de negocio de la ITV. Aquí se manejan las
 * citas, las líneas de inspección y los usuarios. Es el "cerebro" de la
 * aplicación.
 *
 * @author AC.
 */
public class RecursoCompartido {

  // Almacena las citas: matrícula -> número de línea (0 = en espera)
  private static final ConcurrentHashMap<String, Integer> citas = new ConcurrentHashMap<>();
  private final int MAX_LINEAS = 4;

  // Control de líneas ocupadas en la ITV
  private final int LINEAS_TOTALES = 4;
  private int lineasOcupadas = 0;

  // Candados para evitar problemas con hilos simultáneos
  private final Object candadoITV = new Object();     // Para controlar el acceso a las líneas
  private final Object candadoUsuarios = new Object(); // Para controlar el registro de usuarios
  // Gestiona la concurrencia al fichero de usuarios
  private RecursoConcurrencia controlConcurrencia = new RecursoConcurrencia();

  /**
   * Genera el panel HTML con el estado actual de la ITV. Muestra qué citas
   * están pendientes y qué línea ocupa cada coche.
   *
   * @return HTML con el estado de líneas y citas pendientes
   */
  public String generarPanel() {
    StringBuilder sb = new StringBuilder();

    // Primero mostramos las citas pendientes (las que están en línea 0)
    sb.append("<div style='color:white; font-size:18px;'>");
    sb.append("<strong>CITAS</strong><br>");
    sb.append("-----------------------------<br>");

    for (Map.Entry<String, Integer> entry : citas.entrySet()) {
      if (entry.getValue() == 0) {   // solo citas pendientes
        sb.append(entry.getKey())
                .append("<br>");
      }
    }
    sb.append("<br><strong>LINEAS DE INSPECCIÓN</strong><br>")
            .append("-----------------------------<br><br>")
            .append("</div>");

    // Mostramos cada línea de inspección (1 a 4)
    for (int i = 1; i <= MAX_LINEAS; i++) {
      String matricula = "LIBRE";
      boolean libre = true;
      boolean encontrada = false;

      for (Map.Entry<String, Integer> entry : citas.entrySet()) {

        if (!encontrada && entry.getValue() == i) {
          matricula = entry.getKey();
          libre = false;
          encontrada = true;
        }
      }

      String color = libre ? "verde" : "rojo";

      sb.append("""
                <div class="linea">
                    <div class="%s">%s</div>
                    <div class="%s">%d</div>
                </div>
            """.formatted(color, matricula, color, i));
    }

    return sb.toString();
  }

  /**
   * Reserva una cita manualmente desde el formulario web. Primero comprueba si
   * la matrícula ya existe.
   *
   * @param matricula Matrícula a reservar
   * @return true si se reservó correctamente, false si ya existía
   */
  public boolean reservarCitaManual(String matricula) {
    boolean resultado = false;
    if (!citas.containsKey(matricula)) {
      citas.put(matricula, 0);
      System.out.println("Reserva manual: " + matricula);
      resultado = true;
    }
    return resultado;
  }

  /**
   * Carga 6 matrículas de ejemplo para probar la aplicación. Estas matrículas
   * se crean al pulsar el botón "Reservar Cita".
   */
  public void cargarMatriculasIniciales() {
    citas.put("1111AAA", 0);
    citas.put("2222BBB", 0);
    citas.put("3333CCC", 0);
    citas.put("4444DDD", 0);
    citas.put("5555EEE", 0);
    citas.put("6666FFF", 0);

    System.out.println("Debug sout >> 6 matriculas cargadas en el panel de citas de Luis");
  }

  /**
   * Genera 6 matrículas aleatorias con formato: 4 números + 3 consonantes.
   * Ejemplo: 1234ABC
   */
  public void cargarMatriculasAleatorias() {
    Random aleatorio = new Random();
    String consonantes = "BCDFGHJKLMNPQRSTVWXYZ"; // Sin vocales( las matriculas no llevan vocales ) 21 caractees

    // Generamos 4 números aleatorios
    for (int i = 0; i < 6; i++) {
      String matricula = "";
      // Generamos 3 consonantes aleatorias
      //numeros aleatorios (0-9)
      for (int j = 0; j < 4; j++) {
        int numAleatorio = aleatorio.nextInt(10);
        matricula += numAleatorio;
      }

      //consonantes aleatorias
      for (int j = 0; j < 3; j++) {
        // Generamos  un numero  aleatorio entre 0 y 20 (porque hay 21 consonantes)
        int indiceAleatorio = aleatorio.nextInt(consonantes.length());
        // Coge la consonante que esta en esa posicion,  que hemos guardado en indicieAleatorio por ejemplo la K,
        char letraAleatoria = consonantes.charAt(indiceAleatorio);
        // anadimos la matricula 
        matricula += letraAleatoria;
      }

      citas.put(matricula, 0); // anade la matricula generada (key)  con valor 0.
      System.out.println("Matricula generada: " + matricula);
    }
  }

  /**
   * Comprueba si una matrícula tiene cita en la ITV.
   *
   * @param matricula Matrícula a verificar
   * @return true si existe la reserva
   */
  public boolean existeReserva(String matricula) {
    return citas.containsKey(matricula);
  }

  /**
   * Asigna una línea de inspección a un coche. Si todas las líneas están
   * ocupadas, el coche espera.
   *
   * @param nombreMatricula Matrícula que entra a línea
   * @throws InterruptedException Si el hilo es interrumpido
   */
  public void ocuparLinea(String nombreMatricula) throws InterruptedException {
    synchronized (candadoITV) {
      // Si no hay líneas libres, esperamos
      while (lineasOcupadas == LINEAS_TOTALES) {
        System.out.println(nombreMatricula + " espera, no hay lineas disponibles");
         candadoITV.wait();  // CORREGIDO: wait() del candadowait(); // el hilo espera hasta que alguien libere
      }

      // Buscamos la primera línea libre (1 a 4)
      boolean banderaLinea = false;
      for (int i = 1; i <= MAX_LINEAS && !banderaLinea; i++) {

        if (!citas.containsValue(i)) {
          citas.put(nombreMatricula, i);  // Asignamos la línea
          lineasOcupadas++;
          System.out.println(nombreMatricula + " Debug sout >> entra en linea " + i + " - Lineas Ocupadas: " + lineasOcupadas);
          banderaLinea = true;
        }
      }
    }
  }

  /**
   * Libera una línea de inspección cuando el coche termina. Avisa a los coches
   * que están esperando.
   *
   * @param nombreMatricula Matrícula que libera la línea
   * @throws InterruptedException Si el hilo es interrumpido
   */
  public void vaciarLinea(String nombreMatricula) throws InterruptedException {
    synchronized (candadoITV) {

      citas.remove(nombreMatricula);
      lineasOcupadas--;
      System.out.println("Debug " + nombreMatricula + " libera la linea - Lineas Ocupadas: " + lineasOcupadas);
      candadoITV.notifyAll(); // CORREGIDO: notifyAll() del candado;  // Despertar a los que esperan
    }
  }

  /**
   * Simula la inspección de la ITV con 5 pruebas. Cada prueba tarda entre 1 y
   * 10 segundos aleatorios.
   *
   * @param nombreMatricula Matrícula a inspeccionar
   * @param respuestaFrasesCliente Frases que "dice" el cliente
   * @return Resultado detallado de la inspección
   * @throws InterruptedException Si el hilo es interrumpido
   */
  public String realizarITV(String nombreMatricula, String[] respuestaFrasesCliente) throws InterruptedException {

    double probabilidadActual = 0.6; // probabilidad de pasar itv
    boolean itvAprobada = true;
    StringBuilder detallesPruebas = new StringBuilder();

    // Mostramos en qué línea está el coche
    Integer linea = citas.get(nombreMatricula);
    detallesPruebas.append(nombreMatricula)
            .append(" en linea ")
            .append(linea)
            .append("<br>")
            .append("<br>");

    // Las 5 pruebas de la ITV
    String[] pruebas = {"Luces", "Frenos", "Emisiones", "Dirección", "Suspensión"};
    for (int i = 0; i < pruebas.length; i++) {

      String fraseCliente = respuestaFrasesCliente[i];
      String resultado = fraseCunadoNoPasa(fraseCliente);
      // Si el cliente es "cuñado", la prueba falla
      if (resultado.equals("NO")) {
        probabilidadActual -= 0.1;
        if (probabilidadActual < 0) {
          probabilidadActual = 0;
        }
        itvAprobada = false;
      }

      // Guardamos el resultado de cada prueba
      detallesPruebas.append(pruebas[i]).append(" ")
              .append(resultado).append(" (\"")
              .append(fraseCliente).append("\") - Prob: ")
              .append((int) (probabilidadActual * 100))
              .append("%<br>");
      // Simulamos el tiempo que tarda cada prueba (1-10 segundos)
      Random random = new Random();
      int numAleatorio = 1 + random.nextInt(10);
      Thread.sleep(numAleatorio * 1000);

    }

    // Resultado final
    detallesPruebas.append("-----------<br>");

    if (itvAprobada) {
      detallesPruebas.append("ITV SUPERADA");
    } else {
      detallesPruebas.append("ITV NO SUPERADA");
    }

    return detallesPruebas.toString();
  }

  /**
   * Comprueba si la frase del cliente es de "cuñado". Si es de cuñado, la
   * prueba falla automáticamente.
   *
   * @param fraseCliente Frase que dice el cliente
   * @return "NO" si es de cuñado, "SI" si es normal
   */
  public String fraseCunadoNoPasa(String fraseCliente) {
    String resultado;
    if (fraseCliente.equals("ok jefe") || fraseCliente.equals("lo que tú digas")
            || fraseCliente.equals("a mandar") || fraseCliente.equals("como usted mande")
            || fraseCliente.equals("vamos al lio") || fraseCliente.equals("marchando")
            || fraseCliente.equals("manda usted") || fraseCliente.equals("perfecto maquina")
            || fraseCliente.equals("de lujo")) {
      resultado = "NO";  // Es cuñado, no pasa
    } else {
      resultado = "SI";  // Es normal, puede pasar
    }
    return resultado;
  }

  /**
   * Genera una frase aleatoria para simular respuestas del cliente. 70% frases
   * normales, 30% frases de cuñado.
   *
   * @return Frase seleccionada aleatoriamente
   */
  public String fraseSeleccionada() {
    Random aleatorio = new Random();

    // Frases de cuñado (30% de probabilidad)
    String[] fraseCunado30 = {
      "ok jefe", "lo que tu digas", "a mandar", "como usted mande",
      "vamos al lio", "marchando", "manda usted",
      "perfecto maquina", "de lujo"
    };

    // Frases normales (70% de probabilidad)
    String[] fraseNormal70 = {
      "vale", "recibido", "entendido", "procedo", "hecho",
      "si", "correcto", "ok", "de acuerdo"
    };

    double numeroAleatorio = Math.random();

    if (numeroAleatorio < 0.7) {
      int indiceAleatorio = aleatorio.nextInt(fraseNormal70.length);
      return fraseNormal70[indiceAleatorio];
    } else {
      int indiceAleatorio = aleatorio.nextInt(fraseCunado30.length);
      return fraseCunado30[indiceAleatorio];
    }
  }

  /**
   * Registra un nuevo usuario en el sistema. La contraseña se cifra con BCrypt
   * y se guarda en un fichero cifrado con AES.
   *
   * @param email Email del usuario
   * @param password Contraseña en texto plano
   * @return true si se registró correctamente
   */
  public boolean registrarUsuario(String email, String password) {
    synchronized (candadoUsuarios) {
      try {
        // Control de acceso concurrente al fichero
        controlConcurrencia.escribirFichero();
        // Ciframos la contraseña con BCrypt
        String hashPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

        String linea = email + ":" + hashPassword + "\n";
        // Si ya existe el fichero, lo desciframos para añadir el nuevo usuario
        File fichero = new File("usuarios.txt");
        String contenidoDescifrado = "";

        if (fichero.exists()) {
          contenidoDescifrado = descifrarAES("usuarios.txt");
        }
        // Añadimos el nuevo usuario y ciframos todo de nuevo
        String contenidoActualizado = contenidoDescifrado + linea;

        cifrarAES(contenidoActualizado, "usuarios.txt");

        // Liberamos el control de acceso
        controlConcurrencia.terminarEscribir();

        return true;
      } catch (Exception e) {
        return false;
      }
    }
  }

  /**
   * Cifra un texto usando AES-128. La clave es fija para simplificar (en
   * producción se usaría una clave segura).
   *
   * @param texto Texto a cifrar
   * @param rutaArchivo Ruta donde guardar el fichero cifrado
   * @throws Exception Si ocurre un error durante el cifrado
   */
  public void cifrarAES(String texto, String rutaArchivo) throws Exception {

    Cipher cipher = Cipher.getInstance("AES");

    SecretKey clave = new SecretKeySpec("1234567890123456".getBytes(), "AES");  // Clave de 16 bytes

    cipher.init(Cipher.ENCRYPT_MODE, clave);

    byte[] cifrado = cipher.doFinal(texto.getBytes());

    try ( FileOutputStream fos = new FileOutputStream(rutaArchivo)) {
      fos.write(cifrado);
    }

  }

  /**
   * Descifra un fichero cifrado con AES-128.
   *
   * @param rutaArchivo Ruta del fichero a descifrar
   * @return Contenido descifrado como String
   * @throws Exception Si ocurre un error durante el descifrado
   */
  public String descifrarAES(String rutaArchivo) throws Exception {

    Cipher cipher = Cipher.getInstance("AES");

    SecretKey clave = new SecretKeySpec("1234567890123456".getBytes(), "AES");

    cipher.init(Cipher.DECRYPT_MODE, clave);

    byte[] cifrado = Files.readAllBytes(Paths.get(rutaArchivo));
    byte[] descifrado = cipher.doFinal(cifrado);

    return new String(descifrado);
  }

  /**
   * Autentica a un usuario comprobando email y contraseña. Lee el fichero de
   * usuarios y verifica con BCrypt.
   *
   * @param emailParam Email del usuario
   * @param passwordParam Contraseña a verificar
   * @return true si las credenciales son correctas
   * @throws InterruptedException Si el hilo es interrumpido
   */
  public boolean autenticarUsuario(String emailParam, String passwordParam) throws InterruptedException {
    // Control de acceso concurrente al fichero (solo lectura)
    controlConcurrencia.leerFichero();

    boolean resultado = false;
    try {
      // Desciframos el fichero y leemos línea por línea
      String contenidoFichero = descifrarAES("usuarios.txt");
      String[] lineas = contenidoFichero.split("\n");
      for (int i = 0; i < lineas.length && !resultado; i++) {
        String lineaIndividual = lineas[i];

        String[] parteLinea = lineaIndividual.split(":");
        String emailGuardado = parteLinea[0];
        String hashGuardado = parteLinea[1];
        // Si el email coincide, verificamos la contraseña con BCrypt
        if (emailGuardado.equals(emailParam)) {

          resultado = BCrypt.checkpw(passwordParam, hashGuardado);
        }
        // Liberamos el control de acceso
        controlConcurrencia.terminarLeer();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return resultado;
  }

}
