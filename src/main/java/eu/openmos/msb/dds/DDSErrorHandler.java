package eu.openmos.msb.dds;

/*
 * OpenSplice DDS
 *
 * This software and documentation are Copyright 2006 to 2013 PrismTech Limited and its licensees. All rights reserved.
 * See file:
 *
 * $OSPL_HOME/LICENSE
 *
 * for full copyright notice and license terms.
 *
 */
/**
 * **********************************************************************
 * LOGICAL_NAME: ErrorHandler.java FUNCTION: ErrorHandler class for the HelloWorld OpenSplice programming example.
 * MODULE: OpenSplice HelloWorld example for the java programming language. DATE September 2010.
 * **********************************************************************
 */
import DDS.*;

public class DDSErrorHandler
{

  public static final int NR_ERROR_CODES = 13;

  /*
   * Array to hold the names for all ReturnCodes.
   */
  public static String[] RetCodeName = new String[NR_ERROR_CODES];

  static
  {
    RetCodeName[0] = "DDS_RETCODE_OK";
    RetCodeName[1] = "DDS_RETCODE_ERROR";
    RetCodeName[2] = "DDS_RETCODE_UNSUPPORTED";
    RetCodeName[3] = "DDS_RETCODE_BAD_PARAMETER";
    RetCodeName[4] = "DDS_RETCODE_PRECONDITION_NOT_MET";
    RetCodeName[5] = "DDS_RETCODE_OUT_OF_RESOURCES";
    RetCodeName[6] = "DDS_RETCODE_NOT_ENABLED";
    RetCodeName[7] = "DDS_RETCODE_IMMUTABLE_POLICY";
    RetCodeName[8] = "DDS_RETCODE_INCONSISTENT_POLICY";
    RetCodeName[9] = "DDS_RETCODE_ALREADY_DELETED";
    RetCodeName[10] = "DDS_RETCODE_TIMEOUT";
    RetCodeName[11] = "DDS_RETCODE_NO_DATA";
    RetCodeName[12] = "DDS_RETCODE_ILLEGAL_OPERATION";
  }

  /**
   * Returns the name of an error code.
   *
   * @param status
   * @return the error code
   */
  public static String getErrorName(int status)
  {
    return RetCodeName[status];
  }

  /**
   * Check the return status for errors. If there is an error, then terminate.
   *
   * @param status
   * @param info
   */
  public static void checkStatus(int status, String info)
  {
    if (status != RETCODE_OK.value && status != RETCODE_NO_DATA.value)
    {
      System.out.println("Error in " + info + ": " + getErrorName(status));
      System.exit(-1);
    }
  }

  /**
   * Check whether a valid handle has been returned. If not, then terminate.
   *
   * @param handle
   * @param info
   */
  public static void checkHandle(Object handle, String info)
  {
    if (handle == null)
    {
      System.out.println("Error in " + info + ": Creation failed: invalid handle");
      System.exit(-1);
    }
  }

}
