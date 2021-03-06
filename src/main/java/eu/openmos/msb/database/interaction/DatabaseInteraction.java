/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.openmos.msb.database.interaction;

import static eu.openmos.msb.datastructures.MSBConstants.DATABASE_DRIVER_CLASS;
import static eu.openmos.msb.datastructures.MSBConstants.DB_MEMORY_CONNECTION;
import static eu.openmos.msb.datastructures.MSBConstants.DB_SQL_PATH;
import eu.openmos.msb.datastructures.PerformanceMasurement;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.time.StopWatch;

/**
 *
 * @author Fabio Miranda (1st)
 * @author af-silva (2nd)
 */
public class DatabaseInteraction
{

  private static final Object lock = new Object();
  private static volatile DatabaseInteraction instance;
  private Connection conn = null;
  private PreparedStatement ps = null;
  private final Set<String> trueSet = new HashSet<>(Arrays.asList("1", "true", "True"));
  //private final StopWatch DBqueryTimer = new StopWatch();
  
  public static Semaphore write_stuff = new Semaphore(1);
  
  /**
   * @brief private constructor from the singleton implementation
   */
  private DatabaseInteraction()
  {
    createInMemDatabase();

  }

  /**
   *
   * @return
   */
  public static DatabaseInteraction getInstance()
  {
    DatabaseInteraction i = instance;
    if (i == null)
    {
      synchronized (lock)
      {
        // While we were waiting for the lock, another 
        i = instance; // thread may have instantiated the object.
        if (i == null)
        {
          i = new DatabaseInteraction();
          instance = i;
        }
      }
    }
    return i;
  }

