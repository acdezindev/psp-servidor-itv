package itv;


import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class PaginasHTML {
  
  // En esta pagina index , pasamos por parametro el panelHTML que sera una variable que recogemos para mostrar el panel inicial
    public static String htmlIndex(String panelHTML) {
        return  "<html><head>"
            + "<title>ITV del Infierno</title>"
            + "<meta charset='UTF-8'>"
            + "<meta http-equiv='refresh' content='2;url=/inicio'>" 
            + "<style>"
            + "body {"
            + "  font-family: Arial, sans-serif;"
            + "  background: linear-gradient(135deg, #000000, #440000);"
            + "  color: white;"
            + "  text-align: center;"
            + "  padding-bottom: 40px;"
            + "}"
            + ".boton {"
            + "  background: #ff3333;"
            + "  color: white;"
            + "  padding: 20px 40px;"
            + "  font-size: 22px;"
            + "  margin: 20px;"
            + "  border: none;"
            + "  border-radius: 10px;"
            + "  cursor: pointer;"
            + "  transition: 0.3s;"
            + "}"
            + ".boton:hover { background:#cc0000; transform: scale(1.05); }"

            + ".panel {"
            + "  background: black;"
            + "  width: 60%;"
            + "  margin: auto;"
            + "  padding: 25px;"
            + "  border: 8px solid #333;"
            + "  color: #00ff00;"
            + "  font-family: 'Courier New', monospace;"
            + "  box-shadow: 0 0 15px #ff000055;"
            + "}"
            + ".panel-titulo {"
            + "  text-align: center;"
            + "  font-size: 30px;"
            + "  font-weight: bold;"
            + "  color: #ff3333;"
            + "  border-bottom: 4px solid #ff3333;"
            + "  padding-bottom: 12px;"
            + "  margin-bottom: 20px;"
            + "  letter-spacing: 3px;"
            + "}"
            + ".encabezado, .linea {"
            + "  display: grid;"
            + "  grid-template-columns: 70% 30%;"
            + "  font-size: 22px;"
            + "  letter-spacing: 2px;"
            + "  padding: 6px 0;"
            + "}"
            + ".linea { font-size: 24px; }"
            + ".verde { color: #00ff00; }"
            + ".rojo { color: #ff3333; }"
            + "</style>"
            + "</head><body>"

            + "<h1 style='color:#ff5555;'>ITV del Infierno</h1>"

            + "<form action='/reservar' method='GET'>"
            + "<button class='boton'>Reservar Cita</button>"
            + "</form>"

            + "<form action='/pasar' method='GET'>"
            + "<button class='boton'>Pasar ITV</button>"
            + "</form>"

            + "<h2 style='color:#ff8888;'>Panel de Llamadas</h2>"

            + "<div class='panel'>"
            + "<div class='panel-titulo'>LÍNEAS DE INSPECCIÓN</div>"
            

            + panelHTML

            + "</div></body></html>";
    }


    public static String htmlReservar = "<html><head><meta charset='UTF-8'><title>Reservar Cita</title>"
            + "<style>"
            + "body { background: linear-gradient(135deg,#550000,#220000); "
            + "       color:white; font-family:Arial; text-align:center; padding-top:40px; }"
            + "input { padding:10px; font-size:18px; border-radius:8px; border:none; }"
            + "button { background:#ff4444; color:white; padding:12px 25px; "
            + "        border:none; border-radius:8px; font-size:18px; cursor:pointer; }"
            + "button:hover { background:#cc0000; }"
            + "a { color:#ffaaaa; font-size:20px; }"
            + "</style></head>"
            + "<body>"
            + "<h1>Reservar Cita ITV</h1>"
            + "<form action='/reservar' method='POST'>"
            + "Matrícula: <input type='text' name='matricula' required>"
            + "<br><br>"
            + "<button type='submit'>Confirmar</button>"
            + "</form>"
            + "<br><a href='/inicio'>Volver</a>" 
            + "</body></html>";



    public static String htmlResultado(String contenido) {
        return "<div style='width:100%; text-align:center;'>"
            + "<h1 style='font-size:28px; margin-bottom:20px; color:#ffdddd;'>Resultado de la Inspección</h1>"
            + "<div class='box' style='background:#550000; padding:15px; width:45%; margin:auto; border-radius:10px; box-shadow:0 0 10px #00000088; font-size:20px; color:white;'>"
            + contenido
            + "</div>"
            + "<div class='taller' style='margin-top:20px; background:#ffeecc; color:#442200; padding:12px; border-radius:10px; width:40%; margin-left:auto; margin-right:auto; box-shadow:0 0 8px #ffcc66; border:2px solid #ffbb55; font-family:\"Comic Sans MS\";'>"
            + "<h3 style='color:#aa5500; margin-bottom:4px; font-size:13px;'>🔧 Taller \"EL Oportuno\" 🔧</h3>"
            + "<p style='font-size:10px;'>¿No has pasado la ITV? Vaya, qué lástima…</p>"
            + "<p style='font-size:10px;'><b>Estamos justo al lado</b>, pura coincidencia</p>"
            + "<p style='font-size:10px;'><i>5% descuento con el código: <span style='font-size:3px;'>Deja de hacer zoom, y ponte con la tarea. Vas a suspender</span></i></p>"
            + "<small style='font-size:10px;'>(Si usa el código, se lo arreglará el becario)</small>"
            + "</div>"
            + "</div>";
    }



    
    public static String htmlPasarITV(String matriculaPrellenada, String resultadoFragmento) {
        String valor = (matriculaPrellenada != null) ? matriculaPrellenada : "";
        return "<html><head><meta charset='UTF-8'><title>Pasar ITV</title>"
            + "<style>"
            + "body { background: linear-gradient(135deg, #000000, #440000); color:white; font-family:Arial; text-align:center; padding-top:40px; }"
            + "h1 { color:#ff4444; text-shadow:0 0 10px #ff0000; }"
            + "input { padding:15px; font-size:24px; border-radius:10px; border:3px solid #ff0000; background:#330000; color:white; width:280px; text-align:center; }"
            + "button { background:#ff3333; color:white; padding:15px 35px; border:none; border-radius:10px; font-size:22px; cursor:pointer; transition:0.3s; }"
            + "button:hover { background:#cc0000; transform:scale(1.05); }"
            + "a { color:#ffaaaa; font-size:22px; text-decoration:none; }"
            + "a:hover { text-decoration:underline; }"
            + "</style></head><body>"

            + "<h1>Entrada a la ITV del Infierno</h1>"
            + "<form action='/pasar' method='POST'>"
            + "Matrícula:<br><br>"
            + "<input type='text' name='matricula' required value='" + valor + "'>"
            + "<br><br>"
            + "<button type='submit'>Entrar a línea</button>"
            + "</form>"

            + "<div class='resultado'>" + resultadoFragmento + "</div>"

            + "<br><a href='/inicio'>Volver</a>" 
            + "</body></html>";
    }

    // TAREA 4 LOGIN Y PASS
        public static String login(String msg) {
        return "<!DOCTYPE html>"
        + "<html lang='es'>"
        + "<head>"
        + "<link rel=icon href=data:,/>"                
        + "<meta charset='UTF-8'>"
        + "<title>Login</title>"
        + "<style>"
        + "body {"
        + "  font-family: Arial, sans-serif;"
        + "  background: linear-gradient(135deg, #74ebd5, #9face6);"
        + "  display: flex;"
        + "  justify-content: center;"
        + "  align-items: center;"
        + "  height: 100vh;"
        + "  margin: 0;"
        + "}"
        + ".container {"
        + "  background: white;"
        + "  padding: 40px;"
        + "  border-radius: 15px;"
        + "  box-shadow: 0 8px 16px rgba(0,0,0,0.2);"
        + "  width: 350px;"
        + "}"
        + "h2 {"
        + "  text-align: center;"
        + "}"
        + "input {"
        + "  width: 100%;"
        + "  padding: 10px;"
        + "  margin: 10px 0;"
        + "  border-radius: 8px;"
        + "  border: 1px solid #ccc;"
        + "}"
        + "button {"
        + "  width: 100%;"
        + "  padding: 12px;"
        + "  background-color: #4CAF50;"
        + "  color: white;"
        + "  border: none;"
        + "  border-radius: 8px;"
        + "  cursor: pointer;"
        + "  font-size: 16px;"
        + "}"
        + "button:hover {"
        + "  background-color: #45a049;"
        + "}"
        + ".msg {"
        + "  color: red;"
        + "  text-align: center;"
        + "}"
        + "</style>"
        + "</head>"
        + "<body>"
        + "<div class='container'>"

        + "<div class='msg'>" + msg + "</div>"

        + "<h2>Iniciar sesión</h2>"
        + "<form action='/inicio' method='post'>"
        + "<input name='email' placeholder='Correo electrónico' required>"
        + "<input name='password' type='password' placeholder='Contraseña' required>"
        + "<button>Entrar</button>"
        + "</form>"

        + "<h2>Registro</h2>"
        + "<form action='/registro' method='post'>"
        + "<input name='email' placeholder='Correo electrónico' required>"
        + "<input name='password' type='password' placeholder='Contrasenia' pattern='(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}'"
        + " title='Mi­nimo 6 caracteres, letras y números' minlength='6' required>"
        + "<button>Registrarse</button>"
        + "</form>"

        + "</div></body></html>";
    }
    
    
    
    
    
    
    
    public static final String html_notFound =
        "<html><head><title>Error 404</title><meta charset=UTF-8>"
        + "<link rel=icon href=data:,/>"
        + "<style>"
        + "body{font-family:Arial;background:linear-gradient(135deg,#ff9a9e,#fad0c4);"
        + "text-align:center;padding-top:60px;}"
        + "h1{color:#333;font-size:48px;margin-bottom:10px;}"
        + "p{font-size:20px;color:#555;}"
        + "a.button{display:inline-block;padding:12px 25px;background:#e74c3c;color:white;"
        + "text-decoration:none;border-radius:8px;font-size:18px;transition:0.3s;margin-top:20px;}"
        + "a.button:hover{background:#c0392b;}"
        + "</style></head>"
        + "<body>"
        + "<h1>Error 404</h1>"
        + "<p>La página que buscas no existe o no se encuentra disponible.</p>"
        + "<a class='button' href='/'>Volver al inicio</a>" // estaba la ruta /  >> cambiamos a /inicio >>>  ********** INTENTAR HACER LO DE LAS COOKIES!!!!! 
        + "</body></html>";
  
    
  
}