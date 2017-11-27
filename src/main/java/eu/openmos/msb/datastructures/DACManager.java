/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.openmos.msb.datastructures;

import eu.openmos.msb.database.interaction.DatabaseInteraction;
import eu.openmos.model.Equipment;
import eu.openmos.model.Module;
import eu.openmos.model.Recipe;
import eu.openmos.model.Skill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author fabio.miranda
 *
 */
public class DACManager
{

  private final Set<String> trueSet = new HashSet<>(Arrays.asList("1", "true", "True"));
  // Singleton specific objects
  private static final Object lock = new Object();
  private static volatile DACManager instance = null;

  // [af-silva] TODO valdiate
  // para aceder as classes especificas (DeviceAdapterOPC e DeviceAdapterDDS) fazer cast
  private final Map<Integer, DeviceAdapter> deviceAdapters;

  /**
   * @brief constructor
   */
  protected DACManager()
  {
    deviceAdapters = new HashMap<>();
  }

  /**
   * @brief obtain the Device Adapter Clients Manager unique instance
   * @return
   */
  public static DACManager getInstance()
  {
    DACManager i = instance;
    if (i == null)
    {
      synchronized (lock)
      {
        // While we were waiting for the lock, another 
        i = instance; // thread may have instantiated the object.
        if (i == null)
        {
          i = new DACManager();
          instance = i;
        }
      }
    }
    return i;
  }

  /**
   * @param deviceAdapterName
   * @param device_protocol
   * @param short_info
   * @param long_info
   */
  public DeviceAdapter addDeviceAdapter(String deviceAdapterName, EProtocol device_protocol, String short_info, String long_info)
  {
    String protocol = "";
    try
    {
      DeviceAdapter client = null;
      if (null != device_protocol)
      {
        switch (device_protocol)
        {
          case DDS:
          {
            client = new DeviceAdapterDDS();
            protocol = "DDS";
            break;
          }
          case OPC:
          {
            client = new DeviceAdapterOPC();
            protocol = "OPC";
            break;
          }
          case MQTT:
            throw new UnsupportedOperationException("Not supported yet.");
          default:
            break;

        }
      }
      int id = DatabaseInteraction.getInstance().createDeviceAdapter(deviceAdapterName, protocol, short_info, long_info);
      if (id != -1 && client != null) // the last condition should never happen
      {
        client.setId(id);
        deviceAdapters.put(id, client);
        
        return client;
      }
    } catch (UnsupportedOperationException ex)
    {
      System.out.println("[ERRO] at addDeviceAdapter" + ex.getMessage());
    }
    return null;
  }

  public DeviceAdapter getDeviceAdapterbyName(String deviceAdapterName)
  {

    //System.out.println("getDeviceAdapter id from name: "+deviceAdapterName);
    int id = DatabaseInteraction.getInstance().getDeviceAdapterDB_ID_ByName(deviceAdapterName);
    if (id != -1 && deviceAdapters.containsKey(id))
    {
      return deviceAdapters.get(id);
    }
    return null;
  }

  /**
   * @brief @param deviceAdapterName
   * @return
   */
  public boolean deleteDeviceAdapter(String deviceAdapterName)
  {

    try
    {
      int id = DatabaseInteraction.getInstance().getDeviceAdapterDB_ID_ByName(deviceAdapterName);
      
      if (id != -1 && DatabaseInteraction.getInstance().removeDeviceAdapterByName(deviceAdapterName) && deviceAdapters.containsKey(id))
      {
         
        deviceAdapters.remove(id);
        return true;
      }
    } catch (Exception ex)
    {
      System.out.println("[Error] at deleteDeviceAdapter" + ex);
    }
    return false;
  }

