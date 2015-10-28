/**
* Pedometer bridge with Cordova, programmed by Dario Salvi
* Based on the accelemeter plugin: https://github.com/apache/cordova-plugin-device-motion
* License: MIT
*/
package org.apache.cordova.pedometer;

import java.util.List;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Handler;
import android.os.Looper;

/**
* This class listens to the pedometer sensor
*/
public class PedoListener extends CordovaPlugin implements SensorEventListener {

  public static int STOPPED = 0;
  public static int STARTING = 1;
  public static int RUNNING = 2;
  public static int ERROR_FAILED_TO_START = 3;
  public static int NO_SENSOR_FOUND = 4;

  private float steps;                                // most recent acceleration values
  private long timestamp;                         // time of most recent value
  private int status;                                 // status of listener
  private int accuracy = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM;

  private SensorManager sensorManager;    // Sensor manager
  private Sensor mSensor;                           // Acceleration sensor returned by sensor manager

  private CallbackContext callbackContext;              // Keeps track of the JS callback context.

  private Handler mainHandler=null;
  private Runnable mainRunnable =new Runnable() {
    public void run() {
      AccelListener.this.timeout();
    }
  };

  /**
  * Constructor
  */
  public AccelListener() {
    this.steps = 0;
    this.timestamp = 0;
    this.setStatus(PedoListener.STOPPED);
  }

