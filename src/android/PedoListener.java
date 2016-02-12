/**
 * Pedometer bridge with Cordova, programmed by Dario Salvi </dariosalvi78@gmail.com>
 * Based on the accelerometer plugin: https://github.com/apache/cordova-plugin-device-motion
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

/**
 * This class listens to the pedometer sensor,
 * if no pedometer is installed, a fallback implementation is used.
 */
public class PedoListener extends CordovaPlugin implements SensorEventListener {

    //first value, to be substracted
    private float startsteps;

    //time stamp of when the measurement starts
    private long starttimestamp;

    // Sensor manager
    private SensorManager sensorManager;

    // Pedometer or Accelerometer sensor returned by sensor manager
    private Sensor mSensor;

    //tells if the sensor is running or not
    private boolean running = false;

    //used as a fallback when the hardware step counter is not available
    private AccelerometerPedometer fallback = new AccelerometerPedometer();

    // Keeps track of the JS callback context.
    private CallbackContext callbackContext;

    private Handler mainHandler=null;


    public PedoListener() {
        this.starttimestamp = 0;
        this.startsteps = 0;
    }


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        //starts the sensor service
        this.sensorManager = (SensorManager) cordova.getActivity().getSystemService(Context.SENSOR_SERVICE);
    }

    /**
     * Executes the request.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;

        if (action.equals("isStepCountingAvailable")) {
            List<Sensor> list = this.sensorManager.getSensorList(Sensor.TYPE_STEP_COUNTER);
            if ((list != null) && (list.size() > 0)) {
                PluginResult result;
                result = new PluginResult(PluginResult.Status.OK, true);
                callbackContext.sendPluginResult(result);
                return true;
            } else {
                //check the fallback
                list = this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
                if ((list != null) && (list.size() > 0)) {
                    PluginResult result;
                    result = new PluginResult(PluginResult.Status.OK, true);
                    callbackContext.sendPluginResult(result);
                    return true;
                } else{
                    PluginResult result;
                    result = new PluginResult(PluginResult.Status.OK, false);
                    callbackContext.sendPluginResult(result);
                    return true;
                }
            }
        } else if (action.equals("isDistanceAvailable")) {
            //distance is never available in Android
            PluginResult result;
            result = new PluginResult(PluginResult.Status.OK, false);
            callbackContext.sendPluginResult(result);
            return true;
        } else if (action.equals("isFloorCountingAvailable")) {
            //floor counting is never available in Android
            PluginResult result;
            result = new PluginResult(PluginResult.Status.OK, false);
            callbackContext.sendPluginResult(result);
            return true;
        } else if (action.equals("startPedometerUpdates")) {
            if (!running) {
                start();
            }
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT, "");
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        } else if (action.equals("stopPedometerUpdates")) {
            if (running) {
                stop();
            }
            callbackContext.success();
            return true;
        } else {
            // Unsupported action
            return false;
        }
    }


    public void onDestroy() {
        stop();
    }


    /**
     * Start listening for sensor.
     */
    private void start() {
        // If already starting or running, then return
        if (running) {
            return;
        }

        starttimestamp = System.currentTimeMillis();
        startsteps = 0;

        // Get pedometer from sensor manager
        List<Sensor> list = sensorManager.getSensorList(Sensor.TYPE_STEP_COUNTER);

        // If found, then register as listener
        if ((list != null) && (list.size() > 0)) {
            mSensor = list.get(0);//get the first one
            if (sensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL)) {
                running = true;
            } else {
                callbackContext.error("Pedometer sensor returned an error");
                return;
            }
        } else {
            //try with accelerometer
            list = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

            if ((list != null) && (list.size() > 0)) {
                mSensor = list.get(0);//get the first one
                if (sensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL)) {
                    running = true;
                } else {
                    callbackContext.error("Accelerometer sensor returned an error");
                    return;
                }
            } else {
                running = false;
                callbackContext.error("No suitable sensors found");
                return;
            }
        }
    }

    /**
     * Stop listening to sensor.
     */
    private void stop() {
        sensorManager.unregisterListener(this);
        fallback.reset();
        running = false;
    }

    /**
     * Called when the accuracy of the sensor has changed.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
      //nothing to do here
    }

    /**
     * Sensor listener event.
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // If not running, then just return
        if (!running) return;

        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float steps = event.values[0];

            if(startsteps == 0)
                startsteps = steps;

            steps = steps - startsteps;

            PluginResult result;
            result = new PluginResult(PluginResult.Status.OK, getStepsJSON(steps));
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
        } else if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //fallback scenario
            int steps = fallback.getSteps(event.timestamp, event.values[0], event.values[1], event.values[2]);

            if(startsteps != steps){
                PluginResult result;
                result = new PluginResult(PluginResult.Status.OK, getStepsJSON(steps));
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);
                startsteps = steps;
            }
        } else {
            //ignore it
        }
    }

    /**
     * Called when the view navigates.
     */
    @Override
    public void onReset() {
        stop();
        fallback.reset();
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


    private JSONObject getStepsJSON(float steps) {
        JSONObject r = new JSONObject();
        // pedometerData.startDate; -> ms since 1970
        // pedometerData.endDate; -> ms since 1970
        // pedometerData.numberOfSteps;
        // pedometerData.distance;
        // pedometerData.floorsAscended;
        // pedometerData.floorsDescended;
        try {
            r.put("startDate", starttimestamp);
            r.put("endDate", System.currentTimeMillis());
            r.put("numberOfSteps", steps);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return r;
    }
}
