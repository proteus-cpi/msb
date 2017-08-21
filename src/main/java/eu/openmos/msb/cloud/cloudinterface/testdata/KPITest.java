/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.openmos.msb.cloud.cloudinterface.testdata;

import eu.openmos.model.KPI;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author valerio.gentile
 */
public class KPITest
{

  public static KPI getTestObject()
  {
    /*
     * @param description - KPI's Description.
     * @param uniqueId - KPI's ID.
     * @param name - KPI's name.
     * @param defaultUpperBound - KPI's upper bound.
     * @param defaultLowerBound - KPI's lower bound.
     * @param currentValue - KPI's current value.
     * @param unit - KPI's unit.        
     */

    KPI kpi = new KPI(
            "KPIDescription",
            "KPIUniqueId",
            "KPIName",
            "KPIDefaultUpperBound",
            "KPIDefaultLowerBound",
            "KPICurrentValue",
            "KPIUnit",
            "KPIType");

    return kpi;
  }

  public static List<KPI> getTestList()
  {
    return new LinkedList<>(Arrays.asList(getTestObject()));
  }
}