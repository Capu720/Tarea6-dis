/*
  Servicio.java
  Servicio web tipo REST
  Recibe parámetros utilizando JSON
  Carlos Jesus Morales Hernandez
*/

package servicio_json;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.Response;

import java.sql.*;
import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import com.google.gson.*;

// la URL del servicio web es http://localhost:8080/Servicio/rest/ws
// donde:
//	"Servicio" es el dominio del servicio web (es decir, el nombre de archivo Servicio.war)
//	"rest" se define en la etiqueta <url-pattern> de <servlet-mapping> en el archivo WEB-INF\web.xml
//	"ws" se define en la siguiente anotacin @Path de la clase Servicio

@Path("ws")
public class Servicio
{
  static DataSource pool = null;
  static
  {		
    try
    {
      Context ctx = new InitialContext();
      pool = (DataSource)ctx.lookup("java:comp/env/jdbc/datasource_Servicio");
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  static Gson j = new GsonBuilder().registerTypeAdapter(byte[].class,new AdaptadorGsonBase64()).setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").create();

  @POST
  @Path("alta_articulo")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response alta(String json) throws Exception
  {
    ParamAltaArticulo p = (ParamAltaArticulo) j.fromJson(json,ParamAltaArticulo.class);
    Articulo articulo = p.articulo;

    Connection conexion = pool.getConnection();

    if (articulo.nombre == null || articulo.nombre.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el nombre"))).build();

    if (articulo.descripcion == null || articulo.descripcion.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar una descripcion"))).build();

    if (articulo.precio == null)
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el precio"))).build();

    try
    {
      conexion.setAutoCommit(false);

      PreparedStatement stmt_1 = conexion.prepareStatement("INSERT INTO articulos(id_articulo,nombre,descripcion,precio,cantidad) VALUES (0,?,?,?,?)");
 
      try
      {
        stmt_1.setString(1,articulo.nombre);
        stmt_1.setString(2,articulo.descripcion);
        stmt_1.setDouble(3,articulo.precio);
	stmt_1.setInt(4,articulo.cantidad);

        stmt_1.executeUpdate();
      }
      finally
      {
        stmt_1.close();
      }

      if (articulo.foto != null)
      {
        PreparedStatement stmt_2 = conexion.prepareStatement("INSERT INTO fotos_articulos(id_foto,foto,id_articulo) VALUES (0,?,(SELECT id_articulo FROM articulos WHERE nombre=?))");
        try
        {
          stmt_2.setBytes(1,articulo.foto);
          stmt_2.setString(2,articulo.nombre);
          stmt_2.executeUpdate();
        }
        finally
        {
          stmt_2.close();
        }
      }
      conexion.commit();
    }
    catch (Exception e)
    {
      conexion.rollback();
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    }
    finally
    {
      conexion.setAutoCommit(true);
      conexion.close();
    }
    return Response.ok().build();
  }

@POST
@Path("alta_carrito")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response altaCarrito(String json) throws Exception
{
  ParamAltaArticulo p = (ParamAltaArticulo) j.fromJson(json,ParamAltaArticulo.class);
  Articulo articulo = p.articulo;

  Connection conexion = pool.getConnection();

  try
  {
    conexion.setAutoCommit(false);

    PreparedStatement stmt_2 = conexion.prepareStatement("INSERT INTO carrito_compra(id_articulo, cantidad) VALUES (?,?);");
    
      try
      {
        stmt_2.setInt(1,articulo.id_articulo);
        stmt_2.setInt(2,articulo.cantidad);
        stmt_2.executeUpdate();
      }
      finally
      {
        stmt_2.close();
      }
    String cons_update = "UPDATE articulos SET cantidad = " +articulo.resta+ " WHERE id_articulo = " +articulo.id_articulo+ " ;";
    PreparedStatement stmt_3 = conexion.prepareStatement(cons_update);

    try
    {
       stmt_3.executeUpdate();
    }
    
    finally
    {
      stmt_3.close();
    }

    conexion.commit();
  }
  catch (Exception e)
  {
    conexion.rollback();
    return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
  }
  finally
  {
    conexion.setAutoCommit(true);
    conexion.close();
  }
  return Response.ok().build();
} 

  @POST
  @Path("compra_articulo")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response consulta(String json) throws Exception
  {
    ParamConsultaArticulo p = (ParamConsultaArticulo) j.fromJson(json,ParamConsultaArticulo.class);
    String nombre = p.nombre;
    String descripcion = p.descripcion;


    Connection conexion= pool.getConnection();

    try
    {
      String consulta = "SELECT a.nombre,a.descripcion,a.precio,a.cantidad,b.foto,a.id_articulo FROM articulos a LEFT OUTER JOIN fotos_articulos b ON a.id_articulo=b.id_articulo WHERE nombre LIKE '" + nombre + "%' or descripcion LIKE '" +descripcion+ "';";
      PreparedStatement stmt_1 = conexion.prepareStatement(consulta);
      try      
      {
        ResultSet rs = stmt_1.executeQuery();
        ArrayList <Articulo> lista_articulos = new ArrayList <Articulo>();
        try
        {
          while (rs.next())
          {
	    Articulo a = new Articulo();
            a.nombre = rs.getString(1);
            a.descripcion = rs.getString(2);
            a.precio = rs.getDouble(3);
            a.cantidad = rs.getInt(4);
	    a.foto = rs.getBytes(5);
	    a.id_articulo = rs.getInt(6);

	    lista_articulos.add(a);
            
          }
	  return Response.ok().entity(j.toJson(lista_articulos)).build();
          
        }
        finally
        {
          rs.close();
        }
      }
      finally
      {
        stmt_1.close();
      }
    }
    catch (Exception e)
    {
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    }
    finally
    {
      conexion.close();
    }
  }

@POST
@Path("ver_carrito")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response consultaCarrito(String json) throws Exception
{
  ParamConsultaArticulo p = (ParamConsultaArticulo) j.fromJson(json,ParamConsultaArticulo.class);
  String nombre = p.nombre;

  Connection conexion= pool.getConnection();

  try
  {
    PreparedStatement stmt_1 = conexion.prepareStatement("select t1.id_articulo, t1.cantidad, t2.nombre, t2.precio, t3.foto from carrito_compra t1 inner JOIN articulos t2 on t1.id_articulo = t2.id_articulo inner JOIN fotos_articulos t3 on t1.id_articulo = t3.id_articulo;");
    try
    {
      ResultSet rs = stmt_1.executeQuery();
      ArrayList <Articulo> lista_articulos = new ArrayList <Articulo>();
      try
      {
        while (rs.next())
        {
          Articulo a = new Articulo();
          a.id_articulo = rs.getInt(1);
          a.cantidad = rs.getInt(2);
          a.nombre = rs.getString(3);
          a.precio = rs.getDouble(4);
          a.resta = rs.getInt(2) * (int)rs.getDouble(4);
          a.foto = rs.getBytes(5);
	  lista_articulos.add(a);
        }
          return Response.ok().entity(j.toJson(lista_articulos)).build();
        
      }
      finally
      {
        rs.close();
      }
    }
    finally
    {
      stmt_1.close();
    }
  }
  catch (Exception e)
  {
    return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
  }
  finally
  {
    conexion.close();
  }
}

@POST
  @Path("eliminar_car_completo")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response borraCarroCompleto(String json) throws Exception
  {
    Connection conexion= pool.getConnection();
    conexion.setAutoCommit(false);	
    try
    {
      String comando = "update articulos inner join carrito_compra on carrito_compra.id_articulo = articulos.id_articulo set articulos.cantidad=articulos.cantidad + carrito_compra.cantidad where carrito_compra.id_articulo = articulos.id_articulo";
      PreparedStatement stmt_2 = conexion.prepareStatement(comando);
      try
      {
	      stmt_2.executeUpdate();
      }
      finally
      {
        stmt_2.close();
      }

      String comando1 = "delete from carrito_compra";
      PreparedStatement stmt_3 = conexion.prepareStatement(comando1);
      try
      {
	      stmt_3.executeUpdate();
      }
      finally
      {
        stmt_3.close();
      }
      conexion.commit();
    }
    catch (Exception e)
    {
      conexion.rollback();
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    }
    finally
    {
      conexion.setAutoCommit(true);
      conexion.close();
    }
    return Response.ok().build();
}

  @POST
  @Path("eliminar_articulo")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response borraArt(String json) throws Exception
  {
    ParamAltaArticulo p = (ParamAltaArticulo) j.fromJson(json,ParamAltaArticulo.class);
    Articulo articulo = p.articulo;
    
    Integer id = articulo.id_articulo;
    Integer cantidad = articulo.cantidad;

    Connection conexion= pool.getConnection();
    conexion.setAutoCommit(false);
    try
    {
      String comando = "delete from carrito_compra where id_articulo = " + id;
      PreparedStatement stmt_2 = conexion.prepareStatement(comando);
      try
      {
	      stmt_2.executeUpdate();
      }
      finally
      {
        stmt_2.close();
      }

      String comando1 = "UPDATE articulos SET cantidad = cantidad + "+cantidad+" WHERE id_articulo = "+id;
      PreparedStatement stmt_3 = conexion.prepareStatement(comando1);
      try
      {
              stmt_3.executeUpdate();
      }
      finally
      {
        stmt_3.close();
      }
	
      conexion.commit();

      
    }
    catch (Exception e)
    {
      conexion.rollback();
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    }
    finally
    {
      conexion.setAutoCommit(true);
      conexion.close();
    }
    return Response.ok().build();
}


  @POST
  @Path("modifica_usuario")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response modifica(String json) throws Exception
  {
    ParamModificaUsuario p = (ParamModificaUsuario) j.fromJson(json,ParamModificaUsuario.class);
    Usuario usuario = p.usuario;

    Connection conexion= pool.getConnection();

    if (usuario.email == null || usuario.email.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el email"))).build();

    if (usuario.nombre == null || usuario.nombre.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el nombre"))).build();

    if (usuario.apellido_paterno == null || usuario.apellido_paterno.equals(""))
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar el apellido paterno"))).build();

    if (usuario.fecha_nacimiento == null)
      return Response.status(400).entity(j.toJson(new Error("Se debe ingresar la fecha de nacimiento"))).build();

    conexion.setAutoCommit(false);
    try
    {
      PreparedStatement stmt_1 = conexion.prepareStatement("UPDATE usuarios SET nombre=?,apellido_paterno=?,apellido_materno=?,fecha_nacimiento=?,telefono=?,genero=? WHERE email=?");
      try
      {
        stmt_1.setString(1,usuario.nombre);
        stmt_1.setString(2,usuario.apellido_paterno);

        if (usuario.apellido_materno != null)
          stmt_1.setString(3,usuario.apellido_materno);
        else
          stmt_1.setNull(3,Types.VARCHAR);

        stmt_1.setTimestamp(4,usuario.fecha_nacimiento);

        if (usuario.telefono != null)
          stmt_1.setLong(5,usuario.telefono);
        else
          stmt_1.setNull(5,Types.BIGINT);

        stmt_1.setString(6,usuario.genero);
        stmt_1.setString(7,usuario.email);
        stmt_1.executeUpdate();
      }
      finally
      {
        stmt_1.close();
      }

      PreparedStatement stmt_2 = conexion.prepareStatement("DELETE FROM fotos_usuarios WHERE id_usuario=(SELECT id_usuario FROM usuarios WHERE email=?)");
      try
      {
        stmt_2.setString(1,usuario.email);
        stmt_2.executeUpdate();
      }
      finally
      {
        stmt_2.close();
      }

      if (usuario.foto != null)
      {
        PreparedStatement stmt_3 = conexion.prepareStatement("INSERT INTO fotos_usuarios(id_foto,foto,id_usuario) VALUES (0,?,(SELECT id_usuario FROM usuarios WHERE email=?))");
        try
        {
          stmt_3.setBytes(1,usuario.foto);
          stmt_3.setString(2,usuario.email);
          stmt_3.executeUpdate();
        }
        finally
        {
          stmt_3.close();
        }
      }
      conexion.commit();
    }
    catch (Exception e)
    {
      conexion.rollback();
      return Response.status(400).entity(j.toJson(new Error(e.getMessage()))).build();
    }
    finally
    {
      conexion.setAutoCommit(true);
      conexion.close();
    }
    return Response.ok().build();
  }
}  