  /**
   *
   * @return
   */
  private boolean createInMemDatabase()
  {
    boolean dbCreated = false;
    try
    {
      Class.forName(DATABASE_DRIVER_CLASS);

      conn = DriverManager.getConnection(DB_MEMORY_CONNECTION);

      StringBuilder sb = new StringBuilder();
      try
      {
        System.out.println("caminho da bd  " + DB_SQL_PATH);
        FileReader file = new FileReader(new File(DB_SQL_PATH));
        try (BufferedReader fileReader = new BufferedReader(file))
        {
          String command = new String();
          while ((command = fileReader.readLine()) != null)
          {
            sb.append(command);
          }
        } catch (FileNotFoundException ex)
        {
          System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
          Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
          return dbCreated;
        }
      } catch (FileNotFoundException ex)
      {
        System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
        Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
        return dbCreated;
      } catch (IOException ex)
      {
        System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
        Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
        return dbCreated;
      }
      // each command is split by a ";"
      String[] commands = sb.toString().split(";");
      Statement st = conn.createStatement();
      for (String command : commands)
      {
        // avoid empty statements
        if (!command.trim().equals(""))
        {
          st.executeUpdate(command);
          System.out.println(">>" + command);
        }
      }
      st.closeOnCompletion();
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.INFO, null, "Opened database successfully.");
      System.out.println("Opened database successfully.");
      dbCreated = true;
    } catch (ClassNotFoundException | SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
      return dbCreated;
    }
    return dbCreated;
  }

  /**
   *
   * @return
   */
  public boolean resetDB()
  {
    return createInMemDatabase();
  }

  /**
   *
   * @param device_name
   * @param protocol
   * @param short_descriptor
   * @param long_descriptor
   * @return
   */
  public int createDeviceAdapter(String device_name, String protocol, String short_descriptor, String long_descriptor)
  {
    StopWatch DBqueryTimer = new StopWatch();
    DBqueryTimer.start();

    int ok_id = -1;
    try
    {
      Statement stmt = conn.createStatement();
      stmt.execute("INSERT INTO DeviceAdapter"
              + "(name, short_description, long_description, protocol)"
              + " VALUES ("
              + "'" + device_name + "','" + short_descriptor + "','" + long_descriptor + "','" + protocol + "')");
      ResultSet r = stmt.getGeneratedKeys();
      ok_id = r.getInt(1);
      stmt.close();

    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    DBqueryTimer.stop();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);

    return ok_id;
  }

  /**
   *
   * @return
   */
  public ArrayList<String> listAllDeviceAdapters()
  {
    StopWatch DBqueryTimer = new StopWatch();
    DBqueryTimer.start();

    ArrayList<String> list = new ArrayList<>();
    try
    {
      Statement stmt = conn.createStatement();
      String sql = "SELECT id, name FROM DeviceAdapter;";
      ResultSet rs = stmt.executeQuery(sql);
      if (!rs.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (rs.next())
        {
          list.add(rs.getString(2));
        }
      }
      stmt.close();
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    DBqueryTimer.stop();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);

    return list;
  }

  /**
   * @param protocol
   * @return
   */
  public ArrayList<String> listDevicesByProtocol(String protocol)
  {
    StopWatch DBqueryTimer = new StopWatch();
    DBqueryTimer.start();

    ArrayList<String> list = new ArrayList<>();
    try
    {
      Statement stmt = conn.createStatement();
      String sql = "SELECT name FROM DeviceAdapter WHERE protocol = '" + protocol + "';";
      ResultSet rs = stmt.executeQuery(sql);
      if (!rs.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (rs.next())
        {
          list.add(rs.getString(1));
        }
      }
      rs.close();
      stmt.close();
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    DBqueryTimer.stop();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);

    return list;
  }

  /**
   *
   * @param deviceName
   * @return
   */
  public ArrayList<String> readDeviceInfoByName(String deviceName)
  {
    StopWatch DBqueryTimer = new StopWatch();
    DBqueryTimer.start();

    ArrayList<String> list = new ArrayList<>();
    try
    {
      Statement stmt = conn.createStatement();
      String sql = "SELECT id, short_description, long_description, protocol FROM DeviceAdapter WHERE name = '" + deviceName + "'";
      ResultSet rs = stmt.executeQuery(sql);
      if (!rs.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (rs.next())
        {
          list.add(rs.getString(1));
          list.add(rs.getString(2));
          list.add(rs.getString(3));
          list.add(rs.getString(4));
          list.add(rs.getString(5));
          list.add(rs.getString(6));
        }
      }
      rs.close();
      stmt.close();
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    DBqueryTimer.stop();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);

    return list;
  }

  /**
   *
   * @param deviceName
   * @return
   */
  public boolean removeDeviceAdapterByName(String deviceName)
  {
    try
    {
      Statement stmt = conn.createStatement();
      int query = stmt.executeUpdate("DELETE FROM DeviceAdapter WHERE name = '" + deviceName + "'");
      stmt.close();

      return true;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    return false;
  }

  /**
   *
   * @param deviceId
   * @return
   */
  public boolean removeDeviceAdapterById(String deviceId)
  {
    try
    {
      Statement stmt = conn.createStatement();
      stmt.executeUpdate("DELETE FROM DeviceAdapter WHERE id = '" + deviceId + "'");
      stmt.close();
      return true;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    return false;
  }

  /**
   *
   * @param deviceId
   * @return
   */
  public ArrayList<String> getDeviceAdapterAddressProtocolById(String deviceId)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    try
    {
      Statement stmt = conn.createStatement();
      ArrayList<String> myresult = new ArrayList<>();;
      try (ResultSet query = stmt.executeQuery("SELECT address, protocol FROM DeviceAdapter WHERE id = '" + deviceId + "'"))
      {
        if (!query.isBeforeFirst())
        {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
          //System.out.println("No data");
        } else
        {
          while (query.next())
          {
            myresult.add(query.getString(1));
            myresult.add(query.getString(2));
          }
        }
      }
      stmt.close();

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();

      return myresult;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return null;

  }

  /**
   *
   * @param address
   * @return
   */
  public String getDeviceAdapterName(String address)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();
    try
    {
      String name = "";
      Statement stmt = conn.createStatement();
      ResultSet query = stmt.executeQuery("SELECT name FROM DeviceAdapter WHERE address = '" + address + "'");
      query.close();
      if (!query.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        name = query.getString(1);
      }
      stmt.close();

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();

      return name;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return null;
  }

  public ArrayList<String> getDeviceAdapters_name()
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();
    try
    {
      Statement stmt = conn.createStatement();
      ArrayList<String> myresult = new ArrayList<>();
      try (ResultSet query = stmt.executeQuery("SELECT name FROM DeviceAdapter"))
      {
        if (!query.isBeforeFirst())
        {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
          //System.out.println("No data");
        } else
        {
          while (query.next())
          {
            if (query.getString(1).equals("MSB Milo server"))
            {
              continue;
            }
            myresult.add(query.getString(1));
          }
        }
      }
      stmt.close();

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();

      return myresult;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return null;
  }

  public ArrayList<String> getDeviceAdapters_AML_ID()
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();
    try
    {
      Statement stmt = conn.createStatement();
      ArrayList<String> myresult = new ArrayList<>();
      try (ResultSet query = stmt.executeQuery("SELECT aml_id FROM DeviceAdapter"))
      {
        if (!query.isBeforeFirst())
        {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
          //System.out.println("No data");
        } else
        {
          while (query.next())
          {
            if (query.getString("aml_id") == null)
            {
              continue;
            }
            myresult.add(query.getString("aml_id"));
          }
        }
      }
      stmt.close();

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();

      return myresult;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return null;
  }

  /**
   *
   * @param name
   * @param protocol
   * @param short_descriptor
   * @param long_descriptor
   * @param client_id
   * @param agent_id
   * @return
   */
  public boolean updateDeviceAdapter(String name, String protocol, String short_descriptor, String long_descriptor, String client_id, String agent_id)
  {
    try
    {
      ps = conn.prepareStatement("UPDATE DeviceAdapter "
              + "SET short_description = ?, long_description = ?, protocol = ?, client_id = ?, agent_id = ?"
              + "WHERE name = ?");
      ps.setString(1, short_descriptor);
      ps.setString(2, long_descriptor);
      ps.setString(3, protocol);
      ps.setString(4, client_id);
      ps.setString(4, agent_id);
      ps.setString(5, name);
      ps.executeUpdate();
      ps.close();

      return true;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    return false;
  }

  /**
   *
   * @param deviceName
   * @return
   */
  public int getDeviceAdapterDB_ID_ByName(String deviceName)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    int id = -1;
    String sql = "SELECT id FROM DeviceAdapter WHERE name = '" + deviceName + "'";

    try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql))
    {
      if (!rs.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (rs.next())
        {
          id = rs.getInt("id");
          //System.out.println("Found divce with id:  " + id);
          break;
        }
      }
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return id;
  }

  public int getDeviceAdapterDB_ID_ByAML_ID(String da_aml_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    int id = -1;
    String sql = "SELECT id FROM DeviceAdapter WHERE aml_id = '" + da_aml_id + "'";

    try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql))
    {
      if (!rs.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (rs.next())
        {
          id = rs.getInt("id");
          //System.out.println("Found divce with id:  " + id);
          break;
        }
      }
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return id;
  }

  public List<String> getModulesAML_ID_ByDA_DB_ID(String da_db_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    List<String> ids = new ArrayList<>();
    String sql = "SELECT Modules.aml_id FROM Modules WHERE da_id = '" + da_db_id + "'";

    try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql))
    {
      if (!rs.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (rs.next())
        {
          ids.add(rs.getString("aml_id"));
          //System.out.println("Found divce with id:  " + id);
          //break;
        }
      }
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return ids;
  }

  public String getDeviceAdapterNameByDB_ID(String deviceID)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    String name = "";
    String sql = "SELECT name FROM DeviceAdapter WHERE id = '" + deviceID + "'";

    try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql))
    {
      if (!rs.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (rs.next())
        {
          name = rs.getString("name");
          //System.out.println("Found device with name:  " + name);
          break;
        }
      }
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return name;
  }

  public String getDeviceAdapterNameByAmlID(String deviceAMLID)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    String name = "";
    String sql = "SELECT name FROM DeviceAdapter WHERE aml_id = '" + deviceAMLID + "'";

    try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql))
    {
      if (!rs.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (rs.next())
        {
          name = rs.getString("name");
          //System.out.println("Found device with name:  " + name);
          break;
        }
      }
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return name;
  }

  public int getDeviceAdapter_DB_ID_byModuleID(String module_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    int id = 0;
    String sql = "SELECT Modules.da_id FROM Modules WHERE Modules.aml_id = '" + module_id + "'";

    try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql))
    {
      if (!rs.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (rs.next())
        {
          id = rs.getInt("da_id");
          //System.out.println("Found device with name:  " + name);
          break;
        }
      }
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return id;
  }

  /**
   *
   * @param skillName
   * @return
   */
  public int getSkillIdByName(String skillName)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    int id = -1;
    String sql = "SELECT id FROM Skill WHERE name = '" + skillName + "'";

    try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql))
    {
      if (!rs.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (rs.next())
        {
          id = rs.getInt("id");
          //System.out.println("Found skill with id:  " + id);
          break;
        }
      }
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return id;
  }

  // **************************************************************************************************************** //
  // **************************************************************************************************************** //
  /**
   *
   * @param device_name
   * @param aml_id
   * @param skill_name
   * @param description
   * @return
   */
  public boolean registerSkill(String device_name, String aml_id, String skill_name, String description)
  {
    int device_id = getDeviceAdapterDB_ID_ByName(device_name);
    int skill_id;
    if (device_id == -1)
    {
      return false;
    }

    try
    {
      skill_id = getSkill(aml_id);
      Statement stmt = conn.createStatement();
      {
        if (skill_id == -1)
        {
          String sql = "INSERT INTO Skill"
                  + "(aml_id, name, description)"
                  + " VALUES ("
                  + "'" + aml_id + "','" + skill_name + "','" + description + "')";
          stmt.execute(sql);
          ResultSet r = stmt.getGeneratedKeys();
          skill_id = r.getInt(1);
        }
      }
      {
        if (skill_id != -1)
        {
          String sql = "INSERT INTO DAS"
                  + "(sk_id, da_id)"
                  + " VALUES ("
                  + "'" + skill_id + "','" + device_id + "')";
          stmt.execute(sql);
        }
      }

      stmt.close();
      System.out.println("REGISTER SKILL  " + skill_name + " " + device_name + " " + aml_id + " " + description);
      return true;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    return false;
  }

  public int getSkill(String skill_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    int id = -1;
    String sql = "SELECT Skill.id FROM Skill WHERE Skill.aml_id = '" + skill_id + "'";

    try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql))
    {
      if (!rs.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (rs.next())
        {
          id = rs.getInt("id");
          //System.out.println("Found skill with id:  " + id);
          break;
        }
      }
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return id;
  }

  /**
   *
   * @param skill_name
   * @return
   */
  public int removeSkillByName(String skill_name)
  {
    try
    {
      Statement stmt = conn.createStatement();
      int query = stmt.executeUpdate("DELETE FROM Skill WHERE name = '" + skill_name + "'");
      stmt.close();

      return query;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    return -1;
  }

  public int removeSkillByID(String skill_id)
  {
    try
    {
      Statement stmt = conn.createStatement();
      int query = stmt.executeUpdate("DELETE FROM Skill WHERE id = '" + skill_id + "'");
      stmt.close();

      return query;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    return -1;
  }

  /**
   *
   * @param device_name
   * @return
   */
  public ArrayList<String> getSkillsByDeviceAdapter(String device_name)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    try
    {
      ArrayList<String> myresult;

      Statement stmt = conn.createStatement();
      String sql = "SELECT DeviceAdapter.id, DeviceAdapter.name, Skill.name "
              + "FROM Skill, DeviceAdapter, DAS "
              + "WHERE DeviceAdapter.id = DAS.da_id AND Skill.id = DAS.sk_id AND DeviceAdapter.name = '" + device_name + "';";
      ResultSet query = stmt.executeQuery(sql);
      myresult = new ArrayList<>();
      if (!query.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (query.next())
        {
          myresult.add(query.getString(1));
          myresult.add(query.getString(2));
          myresult.add(query.getString(3));
        }
      }
      stmt.close();

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();

      return myresult;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return null;
  }

  /**
   *
   * @param id
   * @return
   */
  public String getSkillNameById(String id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    try
    {
      String name = "";
      Statement stmt = conn.createStatement();
      String sql = "SELECT Skill.name "
              + "FROM Skill "
              + "WHERE Skill.id = '" + id + "';";
      ResultSet rs = stmt.executeQuery(sql);
      if (!rs.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        name = rs.getString(1);
      }
      stmt.close();

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();

      return name;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return null;
  }

  public boolean getRecipeIdIsValid(String r_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    try
    {
      //String temp= "'"+r_id+"'";
      Statement stmt = conn.createStatement();
      String sql = "SELECT Recipe.valid "
              + "FROM Recipe "
              + "WHERE Recipe.aml_id = '" + r_id + "';";
      ResultSet rs = stmt.executeQuery(sql);

      //int numberOfRows = rs.getRow();
      //System.out.println("ResultSet number of rows: "+numberOfRows);
      if (!rs.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {

        String valid = rs.getString("valid");
        stmt.close();

        Long time = DBqueryTimer.getTime();
        perfMeasure.getDatabaseQueryTimers().add(time);
        DBqueryTimer.stop();

        return valid.equals("true");
      }
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return false;
  }

  public boolean skillExists(String aml_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    try
    {
      Statement stmt = conn.createStatement();
      String sql = "SELECT Skill.aml_id "
              + "FROM Skill "
              + "WHERE Skill.aml_id = '" + aml_id + "';";
      ResultSet rs = stmt.executeQuery(sql);

      if (!rs.isBeforeFirst())
      {
        stmt.close();
        Long time = DBqueryTimer.getTime();
        perfMeasure.getDatabaseQueryTimers().add(time);
        DBqueryTimer.stop();
        return false;
      } else
      {
        stmt.close();
        Long time = DBqueryTimer.getTime();
        perfMeasure.getDatabaseQueryTimers().add(time);
        DBqueryTimer.stop();
        return true;
      }
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return false;
  }

  // **************************************************************************************************************** //
  // **************************************************************************************************************** //
  /**
   *
   * @param da_id
   * @param aml_id
   * @param sk_id
   * @param valid
   * @param name
   * @param object_id
   * @param method_id
   * @return
   */
  public boolean registerRecipe(String aml_id, int da_id, int sk_id, boolean valid, String name, String object_id, String method_id)
  {
    try
    {
      Statement stmt = conn.createStatement();
      String sql = "INSERT INTO Recipe (aml_id, da_id, sk_id, valid, name, obj_id, method_id)\n"
              + "VALUES('" + aml_id + "','" + Integer.toString(da_id) + "','" + Integer.toString(sk_id)
              + "','" + Boolean.toString(valid) + "','" + name + "','" + object_id + "','" + method_id + "');";
      stmt.execute(sql);
      stmt.close();
      //System.out.println("NEW RECIPE: " + name + " " + aml_id + " " + da_id + " " + sk_id + " " + valid + " " + object_id + " " + method_id);
      return true;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    return false;
  }

  /**
   *
   * @param recipe_name
   * @return
   */
  public int removeRecipeByName(String recipe_name)
  {
    try
    {
      Statement stmt = conn.createStatement();
      int query = stmt.executeUpdate("DELETE FROM Recipe WHERE name = '" + recipe_name + "'");
      stmt.close();

      return query;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    return -1;
  }

  /**
   *
   * @param recipe_id
   * @return
   */
  public int removeRecipeById(String recipe_id)
  {
    try
    {
      Statement stmt = conn.createStatement();
      int query = stmt.executeUpdate("DELETE FROM Recipe WHERE aml_id = '" + recipe_id + "'");
      stmt.close();

      return query;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    return -1;
  }

  public int removeRecipeByDaId(int da_id)
  {
    try
    {
      Statement stmt = conn.createStatement();
      int query = stmt.executeUpdate("DELETE FROM Recipe WHERE da_id = '" + da_id + "'");
      stmt.close();

      return query;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    return -1;
  }

  public int removeSkillByDaId(int da_id)
  {
    try
    {
      Statement stmt = conn.createStatement();
      int query = stmt.executeUpdate("DELETE FROM DAS WHERE da_id = '" + da_id + "'");
      stmt.close();

      return query;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    return -1;
  }

  /**
   *
   * @param recipe_name
   * @return
   */
  public int getRecipeId(String recipe_name)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    try
    {
      Statement stmt = conn.createStatement();
      ResultSet query = stmt.executeQuery("Select Recipe.id FROM Recipe WHERE name = '" + recipe_name + "'");
      stmt.close();

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();
      if (!query.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        return Integer.parseInt(query.getString(1));
      }

    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return -1;
  }

  /**
   *
   * @param id
   * @return
   */
  public String getRecipeNameByID(String aml_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    String result = "";
    try
    {
      Statement stmt = conn.createStatement();
      String sql = "Select Recipe.name FROM Recipe WHERE aml_id = '" + aml_id + "';";
      ResultSet query = stmt.executeQuery(sql);
      if (!query.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        result = query.getString(1);
      }
      stmt.close();
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return result;
  }

  /**
   *
   * @param deviceAdapterName
   * @return
   */
  public ArrayList<String> getRecipesIDByDAName(String deviceAdapterName)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    ArrayList<String> result = new ArrayList<>();
    try
    {
      try (Statement stmt = conn.createStatement())
      {
        String sql = "SELECT Recipe.aml_id, Recipe.sk_id, Recipe.da_id, Recipe.valid, Recipe.name, DeviceAdapter.id, DeviceAdapter.name\n"
                + "FROM Recipe, DeviceAdapter\n"
                + "WHERE Recipe.da_id = DeviceAdapter.id AND DeviceAdapter.name = '" + deviceAdapterName + "';";
        ResultSet rs = stmt.executeQuery(sql);
        if (!rs.isBeforeFirst())
        {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
          //System.out.println("No data");
        } else
        {
          while (rs.next())
          {
            result.add(rs.getString("aml_id"));
          }
        }
      }
    } catch (SQLException ex)
    {
      System.out.println("[ERROR] getRecipesByDAName " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return result;
  }

  /*
  public ArrayList<Recipe> getModulesByDAName(String deviceAdapterName)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    ArrayList<Module> result = new ArrayList<>();
    try
    {
      try (Statement stmt = conn.createStatement())
      {
        String sql = "SELECT Modules.address, Modules.da_id, DeviceAdapter.id, DeviceAdapter.name\n"
                + "FROM Recipe, DeviceAdapter\n"
                + "WHERE Modules.da_id = DeviceAdapter.id AND DeviceAdapter.name = '" + deviceAdapterName + "';";
        ResultSet rs = stmt.executeQuery(sql);
        if (!rs.isBeforeFirst())
        {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
          //System.out.println("No data");
        } else
        {
          while (rs.next())
          {
            Recipe recipe = new Recipe();
            recipe.setUniqueId(rs.getString(1));
            recipe.setName(rs.getString(5));
            recipe.setValid(trueSet.contains(rs.getString(4)));
            result.add(recipe);
          }
        }
      }
    } catch (SQLException ex)
    {
      System.out.println("[ERROR] getRecipesByDAName " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return result;
  }
   */
  public ArrayList<String> getRecipesIDbySkillReqID(String sr_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    ArrayList<String> result = new ArrayList<>();
    try
    {
      Statement stmt = conn.createStatement();
      ResultSet query = stmt.executeQuery("SELECT SR.r_id FROM SR WHERE sr_id = '" + sr_id + "'");
      if (!query.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (query.next())
        {
          result.add(query.getString(1));
        }
      }
      stmt.close();

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();

      return result;

    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return null;
  }

  /**
   *
   * @param recipe_name
   * @return
   */
  public String getRecipeMethodByName(String recipe_name)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    try
    {
      Statement stmt = conn.createStatement();
      ResultSet query = stmt.executeQuery("SELECT Recipe.endpoint FROM Recipe WHERE name = '" + recipe_name + "'");
      stmt.close();

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();
      if (!query.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        return query.getString(1);
      }
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return null;
  }

  public String getRecipeMethodByID(String recipe_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    try
    {
      //System.out.println("*DB query* trying to get method for recipe:" + recipe_id);

      ArrayList<String> result = new ArrayList<>();

      Statement stmt = conn.createStatement();
      ResultSet query = stmt.executeQuery("SELECT Recipe.method_id FROM Recipe WHERE aml_id = '" + recipe_id + "'");
      if (!query.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (query.next())
        {
          result.add(query.getString(1));
        }
      }
      stmt.close();

      //stmt.close();
      //String res= query.getString("da_id");
      if (result.size() > 0)
      {
        Long time = DBqueryTimer.getTime();
        perfMeasure.getDatabaseQueryTimers().add(time);
        DBqueryTimer.stop();

        return result.get(0);
      }

    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return null;
  }

  public String getRecipeObjectByID(String recipe_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    try
    {
      //System.out.println("*DB query* trying to get object for recipe:" + recipe_id);

      ArrayList<String> result = new ArrayList<>();

      Statement stmt = conn.createStatement();
      ResultSet query = stmt.executeQuery("SELECT Recipe.obj_id FROM Recipe WHERE aml_id = '" + recipe_id + "'");
      if (!query.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (query.next())
        {
          result.add(query.getString(1));
        }
      }
      stmt.close();

      //stmt.close();
      //String res= query.getString("da_id");
      if (result.size() > 0)
      {
        Long time = DBqueryTimer.getTime();
        perfMeasure.getDatabaseQueryTimers().add(time);
        DBqueryTimer.stop();

        return result.get(0);
      }

      //stmt.close();
      //return query.getString(1);
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return null;
  }

  public String getDA_DB_IDbyRecipeID(String recipe_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();
    try
    {
      //System.out.println("*DB query* gtting DAID from RecipeID: " + recipe_id);
      ArrayList<String> result = new ArrayList<>();

      Statement stmt = conn.createStatement();
      ResultSet query = stmt.executeQuery("SELECT Recipe.da_id FROM Recipe WHERE aml_id = '" + recipe_id + "'");
      if (!query.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (query.next())
        {
          result.add(query.getString(1));
        }
      }
      stmt.close();

      //stmt.close();
      //String res= query.getString("da_id");
      if (result.size() > 0)
      {

        Long time = DBqueryTimer.getTime();
        perfMeasure.getDatabaseQueryTimers().add(time);
        DBqueryTimer.stop();

        return result.get(0);
      }

    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return null;
  }

  public String getDA_AML_IDbyRecipeID(String recipe_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();
    try
    {
      //System.out.println("*DB query* gtting DAID from RecipeID: " + recipe_id);
      ArrayList<String> result = new ArrayList<>();

      Statement stmt = conn.createStatement();
      ResultSet query = stmt.executeQuery("SELECT DeviceAdapter.aml_id "
              + "FROM DeviceAdapter, Recipe "
              + "WHERE Recipe.aml_id = '" + recipe_id + "' AND Recipe.da_id = DeviceAdapter.id");
      if (!query.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (query.next())
        {
          result.add(query.getString(1));
        }
      }
      stmt.close();

      //stmt.close();
      //String res= query.getString("da_id");
      if (result.size() > 0)
      {

        Long time = DBqueryTimer.getTime();
        perfMeasure.getDatabaseQueryTimers().add(time);
        DBqueryTimer.stop();

        return result.get(0);
      }

    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return null;
  }

  public String getDA_DB_IDbyAML_ID(String da_aml_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();
    try
    {
      //System.out.println("*DB query* gtting DAID from RecipeID: " + recipe_id);
      ArrayList<String> result = new ArrayList<>();

      Statement stmt = conn.createStatement();
      ResultSet query = stmt.executeQuery("SELECT DeviceAdapter.id FROM DeviceAdapter WHERE DeviceAdapter.aml_id = '" + da_aml_id + "'");
      if (!query.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
      } else
      {
        while (query.next())
        {
          result.add(query.getString(1));
        }
      }
      stmt.close();

      //stmt.close();
      //String res= query.getString("da_id");
      if (result.size() > 0)
      {

        Long time = DBqueryTimer.getTime();
        perfMeasure.getDatabaseQueryTimers().add(time);
        DBqueryTimer.stop();

        return result.get(0);
      }

    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return null;
  }

  public String getSkillReqIDbyRecipeID(String recipe_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();
    String res = null;
    try
    {
      Statement stmt = conn.createStatement();
      ResultSet query = stmt.executeQuery("SELECT SR.sr_id FROM SR WHERE SR.r_id = '" + recipe_id + "'");

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();
      if (!query.isBeforeFirst())
      {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
        //System.out.println("No data");
        stmt.close();
      } else
      {
        res = query.getString(1);
        stmt.close();
      }
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    return res;
  }

  public ArrayList<String> getAvailableSkillIDList()
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    try
    {
      Statement stmt = conn.createStatement();
      ArrayList<String> myresult;
      try (ResultSet query = stmt.executeQuery("SELECT Skill.id  FROM Skill"))
      {
        myresult = new ArrayList<>();
        if (!query.isBeforeFirst())
        {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
          //System.out.println("No data");
        } else
        {
          while (query.next())
          {
            myresult.add(query.getString(1));
          }
        }
      }
      stmt.close();

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();

      return myresult;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return null;
  }

  public ArrayList<String> getDAassociatedSkillIDList()
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    try
    {
      Statement stmt = conn.createStatement();
      ArrayList<String> myresult;
      try (ResultSet query = stmt.executeQuery("SELECT DAS.sk_id  FROM DAS"))
      {
        myresult = new ArrayList<>();
        if (!query.isBeforeFirst())
        {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
          //System.out.println("No data");
        } else
        {
          while (query.next())
          {
            myresult.add(query.getString(1));
          }
        }
      }
      stmt.close();

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();

      return myresult;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return null;
  }

  public boolean registerModule(String da_name, String module_name, String aml_id, String status, String address)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    int device_id;
    device_id = getDeviceAdapterDB_ID_ByName(da_name);
    if (device_id == -1)
    {
      return false;
    }

    try
    {
      Statement stmt = conn.createStatement();
      {
        String sql = "INSERT INTO Modules"
                + "(da_id, aml_id, status, name, address)"
                + " VALUES ("
                + "'" + device_id + "','" + aml_id + "','" + status + "','" + module_name + "','" + address + "')";
        stmt.execute(sql);
        ResultSet r = stmt.getGeneratedKeys();
      }
      stmt.close();
      System.out.println("REGISTER MODULE  " + module_name + " " + status + " " + da_name);

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();

      return true;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return false;
  }

  public int removeModuleByDAId(int da_id)
  {
    try
    {
      Statement stmt = conn.createStatement();
      int query = stmt.executeUpdate("DELETE FROM Modules WHERE da_id = '" + da_id + "'");
      stmt.close();

      return query;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    return -1;
  }

  public int removeModuleByID(String module_id)
  {
    try
    {
      Statement stmt = conn.createStatement();
      int query = stmt.executeUpdate("DELETE FROM Modules WHERE aml_id = '" + module_id + "'");
      stmt.close();

      return query;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    return -1;
  }

  public boolean associateRecipeToSR(String sr_id, List<String> recipes_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    try
    {
      for (String recipe_id : recipes_id)
      {

        if (recipe_exists_in_SR(recipe_id))
        {
          continue;
        }

        Statement stmt = conn.createStatement();
        {
          String sql = "INSERT INTO SR"
                  + "(r_id, sr_id)"
                  + " VALUES ("
                  + "'" + recipe_id + "','" + sr_id + "')";
          stmt.execute(sql);
          ResultSet r = stmt.getGeneratedKeys();
        }
        stmt.close();
        System.out.println("REGISTER SR  " + recipe_id + " " + sr_id);
      }

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();

      return true;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return false;
  }

  public boolean UpdateDAamlID(String aml_id, int da_id)
  {
    try
    {
      Statement stmt = conn.createStatement();
      int query = stmt.executeUpdate("UPDATE DeviceAdapter SET aml_id = '" + aml_id + "' WHERE id = '" + da_id + "'");
      stmt.close();

      return true;

    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }
    return false;
  }

  public Boolean recipe_exists_in_SR(String recipe_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();
    Boolean result = false;
    try
    {
      Statement stmt = conn.createStatement();

      try (ResultSet query = stmt.executeQuery("SELECT SR.r_id FROM SR WHERE SR.r_id= '" + recipe_id + "'"))
      {
        if (!query.isBeforeFirst())
        {     //returns false if the cursor is not before the first record or if there are no rows in the ResultSet
          //System.out.println("No data");
        } else
        {
          result = true;
        }
      }
      stmt.close();

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();

      return result;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return result;
  }

  public Boolean remove_recipe_from_SR(String recipe_id)
  {
    StopWatch DBqueryTimer = new StopWatch();
    PerformanceMasurement perfMeasure = PerformanceMasurement.getInstance();
    DBqueryTimer.start();

    try
    {
      Statement stmt = conn.createStatement();

      int query = stmt.executeUpdate("DELETE FROM SR WHERE SR.r_id= '" + recipe_id + "'");

      stmt.close();

      Long time = DBqueryTimer.getTime();
      perfMeasure.getDatabaseQueryTimers().add(time);
      DBqueryTimer.stop();

      return query == 1;
    } catch (SQLException ex)
    {
      System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
      Logger.getLogger(DatabaseInteraction.class.getName()).log(Level.SEVERE, null, ex);
    }

    Long time = DBqueryTimer.getTime();
    perfMeasure.getDatabaseQueryTimers().add(time);
    DBqueryTimer.stop();

    return false;
  }

}
