package itv;

/**
 * Esta clase controla el acceso concurrente al fichero de usuarios. Implementa
 * el patrón de Lectores-Escritores para que: - Varios hilos puedan leer a la
 * vez (autenticar usuarios) - Solo un hilo pueda escribir (registrar usuarios)
 *
 * Esto evita que se corrompa el fichero cuando varios usuarios intentan
 * registrarse o autenticarse al mismo tiempo.
 *
 * @author AC.
 */
public class RecursoConcurrencia {
  // Contadores para controlar el acceso

  private int lectores = 0; // Cuántos hilos están leyendo
  private int escritores = 0;     // Cuántos hilos están escribiendo
  private int escritoresEsperando = 0;  // Cuántos escritores están en cola

  /**
   * Un hilo llama a este método cuando quiere leer el fichero. Solo puede leer
   * si no hay escritores activos. Varios lectores pueden leer a la vez.
   *
   * @throws InterruptedException Si el hilo es interrumpido
   */
  public synchronized void leerFichero() throws InterruptedException {
    // Si hay escritores o más de un escritor esperando, el lector espera
    while (escritores > 0 || escritoresEsperando > 1) {
      System.out.println(Thread.currentThread().getName() + ": Hay alguien usando el fichero. Espere");
      wait();
    }
    lectores++;
    System.out.println(Thread.currentThread().getName() + ": Leyendo fichero. Lectores: " + lectores);
  }

  /**
   * Un hilo llama a este método cuando termina de leer. Si es el último lector,
   * avisa a los que esperan.
   *
   * @throws InterruptedException Si el hilo es interrumpido
   */
  public synchronized void terminarLeer() throws InterruptedException {
    lectores--;
    System.out.println(Thread.currentThread().getName() + ": he terminado de leer. Lectores: " + lectores);
    // Si soy el último lector, despierto a los escritores que esperan
    if (lectores == 0) {
      notifyAll();
    }
  }

  /**
   * Un hilo llama a este método cuando quiere escribir en el fichero. Solo
   * puede escribir si no hay lectores ni otros escritores. Solo un escritor a
   * la vez.
   *
   * @throws InterruptedException Si el hilo es interrumpido
   */
  public synchronized void escribirFichero() throws InterruptedException {
    // Si hay lectores o escritores, esperamos
    while (lectores > 0 || escritores > 0) {
      System.out.println(Thread.currentThread().getName() + ": Hay alguien usando el fichero. Espere");
      escritoresEsperando++;
      wait();
      escritoresEsperando--;
    }
    escritores++;
    System.out.println(Thread.currentThread().getName() + ": Escribiendo en fichero. Escritores: " + escritores);
  }

  /**
   * Un hilo llama a este método cuando termina de escribir. Avisa a todos los
   * hilos que están esperando.
   *
   * @throws InterruptedException Si el hilo es interrumpido
   */
  public synchronized void terminarEscribir() throws InterruptedException {
    escritores--;
    System.out.println(Thread.currentThread().getName() + ": he terminado de Escribir. Escritores:" + escritores);
    notifyAll(); // Despertamos a todos los hilos en espera
  }

}