  public boolean deleteDAStuffByName(String deviceAdapterName)
  {
    DatabaseInteraction.getInstance().removeRecipeByDaId(DatabaseInteraction.getInstance().getDeviceAdapterDB_ID_ByName(deviceAdapterName));
    DatabaseInteraction.getInstance().removeModuleByDAId(DatabaseInteraction.getInstance().getDeviceAdapterDB_ID_ByName(deviceAdapterName));

    //remove skills for a DA. from DAS table
    DatabaseInteraction.getInstance().removeSkillByDaId(DatabaseInteraction.getInstance().getDeviceAdapterDB_ID_ByName(deviceAdapterName));
    //remove skills that don't have a DA associated
    ArrayList<String> availableSkillIDList = DatabaseInteraction.getInstance().getAvailableSkillIDList();
    ArrayList<String> DAassociatedSkillIDList = DatabaseInteraction.getInstance().getDAassociatedSkillIDList();

    //check if all the available skills are associated with a da
    for (int i = 0; i < availableSkillIDList.size(); i++)
    {
      String availableSkillID=availableSkillIDList.get(i);
      boolean exists = false;
      for (int j = 0; j < DAassociatedSkillIDList.size(); j++)
      {
        if (availableSkillID == null ? DAassociatedSkillIDList.get(j) == null : availableSkillID.equals(DAassociatedSkillIDList.get(j)))
        {
          exists = true;
          System.out.println(availableSkillID+" Exists");
        }
      }
      if (!exists)
      {
        int ret = DatabaseInteraction.getInstance().removeSkillByID(availableSkillIDList.get(i));
      }
    }
    return true;
  }
  
  
  /**
   * @param deviceAdapterName
   * @brief WORKSTATIONName vs DEVICE data MAPS
   * @return
   */
  public List<Module> getModulesFromDeviceAdapter(String deviceAdapterName)
  {
    int id = DatabaseInteraction.getInstance().getDeviceAdapterDB_ID_ByName(deviceAdapterName);
    if (id != -1 && deviceAdapters.containsKey(id))
    {
      return deviceAdapters.get(id).getListOfEquipments();
    }
    return null;
  }

  /**
   *
   * @param deviceAdapterName
   * @param device
   */
  public void addEquipmentModuleToList(String deviceAdapterName, Module device)
  {
    int id = DatabaseInteraction.getInstance().getDeviceAdapterDB_ID_ByName(deviceAdapterName);
    if (id != -1 && deviceAdapters.containsKey(id))
    {
      deviceAdapters.get(id).getSubSystem().getInternalModules().add(device);
    }
  }

  /**
   *
   * @param deviceAdapterName
   * @param deviceName
   * @return
   */
  public boolean deleteEquipmentModuleFromList(String deviceAdapterName, String deviceName)
  {
    int id = DatabaseInteraction.getInstance().getDeviceAdapterDB_ID_ByName(deviceAdapterName);
    if (id != -1 && deviceAdapters.containsKey(id))
    {
      return deviceAdapters.get(id).removeEquipmentModule(deviceName);
    }
    return false;
  }

  public ArrayList<String> listDevicesByProtocol(EProtocol p)
  {
    switch (p)
    {
      case DDS:
        return DatabaseInteraction.getInstance().listDevicesByProtocol("DDS");
      case OPC:
        return DatabaseInteraction.getInstance().listDevicesByProtocol("OPC");
      default:
        return null;
    }
  }

  /**
   * @brief Wrapper
   * @param deviceAdapterName
   * @return
   */
  public ArrayList<Recipe> getRecipesFromDeviceAdapter(String deviceAdapterName)
  {
    return DatabaseInteraction.getInstance().getRecipesByDAName(deviceAdapterName);
  }

  /**
   *
   * @return
   */
  public List<String> getDeviceAdapters()
  {
    return DatabaseInteraction.getInstance().getDeviceAdapters();
  }

  /**
   *
   * @param deviceAdapterName
   * @param aml_id
   * @param skillName
   * @param recipeValid
   * @param recipeName
     * @param object_id
     * @param method_id
   * @return
   */
  public boolean registerRecipe(String deviceAdapterName, String aml_id, String skillName, String recipeValid, String recipeName, String object_id, String method_id)
  {
    DatabaseInteraction db = DatabaseInteraction.getInstance();
    int da_id = db.getDeviceAdapterDB_ID_ByName(deviceAdapterName);
    int sk_id = db.getSkillIdByName(skillName);
    boolean valid = trueSet.contains(recipeValid);
    if (da_id != -1 && sk_id != -1)
    {
      return db.registerRecipe(aml_id, da_id, sk_id, valid, recipeName, object_id, method_id);
    } else
    {
      return false;
    }
  }

  /**
   *
   * @param deviceAdapterName
   * @param aml_id
   * @param name
   * @param description
   * @return
   */
  public boolean registerSkill(String deviceAdapterName, String aml_id, String name, String description)
  {
    DatabaseInteraction db = DatabaseInteraction.getInstance();
    return db.registerSkill(deviceAdapterName, aml_id, name, description);
  }

  public boolean registerModule(String deviceAdapterName, String name, String status, String address)
  {
    DatabaseInteraction db = DatabaseInteraction.getInstance();
    return db.registerModule(deviceAdapterName, name, status, address);
  }
  
  public boolean skillExists(String aml_id)
  {
    DatabaseInteraction db = DatabaseInteraction.getInstance();
    return db.skillExists(aml_id);
  }
}