  /**
  * Sets the context of the Command. This can then be used to do things like
  * get file paths associated with the Activity.
  *
  * @param cordova The context of the main Activity.
  * @param webView The associated CordovaWebView.
  */
  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    this.sensorManager = (SensorManager) cordova.getActivity().getSystemService(Context.SENSOR_SERVICE);
  }

  /**
  * Executes the request.
  *
  * @param action        The action to execute.
  * @param args          The exec() arguments.
  * @param callbackId    The callback id used when calling back into JavaScript.
  * @return              Whether the action was valid.
  */
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

    if (action.equals("isStepCountingAvailable")) {
      List<Sensor> list = this.sensorManager.getSensorList(Sensor.TYPE_STEP_COUNTER);
      // If found, then register as listener
      if ((list != null) && (list.size() > 0)) {
        PluginResult result = new PluginResult(PluginResult.Status.OK, this.getStepsJSON());
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
        return true;
      } else {
        this.setStatus(PedoListener.NO_SENSOR_FOUND);
        this.fail(PedoListener.NO_SENSOR_FOUND, "No sensors found to register step counter listening to.");
        return true;
      }
    } else if (action.equals("isDistanceAvailable")) {
      //distance is never available in Android
      this.fail(PedoListener.NO_SENSOR_FOUND, "No sensors found with distance estimation.");
      return true;
    }else if (action.equals("isFloorCountingAvailable")) {
      //floor counting is never available in Android
      this.fail(PedoListener.NO_SENSOR_FOUND, "No sensors found with distance estimation.");
      return true;
    }
    else if (action.equals("startPedometerUpdates")) {
      this.callbackContext = callbackContext;
      if (this.status != PedoListener.RUNNING) {
        // If not running, then this is an async call, so don't worry about waiting
        // We drop the callback onto our stack, call start, and let start and the sensor callback fire off the callback down the road
        this.start();
      }
    }
    else if (action.equals("stopPedometerUpdates")) {
      if (this.status == PedoListener.RUNNING) {
        this.stop();
      }
    } else {
      // Unsupported action
      return false;
    }

    PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT, "");
    result.setKeepCallback(true);
    callbackContext.sendPluginResult(result);
    return true;
  }

  /**
  * Called by AccelBroker when listener is to be shut down.
  * Stop listener.
  */
  public void onDestroy() {
    this.stop();
  }

  //--------------------------------------------------------------------------
  // LOCAL METHODS
  //--------------------------------------------------------------------------
  //

  /**
  * Start listening for acceleration sensor.
  *
  * @return          status of listener
  */
  private int start() {
    // If already starting or running, then restart timeout and return
    if ((this.status == PedoListener.RUNNING) || (this.status == PedoListener.STARTING)) {
      startTimeout();
      return this.status;
    }

    this.setStatus(PedoListener.STARTING);

    // Get accelerometer from sensor manager
    List<Sensor> list = this.sensorManager.getSensorList(Sensor.TYPE_STEP_COUNTER);

    // If found, then register as listener
    if ((list != null) && (list.size() > 0)) {
      this.mSensor = list.get(0);
      if (this.sensorManager.registerListener(this, this.mSensor, SensorManager.SENSOR_DELAY_UI)) {
        this.setStatus(AccelListener.STARTING);
      } else {
        this.setStatus(AccelListener.ERROR_FAILED_TO_START);
        this.fail(AccelListener.ERROR_FAILED_TO_START, "Device sensor returned an error.");
        return this.status;
      };

    } else {
      this.setStatus(AccelListener.ERROR_FAILED_TO_START);
      this.fail(AccelListener.ERROR_FAILED_TO_START, "No sensors found to register accelerometer listening to.");
      return this.status;
    }

    startTimeout();

    return this.status;
  }
  private void startTimeout() {
    // Set a timeout callback on the main thread.
    stopTimeout();
    mainHandler = new Handler(Looper.getMainLooper());
    mainHandler.postDelayed(mainRunnable, 2000);
  }
  private void stopTimeout() {
    if(mainHandler!=null){
      mainHandler.removeCallbacks(mainRunnable);
    }
  }
  /**
  * Stop listening to acceleration sensor.
  */
  private void stop() {
    stopTimeout();
    if (this.status != AccelListener.STOPPED) {
      this.sensorManager.unregisterListener(this);
    }
    this.setStatus(AccelListener.STOPPED);
    this.accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE;
  }

  /**
  * Returns latest cached position if the sensor hasn't returned newer value.
  *
  * Called two seconds after starting the listener.
  */
  private void timeout() {
    if (this.status == AccelListener.STARTING) {
      // call win with latest cached position
      this.timestamp = System.currentTimeMillis();
      this.win();
    }
  }

  /**
  * Called when the accuracy of the sensor has changed.
  *
  * @param sensor
  * @param accuracy
  */
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    // Only look at accelerometer events
    if (sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
      return;
    }

    // If not running, then just return
    if (this.status == AccelListener.STOPPED) {
      return;
    }
    this.accuracy = accuracy;
  }

  /**
  * Sensor listener event.
  *
  * @param SensorEvent event
  */
  public void onSensorChanged(SensorEvent event) {
    // Only look at accelerometer events
    if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
      return;
    }

    // If not running, then just return
    if (this.status == AccelListener.STOPPED) {
      return;
    }
    this.setStatus(AccelListener.RUNNING);

    if (this.accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {

      // Save time that event was received
      this.timestamp = System.currentTimeMillis();
      this.x = event.values[0];
      this.y = event.values[1];
      this.z = event.values[2];

      this.win();
    }
  }

  /**
  * Called when the view navigates.
  */
  @Override
  public void onReset() {
    if (this.status == AccelListener.RUNNING) {
      this.stop();
    }
  }

  // Sends an error back to JS
  private void fail(int code, String message) {
    // Error object
    JSONObject errorObj = new JSONObject();
    try {
      errorObj.put("code", code);
      errorObj.put("message", message);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    PluginResult err = new PluginResult(PluginResult.Status.ERROR, errorObj);
    err.setKeepCallback(true);
    callbackContext.sendPluginResult(err);
  }

  private void win() {
    // Success return object
    PluginResult result = new PluginResult(PluginResult.Status.OK, this.getStepsJSON());
    result.setKeepCallback(true);
    callbackContext.sendPluginResult(result);
  }

  private void setStatus(int status) {
    this.status = status;
  }
  private JSONObject getStepsJSON() {
    JSONObject r = new JSONObject();
    try {
      r.put("x", this.x);
      r.put("y", this.y);
      r.put("z", this.z);
      r.put("timestamp", this.timestamp);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return r;
  }
}
